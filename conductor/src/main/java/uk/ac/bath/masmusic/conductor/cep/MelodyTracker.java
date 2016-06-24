package uk.ac.bath.masmusic.conductor.cep;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.conductor.Conductor;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Melody analyzer for Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
//@Component
public class MelodyTracker extends EsperStatementSubscriber {

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 40;  // TODO Use this?

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 5000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 1000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MelodyTracker.class);

    @Autowired
    private RhythmDetector rhythmDetector;

    @Autowired
    private Conductor conductor;

    /** Events in the last analyzed window (sorted by time) */
    private final ArrayList<Onset> onsets;

    /**
     * Constructor.
     */
    public MelodyTracker() {
        onsets = new ArrayList<>();
    }

    /*** Esper ***/

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatementQuery() {
        return "select"
                // + " Math.round(avg(timestamp)) as timestamp"
                + "noteOnset(*) as onset"
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
        onsets.clear();
        onsets.ensureCapacity(countNew);
    }

    /**
     * Receive query event.
     *
     * @param eventMap
     *            Query event data
     */
    public void update(Map<String, Onset> eventMap) {
        onsets.add(eventMap.get("onset"));
    }

    /**
     * Finish event delivery.
     */
    public void updateEnd() {
        if (onsets.isEmpty()) {
            return;
        }
        // Snap onsets to the detected rhythm
        Rhythm rhythm = rhythmDetector.getDetectedRhtyhm();
        ListIterator<Onset> it = onsets.listIterator();
        while (it.hasNext()) {
            it.set(snapOnsetToBeat(it.next(), rhythm.getBeat(), 2));
        }

        onsets.clear();
    }

    /**
     * Create a new onset resulting of snapping the given onset to the closest
     * beat subdivision.
     *
     * The resulting onset has a timestamp and duration that matches exactly
     * some beat subdivisions.
     *
     * @param onset
     *            Onset to snap
     * @param beat
     *            Beat to which the onset is snapped
     * @param subdivision
     *            Number of beat subdivisons allowed (0 = full beat, 1 = half
     *            beat, 2 = quarter beat, etc.)
     * @return
     */
    public static Onset snapOnsetToBeat(Onset onset, Beat beat,
            int subdivision) {
        if (subdivision < 0) {
            throw new IllegalArgumentException(
                    "The allowed subdivision level cannot be negative");
        }
        long begin = beat.closestSubbeat(onset.getTimestamp(), subdivision);
        long end = beat.closestSubbeat(
                onset.getTimestamp() + onset.getDuration(), subdivision);
        return new Onset(begin, (int) (end - begin), onset.getPitch(),
                onset.getVelocity());
    }

}
