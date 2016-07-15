package uk.ac.bath.masmusic.cep;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.bath.masmusic.analysis.beatroot.BeatRoot;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.mas.MasMusic;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Beat detector for Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
//@Component  // This class is not currently used
public class BeatRootTracker extends EsperStatementSubscriber {

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 40; // TODO Use this?

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 5000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 1000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(BeatRootTracker.class);

    @Autowired
    private MasMusic masMusic;

    /** BeatRoot beat tracker. */
    @Autowired
    private BeatRoot beatRoot;

    /** Events in the last analyzed window (sorted by time) */
    private final ArrayList<Onset> onsets;

    /** Currently tracked beat. */
    private final AtomicReference<Beat> beat;

    /**
     * Constructor.
     */
    public BeatRootTracker() {
        onsets = new ArrayList<>();
        beat = new AtomicReference<>(null);
    }

    /**
     * @return The current beat
     */
    public Beat getCurrentBeat() {
        return beat.get();
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
        Beat newBeat = beatRoot.estimateBeat(onsets);
        onsets.clear();
        if (newBeat != null) {
            LOG.debug("New beat: {}", newBeat);
            beat.set(newBeat);
            // masMusic.setBeat(newBeat);
        }
    }

}
