package uk.ac.bath.masmusic.common;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A chord represented as an octave-independent structure.
 *
 * @author Javier Dehesa
 */
public class Chord {

    /** Chord root. */
    private final Note root;

    /** Chord structure in half-steps with respect to the root. */
    private final int[] structure;

    /**
     * Constructor.
     *
     * @param root
     *            The chord root
     * @param structure
     *            The chord structure in half-steps with respect to the root
     */
    public Chord(Note root, int[] structure) {
        this.root = root;
        this.structure = Arrays.copyOf(structure, structure.length);
    }

    /**
     * Constructor.
     *
     * @param root
     *            The chord root
     * @param structure
     *            The chord structure in half-steps with respect to the root
     */
    public Chord(Note root, List<Integer> structure) {
        this.root = root;
        this.structure = structure.stream().mapToInt(i -> i).toArray();
    }

    /**
     * @return The chord root
     */
    public Note getRoot() {
        return root;
    }

    /**
     * Get the pitch of the root of the chord for a given octave.
     *
     * @param octave
     *            The octave, where 4 is the octave containing central C
     * @return The pitch of the chord root in the given octave
     */
    public int getRootPitch(int octave) {
        return (octave + 1) * 12 + root.value();
    }

    /**
     * Get the pitches of the chord on the given octave.
     *
     * @param octave
     *            The octave, where 4 is the octave containing central C
     * @return The pitches of the chord in the given octave
     */
    public List<Integer> getPitches(int octave) {
        int basePitch = (octave + 1) * 12 + root.value();
        return Arrays.stream(structure).map(i -> basePitch + i).boxed().collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Chord [root=" + root + ", structure=" + Arrays.toString(structure) + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((root == null) ? 0 : root.hashCode());
        result = prime * result + Arrays.hashCode(structure);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Chord other = (Chord) obj;
        if (root != other.root) {
            return false;
        }
        if (!Arrays.equals(structure, other.structure)) {
            return false;
        }
        return true;
    }

}
