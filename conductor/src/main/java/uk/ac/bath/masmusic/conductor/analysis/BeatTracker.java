package uk.ac.bath.masmusic.conductor.analysis;

import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.bath.masmusic.common.Beat;

/**
 * A beat-tracking agent.
 *
 * Tracks a beat along a set of onsets.
 *
 * @author Javier Dehesa
 */
class BeatTracker implements Comparable<BeatTracker>, Cloneable {

    /** Time margin to consider a beat to be on time (ms). */
    private static final double INNER_MARGIN = 40.;

    /**
     * Proportion of beat duration in which a beat before time may be correct.
     */
    private static final double OUTER_MARGIN_PRE_FACTOR = .15;

    /**
     * Proportion of beat duration in which a beat after time may be correct.
     */
    private static final double OUTER_MARGIN_POST_FACTOR = .3;

    /** Penalty factor applied to onsets out of time. */
    private static final double MISS_PENALTY_FACTOR = .5;

    /**
     * Maximum proportion of the initial beat duration that it may change.
     */
    private static final double MAX_CHANGE_FACTOR = .2;

    /**
     * Factor by which the tempo of a tracker is displaced according to a new
     * beat position.
     */
    private static final double CORRECTION_FACTOR = .02;

    /** Time after which a tracker without beats hit is considered expired. */
    private static final double EXPIRY_TIME = 10000.;

    /** Agent id generator. */
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    /** Agent unique id. */
    private final int id;

    /** Tracker initial beat duration. */
    private final double initialBeatDuration;

    /** Tracker beat duration. */
    private double beatDuration;

    /** Tracker timestamp. */
    private double timestamp;

    /** Tracker last hit timestamp. */
    private double lastHitTimestamp;

    /** Tracker current score. */
    private double score;

    /**
     * Constructor.
     *
     * Create a beat tracker for the given beat duration and timestamp.
     *
     * @param beatDuration
     *            The duration of the beat to track
     * @param timestamp
     *            The tracker timestamp
     */
    public BeatTracker(double beatDuration, double timestamp) {
        if (beatDuration <= 0) {
            throw new IllegalArgumentException(
                    "The beat duration must be positive");
        }
        this.id = ID_GENERATOR.getAndIncrement();
        this.initialBeatDuration = beatDuration;
        this.beatDuration = beatDuration;
        this.timestamp = timestamp;
        this.lastHitTimestamp = timestamp;
        this.score = 0;
    }

    /**
     * @return The beat tracked by this agent
     */
    public Beat getBeat() {
        return new Beat((int) Math.round(beatDuration), Math.round(timestamp));
    }

    /**
     * @return The tracker beat duration
     */
    public double getBeatDuration() {
        return beatDuration;
    }

    /**
     * @return The tracker timestamp
     */
    public double getTimestamp() {
        return timestamp;
    }

    /**
     * @return The tracker score
     */
    public double getScore() {
        return score;
    }

    /**
     * Move the tracker to the next beat position.
     */
    public void nextBeat() {
        this.timestamp += this.beatDuration;
    }

    /**
     * Check if the given timestamp should be considered as a hit.
     *
     * A timestamp is considered a hit if it is within the inner margin of the
     * tracker timestamp. This does not change the status of the tracker.
     *
     * @param timestamp
     *            Timestamp to check
     * @return true if the timestamp should be considered as a hit, false
     *         otherwise
     */
    public boolean isHit(double timestamp) {
        double minTimestamp = this.timestamp - INNER_MARGIN;
        double maxTimestamp = this.timestamp + INNER_MARGIN;
        return (minTimestamp <= timestamp) && (maxTimestamp >= timestamp);
    }

    /**
     * Check if the given timestamp may be considered as a hit.
     *
     * A timestamp may be considered a hit if it is within the outer margin of
     * the tracker timestamp. This does not change the status of the tracker.
     *
     * @param timestamp
     *            Timestamp to check
     * @return true if the timestamp may be considered as a hit, false otherwise
     */
    public boolean mayHit(double timestamp) {
        double minTimestamp = this.timestamp
                - this.beatDuration * OUTER_MARGIN_PRE_FACTOR;
        double maxTimestamp = this.timestamp
                + this.beatDuration * OUTER_MARGIN_POST_FACTOR;
        return (minTimestamp <= timestamp) && (maxTimestamp >= timestamp);
    }

    /**
     * Check if the tracker outer margin is behind the given timestamp.
     *
     * @param timestamp
     *            Timestamp to check
     * @return true if the tracker outer margin is behind the given timestamp,
     *         false otherwise
     */
    public boolean isMarginBehind(double timestamp) {
        double maxTimestamp = this.timestamp
                + this.beatDuration * OUTER_MARGIN_POST_FACTOR;
        return timestamp > maxTimestamp;
    }

    /**
     * Use the given timestamp as a beat hit.
     *
     * @param hitTimestamp
     *            The timestamp to use as hit
     * @param salience
     *            The salience of the event
     */
    public void hit(double hitTimestamp, double salience) {
        // Update score
        double err = hitTimestamp - timestamp;
        double penalty;
        if (err < 0) {
            penalty = 1 + MISS_PENALTY_FACTOR * err
                    / (beatDuration * OUTER_MARGIN_PRE_FACTOR);
        } else {
            penalty = 1 - MISS_PENALTY_FACTOR * err
                    / (beatDuration * OUTER_MARGIN_POST_FACTOR);
        }
        penalty = Math.max(Math.min(penalty, 1), 0);
        score += salience * penalty;
        // Update beat duration
        double beatCorrected = beatDuration + err * CORRECTION_FACTOR;
        double beatCorrectionChange = beatCorrected - initialBeatDuration;
        double maxBeatChange = MAX_CHANGE_FACTOR * initialBeatDuration;
        beatDuration = initialBeatDuration + beatCorrectionChange
                * Math.min(Math.abs(maxBeatChange / beatCorrectionChange), 1);
        // Update timestamps
        timestamp = hitTimestamp;
        lastHitTimestamp = hitTimestamp;
    }

    /**
     * Whether the tracker is expired at the given timestamp.
     *
     * @param timestamp
     *            The timestamp to check
     * @return true if the tracker is expired at the given timestamp, false
     *         otherwise
     */
    public boolean isExpired(double timestamp) {
        return timestamp - this.lastHitTimestamp > EXPIRY_TIME;
    }

    @Override
    public int compareTo(BeatTracker other) {
        // Compare by start of outer margin
        int comp = Double.compare(
                this.timestamp - this.beatDuration * OUTER_MARGIN_PRE_FACTOR,
                other.timestamp - other.beatDuration * OUTER_MARGIN_PRE_FACTOR);
        if (comp == 0) {
            // Avoid having 0-valued comparisons with stable order
            return Integer.compare(this.id, other.id);
        } else {
            return comp;
        }
    }

    @Override
    public BeatTracker clone() {
        return new BeatTracker(beatDuration, timestamp);
    }

}
