package uk.ac.bath.masmusic.cep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.events.MusicInputBufferUpdatedEvent;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Input tracker for Esper {@link TimeSpanNote} events.
 *
 * This tracker publishes {@link MusicInputBufferUpdatedEvent} events
 * periodically.
 *
 * @author Javier Dehesa
 */
@Component
public class MusicInputTracker extends EsperStatementSubscriber {

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 30000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 5000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MusicInputTracker.class);

    @Autowired
    private ApplicationEventPublisher publisher;

    /** Onsets list. */
    private final List<Onset> onsets;

    /**
     * Constructor.
     */
    public MusicInputTracker() {
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
                + " noteOnset(*) as onset"
                + " from TimeSpanNote.win:time(" + ANALYSIS_WINDOW + " msec) "
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
        if (!onsets.isEmpty()) {
            publisher.publishEvent(new MusicInputBufferUpdatedEvent(this, onsets));
        }
    }
}
