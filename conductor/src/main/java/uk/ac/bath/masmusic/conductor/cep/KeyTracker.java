package uk.ac.bath.masmusic.conductor.cep;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Beat detector for Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
@Component
public class KeyTracker implements EsperStatementSubscriber {

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 40;  // TODO Use this?

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 5000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 1000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(KeyTracker.class);

    /**
     * Constructor.
     *
     * Registers the function {@code noteImportance} in the Esper configuration.
     *
     * @param config
     *            Esper configuration
     */
    @Autowired
    public KeyTracker(com.espertech.esper.client.Configuration config) {
        //        config.addPlugInSingleRowFunction("noteSalience",
        //                "uk.ac.bath.masmusic.conductor.cep.KeyTracker",
        //                "noteSalience");
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
                + ", pitch.note"
                + ", pitch.octave"
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
    }

    /**
     * Receive query event.
     *
     * @param eventMap
     *            Query event data
     */
    public void update(Map<String, Object> eventMap) {
        LOG.debug("eventMap: {}", eventMap);
    }

    /**
     * Finish event delivery.
     */
    public void updateEnd() {
    }

}
