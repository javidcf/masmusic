package uk.ac.bath.masmusic.events;

import org.springframework.context.ApplicationEvent;

import uk.ac.bath.masmusic.common.Rhythm;

/**
 * An event informing that the {@link Rhythm} estimation has been updated.
 *
 * @author Javier Dehesa
 */
public class RhythmUpdatedEvent extends ApplicationEvent {

    /** Rhythm. */
    private final Rhythm rhythm;

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param source
     *            Event source
     * @param rhythm
     *            The rhythm
     */
    public RhythmUpdatedEvent(Object source, Rhythm rhythm) {
        super(source);
        this.rhythm = rhythm;
    }

    /**
     * @return The rhythm
     */
    public Rhythm getRhythm() {
        return rhythm;
    }
}
