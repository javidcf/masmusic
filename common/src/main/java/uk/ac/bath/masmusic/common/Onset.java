package uk.ac.bath.masmusic.common;

import java.util.Comparator;

/**
 * A note onset event.
 *
 * Onsets are hashable and comparable by timestamp, duration, pitch and
 * velocity.
 *
 * @author Javier Dehesa
 */
public class Onset implements Comparable<Onset> {

    /** Onset timestamp (ms). */
    private final long timestamp;
    /** Onset duration, or 0 if not available (ms). */
    private final int  duration;
    /** Onset pitch value, in MIDI scale. */
    private final int  pitch;
    /** Onset velocity. */
    private final int  velocity;

    /** Comparator. */
    private static final Comparator<Onset> COMPARATOR = Comparator
            .comparing(Onset::getTimestamp)
            .thenComparing(Onset::getDuration)
            .thenComparing(Onset::getPitch)
            .thenComparing(Onset::getVelocity);;

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

    @Override
    public String toString() {
        return "Onset [timestamp=" + timestamp + ", duration=" + duration + ", pitch=" + pitch + ", velocity="
                + velocity + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + duration;
        result = prime * result + pitch;
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + velocity;
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
        Onset other = (Onset) obj;
        if (duration != other.duration) {
            return false;
        }
        if (pitch != other.pitch) {
            return false;
        }
        if (timestamp != other.timestamp) {
            return false;
        }
        if (velocity != other.velocity) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Onset that) {
        return COMPARATOR.compare(this, that);
    }
}
