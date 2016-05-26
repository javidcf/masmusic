package uk.ac.bath.masmusic.key;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A musical scale.
 *
 * @author Javier Dehesa
 */
public class Scale {
    /** Scale name. */
    private final String name;

    /** Fundamental note of the scale. */
    private final Note fundamental;

    /** Intervals of the scale. */
    private final int[] intervals;

    public Scale(String name, Note fundamental, int[] intervals) {
        int lastInterval = -1;
        for (int interval : intervals) {
            if (interval <= lastInterval || interval >= 12) {
                throw new IllegalArgumentException("Invalid intervals");
            }
        }
        this.name = name;
        this.fundamental = fundamental;
        this.intervals = intervals;
    }

    /**
     * @return The name of the scale.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The number of notes in the scale
     */
    public int size() {
        return intervals.length;
    }

    /**
     * @return The fundamental note of the scale
     */
    public Note getFundamental() {
        return fundamental;
    }

    /**
     * @param degree
     *            A degree of the scale
     * @return The interval of the given degree measured in half steps
     */
    public int getInterval(int degree) {
        if (degree < 0 || degree >= intervals.length) {
            throw new IllegalArgumentException("Invalid scale degree");
        }
        return intervals[degree];
    }

    /**
     * @param degree
     *            A degree of the scale
     * @return The note of the given degree in the scale
     */
    public Note getNote(int degree) {
        if (degree < 0 || degree >= intervals.length) {
            throw new IllegalArgumentException("Invalid scale degree");
        }
        return Note.fromValue(fundamental.value() + intervals[degree]);
    }

    @Override
    public String toString() {
        return fundamental + " " + name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((fundamental == null) ? 0 : fundamental.hashCode());
        result = prime * result + Arrays.hashCode(intervals);
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
        Scale other = (Scale) obj;
        if (fundamental != other.fundamental) {
            return false;
        }
        if (!Arrays.equals(intervals, other.intervals)) {
            return false;
        }
        return true;
    }

    // Taken from jMusic (http://explodingart.com/jmusic/)
    //@formatter:off
    public static final int[] CHROMATIC_SCALE = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    public static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    public static final int[] MINOR_SCALE = {0, 2, 3, 5, 7, 8, 11};
    public static final int[] HARMONIC_MINOR_SCALE = {0, 2, 3, 5, 7, 8, 11};
    public static final int[] MELODIC_MINOR_SCALE = {0, 2, 3, 5, 7, 8, 9, 10, 11};
    public static final int[] NATURAL_MINOR_SCALE = {0, 2, 3, 5, 7, 8, 10};
    public static final int[] DIATONIC_MINOR_SCALE = {0, 2, 3, 5, 7, 8, 10};
    public static final int[] AEOLIAN_SCALE = {0, 2, 3, 5, 7, 8, 10};
    public static final int[] DORIAN_SCALE = {0, 2, 3, 5, 7, 9, 10};
    public static final int[] LYDIAN_SCALE = {0, 2, 4, 6, 7, 9, 11};
    public static final int[] MIXOLYDIAN_SCALE = {0, 2, 4, 5, 7, 9, 10};
    public static final int[] PENTATONIC_SCALE = {0, 2, 4, 7, 9};
    public static final int[] BLUES_SCALE = {0, 2, 3, 4, 5, 7, 9, 10, 11};
    public static final int[] TURKISH_SCALE = {0, 1, 3, 5, 7, 10, 11};
    public static final int[] INDIAN_SCALE = {0, 1, 1, 4, 5, 8, 10};
    //@formatter:on

    /**
     * @return A collection of every known scale.
     */
    public static Set<Scale> createScales() {
        Set<Scale> scales = new HashSet<>();
        for (Note note : Note.values()) {
            scales.add(new Scale("Major", note, MAJOR_SCALE));
            scales.add(new Scale("Natural Minor", note, NATURAL_MINOR_SCALE));
            scales.add(new Scale("Harmonic Minor", note, HARMONIC_MINOR_SCALE));
            scales.add(new Scale("Melodic Minor", note, MELODIC_MINOR_SCALE));
            scales.add(new Scale("Dorian", note, DORIAN_SCALE));
            scales.add(new Scale("Lydian", note, LYDIAN_SCALE));
            scales.add(new Scale("Mixolydian", note, MIXOLYDIAN_SCALE));
            scales.add(new Scale("Pentatonic", note, PENTATONIC_SCALE));
            scales.add(new Scale("Blues", note, BLUES_SCALE));
            scales.add(new Scale("Turkish", note, TURKISH_SCALE));
            scales.add(new Scale("Indian", note, INDIAN_SCALE));
        }
        return scales;
    }

}
