package uk.ac.bath.masmusic.conductor.cep;

import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.beat.Beat;
import uk.ac.bath.masmusic.beat.BeatRoot;
import uk.ac.bath.masmusic.beat.Onset;
import uk.ac.bath.masmusic.protobuf.Pitch;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Beat detector for Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
@Component
public class BeatRootTracker implements EsperStatementSubscriber {

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 40;  // TODO Use this?

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 10000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 1000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(BeatRootTracker.class);

    /** BeatRoot beat tracker. */
    @Autowired
    BeatRoot beatRoot;

    /** Events in the last analyzed window (sorted by time) */
    private ArrayList<Onset> onsets = new ArrayList<>();

    /** Currently tracked beat duration. */
    private int beatDuration;

    /** Currently tracked phase. */
    private int beatPhase;

    /** Currently tracked tempo. */
    private int tempo;

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
    }

    /**
     * @return The current tempo in beats per minute
     */
    public synchronized int getCurrentTempo() {
        return tempo;
    }

    /**
     * @return The current beat duration in milliseconds
     */
    public synchronized int getCurrentBeatDuration() {
        return beatDuration;
    }

    /**
     * @return The current beat phase in milliseconds
     */
    public synchronized int getCurrentBeatPhase() {
        return beatPhase;
    }

    /**
     * Computes the importance of a {@link TimeSpanNote}.
     *
     * The formula used here is an heuristic for the perceived sound importance.
     *
     * @see <a href="http://users.auth.gr/emilios/papers/aaai2000.pdf">From
     *      MIDI to traditional musical notation (Cambouropoulos, 2000)</a>
     *
     * @param note
     *            The note considered
     * @return The importance of the note
     */
    public static double noteSalience(TimeSpanNote note) {
        Pitch pitch = note.getPitch();
        int semitone = pitch.getNote().getNumber();
        int absolutePitch = Math.min(Math.max(semitone + 12 * (pitch.getOctave() + 1), 0), 128);
        double pitchFactor = Math.min(Math.max(absolutePitch, 30), 60);
        double velocityFactor = Math.min(Math.max(note.getVelocity(), 30), 90);
        double duration = note.getDuration();
        return duration * (velocityFactor / pitchFactor);
    }

    /*** Esper ***/

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatement() {
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
     *            Number of elements in the new delivery.
     * @param countOld
     *            Number of elements in the previous delivery.
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
        Beat beat = beatRoot.estimateBeat(onsets);
        onsets.clear();
        if (beat != null) {
            LOG.debug("New beat: {}", beat);
            updateBeat(beat);
        }
    }

    private synchronized void updateBeat(Beat beat) {
        this.beatDuration = beat.getDuration();
        this.beatPhase = beat.getPhase();
        this.tempo = beat.getTempo();
    }

}
