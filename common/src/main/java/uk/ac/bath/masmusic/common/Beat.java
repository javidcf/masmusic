package uk.ac.bath.masmusic.common;

/**
 * A beat specification.
 *
 * Defines the beat duration and the time phase.
 *
 * @author Javier Dehesa
 */
public class Beat implements Cloneable {

    /** Beat duration (ms) */
    private int duration;
    /** Time phase (ms) */
    private int phase;

    /**
     * Constructor.
     *
     * Creates a beat with the given duration an phase 0.
     *
     * @param duration
     *            Beat duration in milliseconds
     */
    public Beat(int duration) {
        this(duration, 0);
    }

    /**
     * Constructor.
     *
     * @param duration
     *            Beat duration in milliseconds
     * @param timestamp
     *            Timestamp of a beat
     */
    public Beat(int duration, long timestamp) {
        if (duration <= 1) {
            throw new IllegalArgumentException(
                    "The duration must be greater than 1");
        }
        this.duration = duration;
        this.phase = (int) Math.floorMod(timestamp, this.duration);
    }

    /**
     * @return Beat duration in milliseconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @return Beat time phase in milliseconds
     */
    public int getPhase() {
        return phase;
    }

    /**
     * @return Beat tempo, in beats per minute
     */
    public int getTempo() {
        return Math.round(((float) 60000) / duration);
    }

    /**
     * Get the number of the beat at the given timestamp.
     *
     * @param timestamp
     *            Time at which the beat number is computed.
     * @return The beat number at the given timestamp
     */
    public long beatNumber(long timestamp) {
        return (timestamp - phase) / duration;
    }

    /**
     * Get the time of a beat given by its number.
     *
     * @param beatNumber
     *            The beat number
     * @return The timestamp of the given beat number
     */
    public long byNumber(long beatNumber) {
        return beatNumber * duration + phase;
    }

    /**
     * Get the timestamp of the last beat that happened at the given time.
     *
     * The returned timestamp is always preceding or equal to the given one.
     *
     * @param timestamp
     *            Time at which the beat time is computed
     * @return The last beat that happened at the given timestamp
     */
    public long currentBeat(long timestamp) {
        return byNumber(beatNumber(timestamp));
    }

    /**
     * Get the timestamp of the next beat after the given time.
     *
     * The returned timestamp is always posterior to the given one.
     *
     * @param timestamp
     *            Time at which the next beat time is computed
     * @return The next beat that will happen after the given timestamp
     */
    public long nextBeat(long timestamp) {
        return currentBeat(timestamp) + duration;
    }

    /**
     * Get the timestamp of the beat that is closest to the given timestamp.
     *
     * The returned timestamp may be equal, preceding or posterior to the given
     * timestamp.
     *
     * @param timestamp
     *            Time at which the closest beat time is computed
     * @return The closest beat to the timestamp
     */
    public long closestBeat(long timestamp) {
        long prev = currentBeat(timestamp);
        long next = nextBeat(timestamp);
        if ((timestamp - prev) <= (next - timestamp)) {
            return prev;
        } else {
            return next;
        }
    }

    /**
     * Get the timestamp of the last subbeat that happened at the given time.
     *
     * The returned timestamp is always preceding or equal to the given one.
     *
     * @param timestamp
     *            Time at which the subbeat time is computed
     * @param binarySubdivision
     *            Level of binary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 2 subdivisions, 2 for 4, etc.)
     * @return The last subbeat that happened at the given timestamp
     */
    public long currentSubbeat(long timestamp, int binarySubdivision) {
        return currentSubbeat(timestamp, binarySubdivision, 0);
    }

    /**
     * Get the timestamp of the last subbeat that happened at the given time.
     *
     * The returned timestamp is always preceding or equal to the given one.
     *
     * @param timestamp
     *            Time at which the subbeat time is computed
     * @param binarySubdivision
     *            Level of binary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 2 subdivisions, 2 for 4, etc.)
     * @param ternarySubdivision
     *            Level of ternary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 3 subdivisions, 2 for 9, etc.)
     * @return The last subbeat that happened at the given timestamp
     */
    public long currentSubbeat(long timestamp, int binarySubdivision, int ternarySubdivision) {
        if (binarySubdivision < 0) {
            throw new IllegalArgumentException(
                    "The allowed subdivision level cannot be negative");
        }
        long prevBeat = currentBeat(timestamp);
        int binarySubbeatDuration = Math.toIntExact(Math.round(duration / (double) (1 << binarySubdivision)));
        long binarySubbeat = ((timestamp - prevBeat) / binarySubbeatDuration) * binarySubbeatDuration;
        double numTernarySubdivisions = ternarySubdivision == 0 ? 1
                : ternarySubdivision == 1 ? 3 : Math.pow(3, ternarySubdivision);
        int ternarySubbeatDuration = Math.toIntExact(Math.round(duration / numTernarySubdivisions));
        long ternarySubbeat = ((timestamp - prevBeat) / ternarySubbeatDuration) * ternarySubbeatDuration;
        return prevBeat + Math.max(binarySubbeat, ternarySubbeat);
    }

    /**
     * Get the timestamp of the next subbeat after the given time.
     *
     * The returned timestamp is always posterior to the given one.
     *
     * @param timestamp
     *            Time at which the next subbeat time is computed
     * @param binarySubdivision
     *            Level of binary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 2 subdivisions, 2 for 4, etc.)
     * @return The next subbeat that will happen after the given timestamp
     */
    public long nextSubbeat(long timestamp, int binarySubdivision) {
        return nextSubbeat(timestamp, binarySubdivision, 0);
    }

    /**
     * Get the timestamp of the next subbeat after the given time.
     *
     * The returned timestamp is always posterior to the given one.
     *
     * @param timestamp
     *            Time at which the next subbeat time is computed
     * @param binarySubdivision
     *            Level of binary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 2 subdivisions, 2 for 4, etc.)
     * @param ternarySubdivision
     *            Level of ternary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 3 subdivisions, 2 for 9, etc.)
     * @return The next subbeat that will happen after the given timestamp
     */
    public long nextSubbeat(long timestamp, int binarySubdivision, int ternarySubdivision) {
        if (binarySubdivision < 0) {
            throw new IllegalArgumentException(
                    "The allowed subdivision level cannot be negative");
        }
        long nextBeat = nextBeat(timestamp);
        int binarySubbeatDuration = Math.toIntExact(Math.round(duration / (double) (1 << binarySubdivision)));
        long binarySubbeat = ((nextBeat - timestamp) / binarySubbeatDuration) * binarySubbeatDuration;
        double numTernarySubdivisions = ternarySubdivision == 0 ? 1
                : ternarySubdivision == 1 ? 3 : Math.pow(3, ternarySubdivision);
        int ternarySubbeatDuration = Math.toIntExact(Math.round(duration / numTernarySubdivisions));
        long ternarySubbeat = ((nextBeat - timestamp) / ternarySubbeatDuration) * ternarySubbeatDuration;
        return nextBeat - Math.max(binarySubbeat, ternarySubbeat);
    }

    /**
     * Get the timestamp of the subbeat that is closest to the given timestamp.
     *
     * The returned timestamp may be equal, preceding or posterior to the given
     * timestamp.
     *
     * @param timestamp
     *            Time at which the closest subbeat time is computed
     * @param binarySubdivision
     *            Level of binary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 2 subdivisions, 2 for 4, etc.)
     * @return The closest subbeat to the timestamp
     */
    public long closestSubbeat(long timestamp, int binarySubdivision) {
        return closestSubbeat(timestamp, binarySubdivision, 0);
    }

    /**
     * Get the timestamp of the subbeat that is closest to the given timestamp.
     *
     * The returned timestamp may be equal, preceding or posterior to the given
     * timestamp.
     *
     * @param timestamp
     *            Time at which the closest subbeat time is computed
     * @param binarySubdivision
     *            Level of binary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 2 subdivisions, 2 for 4, etc.)
     * @param ternarySubdivision
     *            Level of ternary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 3 subdivisions, 2 for 9, etc.)
     * @return The closest subbeat to the timestamp
     */
    public long closestSubbeat(long timestamp, int binarySubdivision, int ternarySubdivision) {
        if (binarySubdivision < 0 || ternarySubdivision < 0) {
            throw new IllegalArgumentException(
                    "The allowed subdivision level cannot be negative");
        }
        long prev = currentSubbeat(timestamp, binarySubdivision, ternarySubdivision);
        long next = nextSubbeat(timestamp, binarySubdivision, ternarySubdivision);
        if ((timestamp - prev) <= (next - timestamp)) {
            return prev;
        } else {
            return next;
        }
    }

    /**
     * Create a new onset resulting of snapping the given onset to the closest
     * beat.
     *
     * The resulting onset has a timestamp and duration that matches exactly
     * some beat.
     *
     * @param onset
     *            Onset to snap
     * @return The snapped onset
     */
    public Onset snap(Onset onset) {
        return snap(onset, 0, 0);
    }

    /**
     * Create a new onset resulting of snapping the given onset to the closest
     * beat subdivision.
     *
     * The resulting onset has a timestamp and duration that matches exactly
     * some beat subdivisions.
     *
     * @param onset
     *            Onset to snap
     * @param binarySubdivision
     *            Level of binary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 2 subdivisions, 2 for 4, etc.)
     * @return The snapped onset
     */
    public Onset snap(Onset onset, int binarySubdivision) {
        if (binarySubdivision < 0) {
            throw new IllegalArgumentException("The allowed subdivisions level cannot be negative");
        }
        long begin = closestSubbeat(onset.getTimestamp(), binarySubdivision);
        long end = closestSubbeat(onset.getTimestamp() + onset.getDuration(), binarySubdivision);
        int subbeatDuration = Math.round(getDuration() / (float) (binarySubdivision));
        int duration = Math.round((end - begin) / ((float) subbeatDuration)) * subbeatDuration;
        return new Onset(begin, duration, onset.getPitch(), onset.getVelocity());
    }

    /**
     * Create a new onset resulting of snapping the given onset to the closest
     * beat subdivision.
     *
     * The resulting onset has a timestamp and duration that matches exactly
     * some beat subdivisions.
     *
     * @param onset
     *            Onset to snap
     * @param binarySubdivision
     *            Level of binary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 2 subdivisions, 2 for 4, etc.)
     * @param ternarySubdivision
     *            Level of ternary beat subdivisons allowed (0 for no
     *            subdivision, 1 for 3 subdivisions, 2 for 9, etc.)
     * @return The snapped onset
     */
    public Onset snap(Onset onset, int binarySubdivision, int ternarySubdivision) {
        if (binarySubdivision < 0 || ternarySubdivision < 0) {
            throw new IllegalArgumentException("The allowed subdivisions level cannot be negative");
        }
        long begin = closestSubbeat(onset.getTimestamp(), binarySubdivision, ternarySubdivision);
        long end = closestSubbeat(onset.getTimestamp() + onset.getDuration(), binarySubdivision, ternarySubdivision);
        return new Onset(begin, (int) (end - begin), onset.getPitch(), onset.getVelocity());
    }

    @Override
    public String toString() {
        return "Beat [tempo=" + getTempo() + ", duration=" + duration
                + ", phase=" + phase + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + duration;
        result = prime * result + phase;
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
        Beat other = (Beat) obj;
        if (duration != other.duration) {
            return false;
        }
        if (phase != other.phase) {
            return false;
        }
        return true;
    }

    @Override
    public Beat clone() {
        return new Beat(duration, phase);
    }

}
