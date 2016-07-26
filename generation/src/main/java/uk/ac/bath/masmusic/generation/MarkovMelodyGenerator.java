package uk.ac.bath.masmusic.generation;

import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.ScoreElement;

/**
 * A pitch generator based on a Markov state transition table.
 *
 * The generator produces pitch values based on the current degree in a scale
 * and a fixed number of pitch transitions.
 *
 * @author Javier Dehesa
 */
public class MarkovMelodyGenerator {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MarkovMelodyGenerator.class);

    /** Pitch generator. */
    private final MarkovPitchGenerator pitchGenerator;

    /** Duration generator. */
    private final MarkovDurationGenerator durationGenerator;

    /** Melody scale. */
    private final Scale scale;

    /** Last generated element. */
    private ScoreElement currentElement;

    /**
     * Constructor.
     *
     * @param pitchTable
     *            The Markov pitch table used by the generator
     * @param durationTable
     *            The Markov duration table used by the generator
     * @param scale
     *            The scale used to generate the melody
     */
    public MarkovMelodyGenerator(MarkovPitchGeneratorTable pitchTable, MarkovDurationGeneratorTable durationTable,
            Scale scale) {
        this(pitchTable, durationTable, scale, 60 + scale.getFundamental().value(), 1f);
    }

    /**
     * Constructor.
     *
     * @param pitchTable
     *            The Markov pitch table used by the generator
     * @param durationTable
     *            The Markov duration table used by the generator
     * @param scale
     *            The scale used to generate the melody
     * @param initialPitch
     *            Initial pitch of the generated melody
     * @param initialDuration
     *            Initial duration of the generated melody
     */
    public MarkovMelodyGenerator(MarkovPitchGeneratorTable pitchTable, MarkovDurationGeneratorTable durationTable,
            Scale scale, int initialPitch, float initialDuration) {
        this.pitchGenerator = new MarkovPitchGenerator(pitchTable, scale, initialPitch);
        this.durationGenerator = new MarkovDurationGenerator(durationTable, scale, initialDuration);
        this.scale = scale;
        this.currentElement = new ScoreElement(initialDuration, Collections.singleton(initialPitch));
    }

    /**
     * @return The generator scale
     */
    public Scale getScale() {
        return scale;
    }

    /**
     * Set the limits for the generated pitch values.
     *
     * @param low
     *            Pitch low bound (inclusive)
     * @param high
     *            Pitch high bound (inclusive)
     */
    public void setPitchBounds(int low, int high) {
        pitchGenerator.setPitchBounds(low, high);
    }

    /**
     * @return The last generated melody element, or the initial one if none has
     *         been generated yet.
     */
    public ScoreElement getCurrentElement() {
        return currentElement;
    }

    /**
     * Generate a new melody element.
     *
     * @return The newly generated melody element
     */
    public ScoreElement generateElement() {
        double nextDuration = durationGenerator.generateDuration(pitchGenerator.getCurrentPitch());
        int nextPitch = pitchGenerator.generatePitch();
        currentElement = new ScoreElement(nextDuration, Collections.singleton(nextPitch));
        return currentElement;
    }

    /**
     * Provide a score element to the generator.
     *
     * The provided score element will become the current one, and the generator
     * will transition to the corresponding status.
     *
     * @param element
     *            The element provided to the generator
     */
    public void provideElement(ScoreElement element) {
        Collection<Integer> pitches = element.getPitches();
        if (pitches.isEmpty()) {
            throw new IllegalArgumentException("The provided element must contain a pitch value");
        }
        if (pitches.size() > 1) {
            LOG.warn("The provided element contains multiple pitch values but only one will be used");
        }
        int pitch = pitches.iterator().next();
        pitchGenerator.providePitch(pitch);
        durationGenerator.provideDuration(element.getDuration());
        currentElement = element;
    }
}
