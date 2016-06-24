package uk.ac.bath.masmusic.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An element of a score.
 *
 * @author Javier Dehesa
 */
public class ScoreElement {

    /** Duration of the element in beats. */
    private final float duration;

    private final List<Note> notes;

    /**
     * Constructor.
     *
     * @param duration
     *            Duration of the element in beats
     * @param notes
     *            Notes in the element
     */
    public ScoreElement(float duration, Collection<Note> notes) {
        this.duration = duration;
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
    }

    /**
     * @return Duration of the element in beats
     */
    public float getDuration() {
        return duration;
    }

    /**
     * @return Notes in the element
     */
    public List<Note> getNotes() {
        return notes;
    }

}
