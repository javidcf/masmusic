package uk.ac.bath.masmusic.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A musical scale.
 *
 * @author Javier Dehesa
 */
public class Scale {

    /** Fundamental note of the scale. */
    private final Note fundamental;

    /** Scale name. */
    private final String name;

    /** Intervals of the scale. */
    private final int[] intervals;

    /** Known scale types. */
    private static Map<String, ScaleType> scaleTypes;

    /** Known scales. */
    private static Set<Scale> scales;

    /**
     * A scale type.
     */
    private static class ScaleType {
        final String name;
        final int[] intervals;

        ScaleType(String name, int[] intervals) {
            this.name = name;
            this.intervals = intervals;
        }
    }

    /**
     * Constructor.
     *
     * The name of the scale must match one of the known scale types.
     *
     * @param fundamental
     *            Fundamental note of the scale
     * @param name
     *            Name of the scale
     */
    public Scale(Note fundamental, String name) {
        this(fundamental, name,
                getScaleTypes().get(name.trim().toLowerCase()).intervals);
    }

    /**
     * Constructor.
     *
     * @param fundamental
     *            Fundamental note of the scale
     * @param name
     *            Name of the scale
     * @param intervals
     *            Intervals of the scale
     */
    public Scale(Note fundamental, String name, int[] intervals) {
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

    /**
     * @param note
     *            A note to look up in the scale
     * @return The degree of the note in the scale, or -1 if the note is not in
     *         the scale
     */
    public int degreeOf(Note note) {
        for (int i = 0; i < size(); i++) {
            if (note == getNote(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "Scale [" + fundamental + " " + name + "]";
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
    // @formatter:off
    public static final int[] CHROMATIC_SCALE = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
    public static final int[] MAJOR_SCALE = { 0, 2, 4, 5, 7, 9, 11 };
    public static final int[] MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 11 };
    public static final int[] HARMONIC_MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 11 };
    public static final int[] MELODIC_MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 9, 10, 11 };
    public static final int[] NATURAL_MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 10 };
    public static final int[] DIATONIC_MINOR_SCALE = { 0, 2, 3, 5, 7, 8, 10 };
    public static final int[] AEOLIAN_SCALE = { 0, 2, 3, 5, 7, 8, 10 };
    public static final int[] DORIAN_SCALE = { 0, 2, 3, 5, 7, 9, 10 };
    public static final int[] LYDIAN_SCALE = { 0, 2, 4, 6, 7, 9, 11 };
    public static final int[] MIXOLYDIAN_SCALE = { 0, 2, 4, 5, 7, 9, 10 };
    public static final int[] PENTATONIC_SCALE = { 0, 2, 4, 7, 9 };
    public static final int[] BLUES_SCALE = { 0, 2, 3, 4, 5, 7, 9, 10, 11 };
    public static final int[] TURKISH_SCALE = { 0, 1, 3, 5, 7, 10, 11 };
    public static final int[] INDIAN_SCALE = { 0, 1, 1, 4, 5, 8, 10 };
    // @formatter:on

    /**
     * @return A collection of every known scale.
     */
    public static Set<Scale> getAllScales() {
        if (scales != null) {
            return scales;
        }

        scales = new HashSet<>();
        for (Note note : Note.values()) {
            for (ScaleType scaleType : getScaleTypes().values()) {
                scales.add(
                        new Scale(note, scaleType.name, scaleType.intervals));
            }
        }
        return scales;
    }

    /**
     * @return A map with every known scale type.
     */
    private static Map<String, ScaleType> getScaleTypes() {
        if (scaleTypes != null) {
            return scaleTypes;
        }

        scaleTypes = new HashMap<>();
        //@formatter:off
        scaleTypes.put("major", new ScaleType("Major", MAJOR_SCALE));
        scaleTypes.put("minor", new ScaleType("Minor", MINOR_SCALE));
        // scaleTypes.put("natural minor", new ScaleType("Natural Minor", NATURAL_MINOR_SCALE));
        // scaleTypes.put("harmonic minor", new ScaleType("Harmonic Minor", HARMONIC_MINOR_SCALE));
        // scaleTypes.put("melodic minor", new ScaleType("Melodic Minor", MELODIC_MINOR_SCALE));
        // scaleTypes.put("aeolian", new ScaleType("Aeolian", AEOLIAN_SCALE));
        // scaleTypes.put("dorian", new ScaleType("Dorian", DORIAN_SCALE));
        // scaleTypes.put("lydian", new ScaleType("Lydian", LYDIAN_SCALE));
        // scaleTypes.put("mixolidyan", new ScaleType("Mixolydian", MIXOLYDIAN_SCALE));
        // scaleTypes.put("pentatonic", new ScaleType("Pentatonic", PENTATONIC_SCALE));
        // scaleTypes.put("blues", new ScaleType("Blues", BLUES_SCALE));
        // scaleTypes.put("turkish", new ScaleType("Turkish", TURKISH_SCALE));
        // scaleTypes.put("indian", new ScaleType("Indian", INDIAN_SCALE));
        //@formatter:on
        return scaleTypes;
    }

}