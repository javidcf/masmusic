package uk.ac.bath.masmusic.generation.melody;

import java.util.Random;

import uk.ac.bath.masmusic.common.EvictingCircularBuffer;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Scale;

/**
 * A duration generator based on a Markov state transition table.
 *
 * The generator produces duration values based on the current degree in a scale
 * and a fixed number of previous durations.
 *
 * @author Javier Dehesa
 */
public class MarkovDurationGenerator {

    /** RNG. */
    private final static Random RNG = new Random();

    /** Markov table. */
    private final MarkovDurationGeneratorTable table;

    /** Scale. */
    private final Scale scale;

    /** Last ngram. */
    private final EvictingCircularBuffer<Double> ngram;

    /**
     * Constructor.
     *
     * @param table
     *            The Markov table used by the generator
     * @param scale
     *            The scale used to generate the melody
     */
    public MarkovDurationGenerator(MarkovDurationGeneratorTable table,
            Scale scale) {
        this(table, scale, 0f);
    }

    /**
     * Constructor.
     *
     * @param table
     *            The Markov table used by the generator
     * @param scale
     *            The scale used to generate the melody
     * @param initialDuration
     *            First generated duration
     */
    public MarkovDurationGenerator(MarkovDurationGeneratorTable table,
            Scale scale, double initialDuration) {
        this.table = table;
        this.scale = scale;
        this.ngram = new EvictingCircularBuffer<>(table.getOrder());
        this.ngram.add(initialDuration);
    }

    /**
     * @return The last generated duration value, or the initial one if none has
     *         been generated yet.
     */
    public double getCurrentDuration() {
        return ngram.lastElement();
    }

    /**
     * Generate a new duration.
     *
     * @param pitch
     *            Pitch MIDI value of the note with the previously generated
     *            duration
     * @return The newly generated duration value
     */
    public double generateDuration(int pitch) {
        Note currentNote = Note.fromValue(pitch);
        int relPitch = scale.getFundamental().ascendingDistanceTo(currentNote);
        double r = RNG.nextDouble();
        Double duration = table.pickDuration(relPitch, ngram, r);
        if (duration == null) {
            // Random walk
            switch (RNG.nextInt(3)) {
            case 0:
                duration = ngram.lastElement();
                break;
            case 1:
                duration = ngram.lastElement() / 2.0;
                if (duration < .25) {
                    duration = ngram.lastElement();
                }
                break;
            case 2:
                duration = ngram.lastElement() * 2.0;
                if (duration > 2.0) {
                    duration = ngram.lastElement();
                }
                break;
            default:
                throw new AssertionError();
            }
        }
        ngram.add(duration);
        return duration;
    }

    /**
     * Provide a duration to the generator.
     *
     * The provided duration will become the current one, and the generator will
     * transition the corresponding status.
     *
     * @param duration
     *            The duration provided to the generator
     */
    public void provideDuration(double duration) {
        ngram.add(duration);
    }
}
