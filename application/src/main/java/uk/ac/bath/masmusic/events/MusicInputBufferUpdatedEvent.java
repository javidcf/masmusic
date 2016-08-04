package uk.ac.bath.masmusic.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationEvent;

import uk.ac.bath.masmusic.common.Onset;

/**
 * An event indicating that the music input buffer has been updated.
 *
 * @author Javier Dehesa
 */
public class MusicInputBufferUpdatedEvent extends ApplicationEvent {

    /** Input buffer. */
    private final List<Onset> inputBuffer;

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param source
     *            Event source
     * @param inputBuffer
     *            The input buffer
     */
    public MusicInputBufferUpdatedEvent(Object source, List<Onset> inputBuffer) {
        super(source);
        this.inputBuffer = Collections.unmodifiableList(new ArrayList<>(inputBuffer));
    }

    /**
     * @return The input buffer
     */
    public List<Onset> getInputBuffer() {
        return inputBuffer;
    }

}
