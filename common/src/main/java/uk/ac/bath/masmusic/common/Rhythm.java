package uk.ac.bath.masmusic.common;

/**
 * A specification of a rhythm.
 *
 * A rhythm is specified by its beat and time signature.
 *
 * @author Javier Dehesa
 */
public class Rhythm {

    /** The rhythm beat. */
    private final Beat beat;

    /** The rhythm time signature. */
    private final TimeSignature timeSignature;

    /** The number of beats between the first beat and the first bar. */
    private final int beatOffset;

    /**
     * Constructor.
     *
     * @param beat
     *            The rhythm beat
     * @param timeSignature
     *            The rhythm time signature
     */
    public Rhythm(Beat beat, TimeSignature timeSignature) {
        this(beat, timeSignature, 0);
    }

    /**
     * Constructor.
     *
     * @param beat
     *            The rhythm beat
     * @param timeSignature
     *            The rhythm time signature
     * @param beatOffset
     *            The number of beats between the first beat and the first bar
     */
    public Rhythm(Beat beat, TimeSignature timeSignature, int beatOffset) {
        this.beat = beat;
        this.timeSignature = timeSignature;
        this.beatOffset = Math.floorMod(beatOffset, timeSignature.getBeats());
    }

    /**
     * @return The rhythm beat
     */
    public Beat getBeat() {
        return beat;
    }

    /**
     * @return The rhythm time signature
     */
    public TimeSignature getTimeSignature() {
        return timeSignature;
    }

    /**
     * @return The number of beats between the first beat and the first bar
     */
    public int getBeatOffset() {
        return beatOffset;
    }

    /**
     * @return The duration of one bar
     */
    public int getBarDuration() {
        return beat.getDuration() * timeSignature.getBeats();
    }

    /**
     * Get the timestamp of the last bar that happened at the given time.
     *
     * The returned timestamp is always preceding or equal to the given one.
     *
     * @param timestamp
     *            Time at which the bar time is computed
     * @return The timestamp of the last bar that happened at the given time
     */
    public long currentBar(long timestamp) {
        long beatNumber = beat.beatNumber(timestamp);
        long barBeat = (beatNumber % timeSignature.getBeats()) - beatOffset;
        if (barBeat < 0) {
            barBeat += timeSignature.getBeats();
        }
        return beat.byNumber(beatNumber - barBeat);
    }

    /**
     * Get the timestamp of the next bar after at the given time.
     *
     * The returned timestamp is always posterior to the given one.
     *
     * @param timestamp
     *            Time at which the next bar time is computed
     * @return The timestamp of the next bar that will happen after the given
     *         time
     */
    public long nextBar(long timestamp) {
        return nextBar(timestamp, 0);
    }

    /**
     * Get the timestamp of the next bars after the given time.
     *
     * The returned timestamp is always posterior to the given one.
     *
     * @param timestamp
     *            Time at which the next bar time is computed
     * @param bars
     *            Number of bars. With 0, the first bar after the given
     *            timestamp is returned.
     * @return The timestamp of the next bar that will happen after the given
     *         time and number of bars
     */
    public long nextBar(long timestamp, int bars) {
        return currentBar(timestamp)
                + (bars + 1) * beat.getDuration() * timeSignature.getBeats();
    }

    /**
     * Get the timestamp of the bar that is closest to the given timestamp.
     *
     * The returned timestamp may be equal, preceding or posterior to the given
     * timestamp.
     *
     * @param timestamp
     *            Time at which the closest bar time is computed
     * @return The timestamp of the closest bar that to the time
     */
    public long closestBar(long timestamp) {
        long prev = currentBar(timestamp);
        long next = nextBar(timestamp);
        if ((timestamp - prev) <= (next - timestamp)) {
            return prev;
        } else {
            return next;
        }
    }

    /**
     * Get the position of a beat in the bar.
     *
     * @param timestamp
     *            A timestamp within the beat
     * @return The position of the beat in the bar, starting on 0
     */
    public int beatPosition(long timestamp) {
        return (int) Math.floorMod(beat.beatNumber(timestamp) - beatOffset, timeSignature.getBeats());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((beat == null) ? 0 : beat.hashCode());
        result = prime * result + beatOffset;
        result = prime * result
                + ((timeSignature == null) ? 0 : timeSignature.hashCode());
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
        Rhythm other = (Rhythm) obj;
        if (beat == null) {
            if (other.beat != null) {
                return false;
            }
        } else if (!beat.equals(other.beat)) {
            return false;
        }
        if (beatOffset != other.beatOffset) {
            return false;
        }
        if (timeSignature == null) {
            if (other.timeSignature != null) {
                return false;
            }
        } else if (!timeSignature.equals(other.timeSignature)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Rhythm [beat=" + beat + ", timeSignature=" + timeSignature
                + ", beatOffset=" + beatOffset + "]";
    }

    /**
     * @return The timestamp of the first bar after timestamp 0
     */
    public int getFirstBarOffset() {
        return beatOffset * beat.getDuration() * beat.getPhase();
    }

}
