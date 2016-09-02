package uk.ac.bath.masmusic.mas;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.events.RhythmUpdatedEvent;
import uk.ac.bath.masmusic.events.ScaleUpdatedEvent;
import uk.ac.bath.masmusic.generation.melody.MelodyLearner;

/**
 * Melody copycat.
 *
 * Imitates the received melody using a {@link MelodyLearner}.
 *
 * @author Javier Dehesa
 */
@Component
public class MelodyCopycat {

    private static final int LEARNER_NUM_BAR_CLASSES = 4;
    private static final int LEARNER_NUM_BAR_PHRASES = 5;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MelodyCopycat.class);

    /** Current rhythm. */
    private Rhythm rhythm;

    /** Current rhythm. */
    private Scale scale;

    /** RNG. */
    private final Random rng;

    /** Melody learner. */
    private MelodyLearner learner;

    /**
     * Constructor.
     */
    public MelodyCopycat() {
        rhythm = null;
        scale = null;
        rng = new Random();
        learner = null;
    }

    /**
     * Handle a rhythm update event.
     *
     * @param event
     *            The rhythm update event
     */
    @EventListener
    public synchronized void onRhythmUpdated(RhythmUpdatedEvent event) {
        setRhythm(event.getRhythm());
    }

    /**
     * Set the music rhythm.
     *
     * @param rhythm
     *            The new rhythm
     */
    private synchronized void setRhythm(Rhythm rhythm) {
        if (this.rhythm == null || !this.rhythm.getTimeSignature().equals(rhythm.getTimeSignature())) {
            learner = new MelodyLearner(LEARNER_NUM_BAR_CLASSES, LEARNER_NUM_BAR_PHRASES);
        }
        this.rhythm = rhythm;
        learner.setRhythm(rhythm);
    }

    /**
     * Handle a scale update event.
     *
     * @param event
     *            The scale update event
     */
    @EventListener
    public void onScaleUpdated(ScaleUpdatedEvent event) {
        setScale(event.getScale());
    }

    /**
     * Set the music scale.
     *
     * @param scale
     *            The new scale
     */
    private synchronized void setScale(Scale scale) {
        if (this.scale == null || !this.scale.equals(scale)) {
            learner = new MelodyLearner(LEARNER_NUM_BAR_CLASSES, LEARNER_NUM_BAR_PHRASES);
            if (this.rhythm != null) {
                learner.setRhythm(this.rhythm);
            }
        }
        this.scale = scale;
    }

    /**
     * Get a number of bars selected randomly from the learned music.
     *
     * @param scale
     *            The music scale
     * @param rhythm
     *            The music rhythm
     * @param timestamp
     *            Timestamp of the first harmony bar; if the timestamp does not
     *            match exactly the beginning of a bar, then the next closest
     *            bar will be the first one
     * @param bars
     *            Length of the generated melody in bars
     * @return The generated melody, or an empty list if no melody could be
     *         generated
     */
    public synchronized List<Onset> getRandomBars(Scale scale, Rhythm rhythm, long timestamp, int bars) {
        Objects.requireNonNull(scale);
        Objects.requireNonNull(rhythm);
        setScale(scale);
        setRhythm(rhythm);
        return getRandomBars(timestamp, bars);
    }

    /**
     * Get a number of bars selected randomly from the learned music.
     *
     * @param timestamp
     *            Timestamp of the first harmony bar; if the timestamp does not
     *            match exactly the beginning of a bar, then the next closest
     *            bar will be the first one
     * @param bars
     *            Length of the generated melody in bars
     * @return The generated melody, or an empty list if no melody could be
     *         generated
     */
    public synchronized List<Onset> getRandomBars(long timestamp, int bars) {
        if (bars < 0) {
            throw new IllegalArgumentException("The number of bars cannot be negative");
        }
        if (rhythm != null && scale != null && learner != null) {
            return learner.getRandomBars(timestamp, bars, MasMusic.DEFAULT_VELOCITY, rng);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Learn patterns from the given music.
     *
     * @param scale
     *            The music scale
     * @param rhythm
     *            The music rhythm
     * @param onsets
     *            Sequence of played notes
     */
    public synchronized void learn(Scale scale, Rhythm rhythm, List<Onset> onsets) {
        Objects.requireNonNull(scale);
        Objects.requireNonNull(rhythm);
        setScale(scale);
        setRhythm(rhythm);
        learn(onsets);
    }

    /**
     * Learn patterns from the given music.
     *
     * @param onsets
     *            Sequence of played notes
     */
    public void learn(List<Onset> onsets) {
        LOG.debug("Harmonizing melody in {}", scale);
        if (scale == null || rhythm == null || learner == null) {
            return;
        }
        learner.learn(onsets);
    }
}
