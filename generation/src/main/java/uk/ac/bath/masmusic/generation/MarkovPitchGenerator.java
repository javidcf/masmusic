package uk.ac.bath.masmusic.generation;

import java.util.Random;

import uk.ac.bath.masmusic.common.EvictingCircularBuffer;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Scale;

/**
 * A pitch generator based on a Markov state transition table.
 *
 * The generator produces pitch values based on the current degree in a scale
 * and a fixed number of pitch transitions.
 *
 * @author Javier Dehesa
 */
public class MarkovPitchGenerator {

    /** RNG. */
    private final static Random RNG = new Random();

    /** Markov table. */
    private final MarkovPitchGeneratorTable table;

    /** Scale. */
    private final Scale scale;

    /** Current pitch MIDI value. */
    private int currentPitch;

    /** Last ngram. */
    private final EvictingCircularBuffer<Integer> ngram;

    /**
     * Constructor.
     *
     * @param table
     *            The Markov table used by the generator
     * @param scale
     *            The scale used to generate the melody
     */
    public MarkovPitchGenerator(MarkovPitchGeneratorTable table, Scale scale) {
        this(table, scale, 0);
    }

    /**
     * Constructor.
     *
     * @param table
     *            The Markov table used by the generator
     * @param scale
     *            The scale on which the music is generated
     * @param scale
     *            The scale used to generate the melody
     */
    public MarkovPitchGenerator(MarkovPitchGeneratorTable table, Scale scale, int initialPitch) {
        this.table = table;
        this.scale = scale;
        this.currentPitch = initialPitch;
        this.ngram = new EvictingCircularBuffer<>(table.getOrder());
    }

    /**
     * @return The last generated pitch MIDI value, or the initial one if none
     *         has been generated yet.
     */
    public int getCurrentPitch() {
        return currentPitch;
    }

    /**
     * Generate a new pitch.
     *
     * @return The newly generated pitch MIDI value
     */
    public int generatePitch() {
        Note currentNote = Note.fromValue(currentPitch);
        int relPitch = scale.getFundamental().ascendingDistanceTo(currentNote);
        double r = RNG.nextDouble();
        Integer step = table.pickStep(relPitch, ngram, r);
        if (step == null) {
            // Random walk
            int degree = scale.degreeWithAlterationOf(currentNote);
            int nextDegree = Math.floorMod(((degree - 1) + (RNG.nextBoolean() ? -1 : +1)), scale.size()) + 1;
            Note nextNote = scale.getNote(nextDegree);
            step = currentNote.distanceTo(nextNote);
        }
        ngram.add(step);
        currentPitch += step;
        return currentPitch;
    }

    /**
     * Provide a pitch to the generator.
     *
     * The provided pitch will become the current one, and the generator status
     * will transition to the status corresponding to the pitch transition
     * between the previous and the new one.
     *
     * @param pitch
     *            The pitch provided to the generator
     */
    public void providePitch(int pitch) {
        int step = pitch - currentPitch;
        ngram.add(step);
        currentPitch = pitch;
    }
}
