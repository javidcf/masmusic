package uk.ac.bath.masmusic.cep;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.TimeSignature;
import uk.ac.bath.masmusic.events.RhythmUpdatedEvent;
import uk.ac.bath.masmusic.mas.MasMusic;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Beat detector for Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
@Component
public class RhythmDetector extends EsperStatementSubscriber {

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 40; // TODO Use this?

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 5000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 100;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(RhythmDetector.class);

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private MasMusic masMusic;

    /** Events in the last analysed window (sorted by time) */
    private final ArrayList<Long> onsetTimes;

    /** Detected rhythm. */
    private final AtomicReference<Rhythm> rhythm;

    /**
     * Constructor.
     *
     * Registers the function {@code noteImportance} in the Esper configuration.
     *
     * @param config
     *            Esper configuration
     */
    public RhythmDetector() {
        onsetTimes = new ArrayList<>();
        rhythm = new AtomicReference<>(null);
    }

    /**
     * @return The detected rhythm, or null if no rhythm has been detected
     */
    public Rhythm getDetectedRhtyhm() {
        return rhythm.get();
    }

    /*** Esper ***/

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatementQuery() {
        return "select"
                // + " Math.round(avg(timestamp)) as timestamp"
                + " timestamp"
                + " from TimeSpanNote.win:time(" + ANALYSIS_WINDOW + " msec) "
                // + " group by Math.round(timestamp / " + QUANTIZATION + ")"
                + " output snapshot every " + ANALYSIS_FREQUENCY + " msec"
                + " order by timestamp asc";
    }

    /**
     * Start new event delivery.
     *
     * @param countNew
     *            Number of elements in the new delivery
     * @param countOld
     *            Number of elements in the previous delivery
     */
    public void updateStart(int countNew, int countOld) {
        onsetTimes.clear();
        if (rhythm.get() != null) {
            return;
        }
        onsetTimes.ensureCapacity(countNew);
    }

    /**
     * Receive query event.
     *
     * @param eventMap
     *            Query event data
     */
    public void update(Map<String, Long> eventMap) {
        if (rhythm.get() != null) {
            return;
        }
        long timestamp = eventMap.get("timestamp");
        onsetTimes.add(timestamp);
    }

    /**
     * Finish event delivery.
     */
    public void updateEnd() {
        if (rhythm.get() != null) {
            return;
        }
        if (onsetTimes.size() < 2) {
            return;
        }
        long diffs = 0;
        for (int i = 1; i < onsetTimes.size(); i++) {
            diffs += onsetTimes.get(i) - onsetTimes.get(i - 1);
        }
        int beatDuration = Math.round(diffs / (onsetTimes.size() - 1.f));
        // Check if rhythm input is finished
        long now = System.currentTimeMillis();
        long lastOnsetTime = onsetTimes.get(onsetTimes.size() - 1);
        if (now - lastOnsetTime < 4 * beatDuration) {
            // May not have finished yet
            return;
        }
        // Create time signature
        int numBeats = onsetTimes.size();
        int beatUnit = 4;
        // Heuristic: use quavers for six beats or more
        if (numBeats >= 6) {
            beatUnit = 8;
        }
        TimeSignature timeSignature = new TimeSignature(numBeats, beatUnit);
        // Create beat and compute beat offset
        Beat beat = new Beat(beatDuration, onsetTimes.get(0));
        long beatNumber = beat.beatNumber(beat.closestBeat(onsetTimes.get(0)));
        int beatOffset = (int) (beatNumber % numBeats);
        // Create rhythm
        Rhythm newRhythm = new Rhythm(beat, timeSignature, beatOffset);
        LOG.debug("New rhythm: {}", newRhythm);
        rhythm.set(newRhythm);
        // Update rhythm
        publisher.publishEvent(new RhythmUpdatedEvent(this, newRhythm));
        // Update rhythm in MAS
        masMusic.setRhythm(newRhythm);
        // Stop listening statement
        getStatement().stop();
    }

}
