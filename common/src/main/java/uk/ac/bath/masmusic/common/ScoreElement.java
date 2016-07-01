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
    private final float duration;

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
    public ScoreElement(float duration, Collection<Integer> pitches) {
        this.duration = duration;
        // Create immutable list without repetitions
        this.pitches = Collections.unmodifiableList(new ArrayList<>(new HashSet<>(pitches)));
    }

    /**
     * @return Duration of the element in beats
     */
    public float getDuration() {
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

}
