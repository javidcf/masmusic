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

    /** Scale type. */
    private final String type;

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
        final String type;
        final int[]  intervals;

        ScaleType(String type, int[] intervals) {
            if (intervals.length < 1) {
                throw new IllegalArgumentException("A scale must have at least one interval");
            }
            this.type = type;
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
     * @param type
     *            Type name of the scale
     */
    public Scale(Note fundamental, String type) {
        this(fundamental, type,
                getScaleTypes().get(type.trim().toLowerCase()).intervals);
    }

    /**
     * Constructor.
     *
     * @param fundamental
     *            Fundamental note of the scale
     * @param type
     *            Type name of the scale
     * @param intervals
     *            Intervals of the scale
     */
    public Scale(Note fundamental, String type, int[] intervals) {
        if (intervals.length < 1) {
            throw new IllegalArgumentException("A scale must have at least one interval");
        }
        int lastInterval = -1;
        for (int interval : intervals) {
            if (interval <= lastInterval || interval >= 12) {
                throw new IllegalArgumentException("Invalid intervals");
            }
            lastInterval = interval;
        }
        this.type = type;
        this.fundamental = fundamental;
        this.intervals = intervals;
    }

    /**
     * @return The type of the scale.
     */
    public String getType() {
        return type;
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
        if (degree < 1 || degree > intervals.length) {
            throw new IllegalArgumentException("Invalid scale degree");
        }
        return fundamental.increasedBy(intervals[degree - 1]);
    }

    /**
     * @param note
     *            A note to look up in the scale
     * @return The degree of the note in the scale (starting from 1), or -1 if
     *         the note is not in the scale
     */
    public int degreeOf(Note note) {
        for (int i = 1; i <= size(); i++) {
            if (note == getNote(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param note
     *            A note to look up in the scale
     * @return The degree of the note in the scale (starting from 1); if it is
     *         not in the scale, the note is considered to be an alteration and
     *         the base degree is returned
     */
    public int degreeWithAlterationOf(Note note) {
        int deg = degreeOf(note);
        if (deg > 0) {
            return deg;
        }
        if (isHeptatonic()) {
            // Try heptatonic conventions
            // TODO Not so sure about these ones...
            if (note.ascendingDistanceTo(getNote(2)) == 1) {
                return 2;
            } else if (note.absoluteDistanceTo(getNote(3)) == 1) {
                return 3;
            } else if (note.ascendingDistanceTo(getNote(5)) == 1) {
                return 5;
            } else if (note.absoluteDistanceTo(getNote(6)) == 1) {
                return 6;
            } else if (note.absoluteDistanceTo(getNote(7)) == 1) {
                return 7;
            }
        }

        // Pick whichever is closest
        int best = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 1; i <= size(); i++) {
            Note scaleNote = getNote(i);
            int distance = note.absoluteDistanceTo(scaleNote);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        if (best < 1) {
            throw new AssertionError("Scale is empty");
        }
        return best;
    }

    /**
     * @param note
     *            A note to look up in the scale
     * @return The alteration of the note in the scale in half steps
     */
    public int alterationOf(Note note) {
        int deg = degreeWithAlterationOf(note);
        return getNote(deg).distanceTo(note);
    }

    /**
     * @return Whether the scale is heptatonic
     */
    public boolean isHeptatonic() {
        return size() == 7;
    }

    @Override
    public String toString() {
        return "Scale [" + fundamental + " " + type + "]";
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
                scales.add(new Scale(note, scaleType.type, scaleType.intervals));
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
