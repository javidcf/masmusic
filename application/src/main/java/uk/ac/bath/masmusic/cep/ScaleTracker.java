package uk.ac.bath.masmusic.cep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.analysis.scale.ScaleInducer;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.mas.MasMusic;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Scale detector for Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
@Component
public class ScaleTracker extends EsperStatementSubscriber {

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 40; // TODO Use this?

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 30000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 5000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(ScaleTracker.class);

    @Autowired
    private MasMusic masMusic;

    /** Scale inducer. */
    private final ScaleInducer scaleInducer;

    /** Notes list. */
    private final List<Note> notes;

    /** Current scale. */
    private final AtomicReference<Scale> scale;

    /**
     * Constructor.
     */
    public ScaleTracker() {
        scaleInducer = new ScaleInducer();
        notes = new ArrayList<>();
        scale = new AtomicReference<>(null);
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
                + ", pitch.note.getNumber() as noteNumber"
                + ", pitch.octave as octave"
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
        notes.clear();
    }

    /**
     * Receive query event.
     *
     * @param eventMap
     *            Query event data
     */
    public void update(Map<String, Object> eventMap) {
        notes.add(Note.fromValue((Integer) eventMap.get("noteNumber")));
    }

    /**
     * Finish event delivery.
     */
    public void updateEnd() {
        Scale newScale = scaleInducer.induceScale(notes);
        if (newScale != null) {
            LOG.debug("New scale: {}", newScale);
            scale.set(newScale);
            masMusic.setScale(newScale);
        }
    }

    /**
     * @return The induced scale, or null of no scale has been induced
     */
    public synchronized Scale getCurrentScale() {
        return scale.get();
    }

}
