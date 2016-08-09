package uk.ac.bath.masmusic.cep;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.analysis.beatroot.BeatRoot;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.events.RhythmUpdatedEvent;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Beat detector for Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
@Component
public class BeatRootTracker extends EsperStatementSubscriber {

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 5000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 1000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(BeatRootTracker.class);

    @Autowired
    private ApplicationEventPublisher publisher;

    /** BeatRoot beat tracker. */
    private final BeatRoot beatRoot;

    /** Events in the last analyzed window (sorted by time) */
    private final ArrayList<Onset> onsets;

    /** Last known rhythm. */
    private final AtomicReference<Rhythm> rhythm;

    /**
     * Constructor.
     */
    public BeatRootTracker() {
        beatRoot = new BeatRoot();
        onsets = new ArrayList<>();
        rhythm = new AtomicReference<>(null);
    }

    /**
     * Handle a rhythm update event.
     *
     * @param event
     *            The rhythm update event
     */
    @EventListener
    public void onRhythmUpdated(RhythmUpdatedEvent event) {
        rhythm.set(event.getRhythm());
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
        Rhythm currentRhythm = rhythm.get();
        if (currentRhythm != null) { // Wait until some rhythm has been detected
            int currentTempo = currentRhythm.getBeat().getTempo();
            int minTempo = Math.round(0.9f * currentTempo);
            int maxTempo = Math.round(1.1f * currentTempo);
            Beat newBeat = beatRoot.estimateBeat(onsets, minTempo, maxTempo);
            if (newBeat != null) {
                Rhythm newRhythm = correctRhythm(currentRhythm, newBeat);
                LOG.debug("New rhythm: {}", newRhythm);
                rhythm.set(newRhythm);
                // Update rhythm
                publisher.publishEvent(new RhythmUpdatedEvent(this, newRhythm));
            }
        }
        onsets.clear();
    }

    /**
     * @param currentRhythm
     *            The current rhythm
     * @param newBeat
     *            The new estimated beat
     * @return The current rhythm corrected with the new beat
     */
    private static Rhythm correctRhythm(Rhythm currentRhythm, Beat newBeat) {
        // Align beat offset
        long currentTime = System.currentTimeMillis();
        long referenceBeat = currentRhythm.getBeat().closestBeat(currentTime);
        long currentBeatNumber = currentRhythm.getBeat().beatNumber(referenceBeat);
        long newBeatNumber = newBeat.beatNumber(newBeat.closestBeat(referenceBeat));
        int barBeats = currentRhythm.getTimeSignature().getBeats();
        int beatOffsetDiff = Math.toIntExact(
                Math.floorMod(newBeatNumber, barBeats) - Math.floorMod(currentBeatNumber, barBeats));
        return new Rhythm(newBeat, currentRhythm.getTimeSignature(), currentRhythm.getBeatOffset() + beatOffsetDiff);
    }

}
