package uk.ac.bath.masmusic.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * An element of a score.
 *
 * @author Javier Dehesa
 */
public class ScoreElement {

    /** Duration of the element in beats. */
    private final double duration;

    /** MIDI pitch values in the element. */
    private final List<Integer> pitches;

    /**
     * Constructor.
     *
     * @param duration
     *            Duration of the element in beats
     * @param pitches
     *            MIDI pitch values in the element
     */
    public ScoreElement(double duration, Collection<Integer> pitches) {
        this.duration = duration;
        // Create immutable list without repetitions
        this.pitches = Collections.unmodifiableList(new ArrayList<>(new HashSet<>(pitches)));
    }

    /**
     * @return Duration of the element in beats
     */
    public double getDuration() {
        return duration;
    }

    /**
     * @return MIDI pitch values in the element
     */
    public Collection<Integer> getPitches() {
        return pitches;
    }

    @Override
    public String toString() {
        return "ScoreElement [duration=" + duration + ", pitches=" + pitches + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(duration);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((pitches == null) ? 0 : pitches.hashCode());
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
        ScoreElement other = (ScoreElement) obj;
        if (Double.doubleToLongBits(duration) != Double.doubleToLongBits(other.duration)) {
            return false;
        }
        if (pitches == null) {
            if (other.pitches != null) {
                return false;
            }
        } else if (!pitches.equals(other.pitches)) {
            return false;
        }
        return true;
    }

}
