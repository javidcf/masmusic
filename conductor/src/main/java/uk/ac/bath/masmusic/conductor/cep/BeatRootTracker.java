package uk.ac.bath.masmusic.conductor.cep;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.conductor.Conductor;
import uk.ac.bath.masmusic.conductor.analysis.BeatRoot;
import uk.ac.bath.masmusic.protobuf.Pitch;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Beat detector for Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
//@Component
public class BeatRootTracker extends EsperStatementSubscriber {

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 40;  // TODO Use this?

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 5000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 1000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(BeatRootTracker.class);

    @Autowired
    private Conductor conductor;

    /** BeatRoot beat tracker. */
    @Autowired
    private BeatRoot beatRoot;

    /** Events in the last analyzed window (sorted by time) */
    private final ArrayList<Onset> onsets;

    /** Currently tracked beat. */
    private final AtomicReference<Beat> beat;

    /**
     * Constructor.
     *
     * Registers the function {@code noteImportance} in the Esper configuration.
     *
     * @param config
     *            Esper configuration
     */
    @Autowired
    public BeatRootTracker(com.espertech.esper.client.Configuration config) {
        config.addPlugInSingleRowFunction("noteSalience",
                "uk.ac.bath.masmusic.conductor.cep.BeatRootTracker",
                "noteSalience");
        onsets = new ArrayList<>();
        beat = new AtomicReference<>(null);
    }

    /**
     * @return The current beat
     */
    public Beat getCurrentBeat() {
        return beat.get();
    }

    /**
     * Computes the importance of a {@link TimeSpanNote}.
     *
     * The formula used here is an heuristic for the perceived sound importance.
     *
     * @see <a href="http://users.auth.gr/emilios/papers/aaai2000.pdf">From MIDI
     *      to traditional musical notation (Cambouropoulos, 2000)</a>
     *
     * @param note
     *            The note considered
     * @return The importance of the note
     */
    public static double noteSalience(TimeSpanNote note) {
        Pitch pitch = note.getPitch();
        int semitone = pitch.getNote().getNumber();
        int absolutePitch = Math
                .min(Math.max(semitone + 12 * (pitch.getOctave() + 1), 0), 128);
        double pitchFactor = Math.min(Math.max(absolutePitch, 30), 60);
        double velocityFactor = Math.min(Math.max(note.getVelocity(), 30), 90);
        double duration = note.getDuration();
        return duration * (velocityFactor / pitchFactor);
        // return duration * (velocityFactor / (pitchFactor * pitchFactor));
        // return duration * (velocityFactor / Math.pow(pitchFactor, 3));
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
                + ", noteSalience(*) as salience"
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
    public void update(Map<String, Object> eventMap) {
        long timestamp = (Long) eventMap.get("timestamp");
        double salience = (Double) eventMap.get("salience");
        onsets.add(new Onset(timestamp, salience));
    }

    /**
     * Finish event delivery.
     */
    public void updateEnd() {
        Beat newBeat = beatRoot.estimateBeat(onsets);
        onsets.clear();
        if (newBeat != null) {
            LOG.debug("New beat: {}", newBeat);
            beat.set(newBeat);
            conductor.conduct();
        }
    }

}
