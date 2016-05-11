package uk.ac.bath.masmusic.conductor.cep;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.protobuf.Pitch;
import uk.ac.bath.masmusic.protobuf.TimePointNote;

/**
 * Beat detector for Esper {@link TimePointNote} events.
 *
 * @author Javier Dehesa
 */
@Component
public class BeatTracker implements EsperStatementSubscriber {

    /** Default initial beat value */
    private static final int DEFAULT_BEAT = 60;

    /** A minute (ms) */
    int MINUTE = 60000;

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 50;

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 6000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 1000;

    /** Minimum number of events required to perform a beat analysis */
    private static final int MINIMUM_NUM_EVENTS = 4;

    /** Minimum beat duration */
    private static final int MIN_BEAT = 40;

    /** Maximum beat duration */
    private static final int MAX_BEAT = 240;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(BeatTracker.class);

    /** Events in the last analyzed window (sorted by time) */
    private ArrayList<NoteReading> readings = new ArrayList<>();

    /** Current beat value */
    private AtomicInteger beat = new AtomicInteger(DEFAULT_BEAT);

    /** Current beat reference */
    private AtomicLong reference = new AtomicLong();

    /**
     * Constructor.
     *
     * Registers the function {@code noteImportance} in the Esper configuration.
     *
     * @param config
     *            Esper configuration
     */
    @Autowired
    public BeatTracker(com.espertech.esper.client.Configuration config) {
        config.addPlugInSingleRowFunction("noteImportance",
                "uk.ac.bath.masmusic.conductor.cep.BeatTracker",
                "noteImportance");
    }

    /**
     * @return The current beat value
     */
    public int getCurrentBeat() {
        return beat.get();
    }

    /**
     * @return The current beat reference time
     */
    public long getCurrentReference() {
        return reference.get();
    }

    private void estimateBeat() {
        if (readings.size() < MINIMUM_NUM_EVENTS) {
            return;
        }
        int estimatedBeat = Math.round(((float) MINUTE) / beat.get());
        long estimatedReference = reference.get();
        double estimationScore = Double.NEGATIVE_INFINITY;

        // Test distance between every pair of events
        for (int iRef = 0; iRef < readings.size(); iRef++) {
            long refTimestamp = readings.get(iRef).timestamp;
            for (int iComp = iRef + 1; iComp < readings.size(); iComp++) {
                long compTimestamp = readings.get(iComp).timestamp;
                int beatDuration = (int) Math.abs(compTimestamp - refTimestamp);
                int beat = Math.round(((float) MINUTE) / beatDuration);
                if (beat >= MIN_BEAT && beat <= MAX_BEAT) {
                    double score = beatHypothesisScore(beat, refTimestamp);
                    if (score > estimationScore) {
                        estimatedBeat = beat;
                        estimatedReference = refTimestamp;
                        estimationScore = score;
                    }
                }
            }
        }

        LOG.debug("New beat: {} (score: {})", estimatedBeat, estimationScore);
        beat.set(estimatedBeat);
        reference.set(estimatedReference
                % (Math.round(((float) MINUTE) / estimatedBeat)));
    }

    /**
     * Get a score to a beat hypothesis.
     *
     * @param beat
     *            Beat value hypothesis
     * @param referenceTimestamp
     *            A reference timestamp for the beat
     * @return The hypothesis score
     */
    private double beatHypothesisScore(int beat,
            long referenceTimestamp) {
        int beatDuration = MINUTE / beat;
        // Threshold to consider an event could be a beat
        int beatThreshold = beatDuration / 8;

        // Find the beat timestamp before the first timestamp
        long referenceOffset = referenceTimestamp % beatDuration;
        long iBeat = (readings.get(0).timestamp +
                beatThreshold - referenceOffset)
                / beatDuration;
        long beatTimestamp = iBeat * beatDuration + referenceOffset;

        // Check the hits
        double score = .0;
        for (NoteReading reading : readings) {
            // Timestamp of the beginning of the hit area
            long readingHitStart = reading.timestamp - beatThreshold;
            // Move to the beat at the reading (or the next one)
            while (beatTimestamp < readingHitStart) {
                beatTimestamp += beatDuration;
            }
            // Check if there is a hit
            long deviation = Math.abs(beatTimestamp - reading.timestamp);
            if (deviation < beatThreshold) {
                // Increase score
                score += (reading.importance * deviation) / beatThreshold;
                // Correct beat timestamp
                beatTimestamp = reading.timestamp;
            }
        }

        long windowSize = ANALYSIS_WINDOW;
        int numBeats = (int) (windowSize / beatDuration);
        return score / Math.log(numBeats);
    }

    /**
     * Computes the importance of a {@link TimePointNote}.
     *
     * The formula used here is an heuristic for the perceived sound importance.
     *
     * @param note
     *            The note considered
     * @return The importance of the note
     */
    public static double noteImportance(TimePointNote note) {
        Pitch pitch = note.getPitch();
        int semitone = pitch.getNote().getNumber();
        int absolutePitch = Math
                .min(Math.max(semitone + 12 * (pitch.getOctave() + 1), 0), 128);
        int velocity = Math.min(Math.max(note.getVelocity(), 0), 128);
        double importance = velocity / (absolutePitch + 1.0);
        return importance;
    }

    private static class NoteReading implements Comparable<NoteReading> {
        long timestamp;
        double importance;

        NoteReading(long timestamp, double importance) {
            this.timestamp = timestamp;
            this.importance = importance;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(importance);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NoteReading other = (NoteReading) obj;
            if (Double.doubleToLongBits(importance) != Double
                    .doubleToLongBits(other.importance)) {
                return false;
            }
            if (timestamp != other.timestamp) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(NoteReading o) {
            return this.importance < o.importance ? -1
                    : this.importance > o.importance ? 1 : 0;
        }

        @Override
        public String toString() {
            return "NoteReading [timestamp=" + timestamp + ", importance="
                    + importance + "]";
        }
    }

    /*** Esper ***/

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatement() {
        return "select"
                + " Math.round(avg(timestamp)) as timestamp"
                + ", noteImportance(*) as importance"
                + " from TimePointNote.win:time(" + ANALYSIS_WINDOW + " msec) "
                + " group by Math.round(timestamp / " + QUANTIZATION + ")"
                + " output snapshot every " + ANALYSIS_FREQUENCY + " msec";
    }

    /**
     * Start new event delivery.
     *
     * @param countNew
     *            Number of elements in the new delivery.
     * @param countOld
     *            Number of elements in the previous delivery.
     */
    public void updateStart(int countNew, int countOld) {
        readings.clear();
        readings.ensureCapacity(countNew);
    }

    /**
     * Receive query event.
     *
     * @param eventMap
     *            Query event data
     */
    public void update(Map<String, Object> eventMap) {
        long timestamp = (Long) eventMap.get("timestamp");
        double importance = (Double) eventMap.get("importance");
        readings.add(new NoteReading(timestamp, importance));
    }

    /**
     * Finish event delivery.
     */
    public void updateEnd() {
        estimateBeat();
        readings.clear();
    }

}
