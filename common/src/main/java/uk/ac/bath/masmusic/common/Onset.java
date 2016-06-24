package uk.ac.bath.masmusic.common;

/**
 * A note onset event.
 *
 * @author Javier Dehesa
 */
public class Onset {

    /** Onset timestamp (ms). */
    private final long timestamp;
    /** Onset duration, or 0 if not available (ms). */
    private final int duration;
    /** Onset pitch value, in MIDI scale. */
    private final int pitch;
    /** Onset velocity. */
    private final int velocity;

    /**
     * Constructor.
     *
     * @param timestamp
     *            Onset timestamp (ms)
     * @param pitch
     *            Onset pitch value, in MIDI scale
     * @param velocity
     *            Onset velocity
     */
    public Onset(long timestamp, int pitch, int velocity) {
        this(timestamp, 0, pitch, velocity);
    }

    /**
     * Constructor.
     *
     * @param timestamp
     *            Onset timestamp (ms)
     * @param duration
     *            Onset duration, or 0 if not available (ms)
     * @param pitch
     *            Onset pitch value, in MIDI scale
     * @param velocity
     *            Onset velocity
     */
    public Onset(long timestamp, int duration, int pitch, int velocity) {
        this.timestamp = timestamp;
        this.duration = duration;
        this.pitch = pitch;
        this.velocity = velocity;
    }

    /**
     * @return Onset timestamp (ms)
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return Onset duration, or 0 if not available (ms)
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @return Onset pitch value, in MIDI scale
     */
    public int getPitch() {
        return pitch;
    }

    /**
     * @return Onset velocity
     */
    public int getVelocity() {
        return velocity;
    }
}
