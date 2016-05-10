package uk.ac.bath.masmusic.conductor.cep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

    /** Minimum beat value */
    private static final int MIN_BEAT = 40;

    /** Maximum beat value */
    private static final int MAX_BEAT = 240;

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 50;

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 5000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 1000;

    /** Minimum number of events required to perform a beat analysis */
    private static final int MINIMUM_NUM_EVENTS = 4;

    /** Number of hypothesis to consider for beat analysis */
    private static final int NUM_HYPOTHESES = 10;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(BeatTracker.class);

    /** Events in the last analyzed window (not necessarily sorted by time) */
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

    private void estimateBeat() {
        if (readings.size() < MINIMUM_NUM_EVENTS) {
            return;
        }
        int estimatedBeat = beat.get();
        long estimatedReference = reference.get();
        double estimationScore = 0;

        // Reorder by importance
        Collections.sort(readings, Collections.reverseOrder());
        // Consider at least NUM_HYPOTHESES, but more if there is a tie
        int numHypo = NUM_HYPOTHESES;
        if (readings.size() > numHypo) {
            double lastImportance = readings.get(numHypo - 1).importance;
            while ((readings.size() > numHypo)
                    && (readings.get(numHypo).importance >= lastImportance)) {
                numHypo++;
            }
        }
        // Consider the most salient readings
        Iterator<NoteReading> it = readings.iterator();
        for (int iHypo = 0; (iHypo < NUM_HYPOTHESES) && it.hasNext(); iHypo++) {
            // For each important reading, consider every possible beat
            long referenceTimestamp = it.next().timestamp;
            for (int beatHypo = MIN_BEAT; beatHypo <= MAX_BEAT; beatHypo++) {
                // Check score and save it if better
                double score = beatHypothesisScore(beatHypo,
                        referenceTimestamp);
                if (score > estimationScore) {
                    estimatedBeat = beatHypo;
                    estimatedReference = referenceTimestamp;
                    estimationScore = score;
                }
            }
        }

        LOG.debug("New beat: {} (score: {})", estimatedBeat, estimationScore);
        beat.set(estimatedBeat);
        reference.set(estimatedReference % (MINUTE / estimatedBeat));
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
    private double beatHypothesisScore(int beat, long referenceTimestamp) {
        int beatDuration = MINUTE / beat;
        // Threshold to consider an event is on beat - a demisemiquaver
        int beatThreshold = beatDuration / 8;
        // Estimate score
        double score = .0;
        long timeStart = Long.MAX_VALUE;
        long timeEnd = Long.MIN_VALUE;
        int numHits = 0;
        for (NoteReading reading : readings) {
            timeStart = Math.min(reading.timestamp, timeStart);
            timeEnd = Math.max(reading.timestamp, timeEnd);
            // Check if reading is on beat according to the hypothesis
            long timeDiff = Math.abs(reading.timestamp - referenceTimestamp);
            long deviation = (timeDiff + beatThreshold / 2) % beatDuration;
            if (deviation < beatThreshold) {
                // Increase score
                score += (reading.importance);
                numHits++;
            }
        }

        // Penalize higher tempos considering hit rate
        int maxBeats = ANALYSIS_WINDOW / beatDuration;
        double hitRate = ((double) numHits) / maxBeats;
        return score * Math.exp(hitRate);
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
        double importance = (velocity / 128.0) / (absolutePitch / 128.0);
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
