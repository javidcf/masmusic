package uk.ac.bath.masmusic.common;

/**
 * A time signature indicating the number of beats and the beats unit.
 *
 * @author Javier Dehesa
 */
public class TimeSignature {

    /** Number of beats on each bar. */
    private final int beats;

    /** Beat unit (1 = whole, 2 = half, 4 = quarter, etc). */
    private final int unit;

    /**
     * Constructor.
     *
     * @param beats
     *            The number of beats on each bar
     * @param unit
     *            The beat unit (1 = whole, 2 = half, 4 = quarter, etc)
     */
    public TimeSignature(int beats, int unit) {
        this.beats = beats;
        this.unit = unit;
    }

    /**
     * @return The number of beats on each bar
     */
    public int getBeats() {
        return beats;
    }

    /**
     * @return The beat unit (1 = whole, 2 = half, 4 = quarter, etc)
     */
    public int getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "TimeSignature [beats=" + beats + ", unit=" + unit + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + beats;
        result = prime * result + unit;
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
        TimeSignature other = (TimeSignature) obj;
        if (beats != other.beats) {
            return false;
        }
        if (unit != other.unit) {
            return false;
        }
        return true;
    }
}
