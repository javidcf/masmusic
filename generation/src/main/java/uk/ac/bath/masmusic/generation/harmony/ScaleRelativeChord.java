package uk.ac.bath.masmusic.generation.harmony;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.bath.masmusic.common.Chord;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Scale;

/**
 * A chord, expressed as a root relative to the scale fundamental and a
 * collection of pitches relative to the root.
 *
 * @author Javier Dehesa
 */
public class ScaleRelativeChord {

    /** Chord root in half-steps with respect to the scale fundamental. */
    private final int root;

    /** Chord structure in half-steps with respect to the root. */
    private final List<Integer> structure;

    /**
     * Constructor.
     *
     * @param root
     *            Chord root in half-steps with respect to the scale fundamental
     * @param structure
     *            Chord structure in half-steps with respect to the root
     */
    public ScaleRelativeChord(int root, Collection<Integer> structure) {
        this.root = root % 12;
        List<Integer> structureList = new ArrayList<>(new HashSet<>(structure));
        Collections.sort(structureList);
        this.structure = Collections.unmodifiableList(structureList);
    }

    public static ScaleRelativeChord fromChord(Note fundamental, Chord chord) {
        int relativeRoot = fundamental.ascendingDistanceTo(chord.getRoot());
        List<Integer> structure = chord.getPitches(4).stream()
                .map(pitch -> pitch - chord.getRootPitch(4)).collect(Collectors.toList());
        return new ScaleRelativeChord(relativeRoot, structure);
    }

    /**
     * @return The chord root in half-steps with respect to the scale
     *         fundamental
     */
    public final int getRoot() {
        return root;
    }

    /**
     * Get a chord for a particular tonality.
     *
     * @param fundamental
     *            The scale fundamental
     * @return A chord for the given fundamental
     */
    public Chord getChord(Note fundamental) {
        return new Chord(fundamental.increasedBy(root), structure);
    }

    /**
     * Get a chord for a particular tonality.
     *
     * @param fundamental
     *            The scale
     * @return A chord for the given scale
     */
    public Chord getChord(Scale scale) {
        return getChord(scale.getFundamental());
    }

    @Override
    public String toString() {
        return "ScaleRelativeChord [root=" + root + ", structure=" + structure + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + root;
        result = prime * result + ((structure == null) ? 0 : structure.hashCode());
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
        ScaleRelativeChord other = (ScaleRelativeChord) obj;
        if (root != other.root) {
            return false;
        }
        if (structure == null) {
            if (other.structure != null) {
                return false;
            }
        } else if (!structure.equals(other.structure)) {
            return false;
        }
        return true;
    }
}
