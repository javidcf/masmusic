package uk.ac.bath.masmusic.events;

import org.springframework.context.ApplicationEvent;

import uk.ac.bath.masmusic.common.Scale;

/**
 * An event informing that the {@link Scale} estimation has been updated.
 *
 * @author Javier Dehesa
 */
public class ScaleUpdatedEvent extends ApplicationEvent {

    /** Scale. */
    private final Scale scale;

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param source
     *            Event source
     * @param scale
     *            The scale
     */
    public ScaleUpdatedEvent(Object source, Scale scale) {
        super(source);
        this.scale = scale;
    }

    /**
     * @return The scale
     */
    public Scale getScale() {
        return scale;
    }
}
