package uk.ac.bath.masmusic.beat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * BeatRoot beat detection.
 *
 * Implements the BeatRoot beat tracking algorithm.
 *
 * @see <a href="https://doi.org/10.1080/09298210701653310">Evaluation
 *      of the Audio Beat Tracking System BeatRoot (Dixon, 2007)</a>
 *
 * @author Javier Dehesa
 */
public class BeatRoot {

    /** Initial window of onsets from where trackers are initialized. */
    private static final int TRACKER_START_WINDOW = 5000;

    /** Threshold to consider two beat durations similar (ms). */
    private static final int BEAT_DURATION_SIMILARITY_THRESHOLD = 20;

    /** Threshold to consider two beat phases similar (ms). */
    private static final int BEAT_PHASE_SIMILARITY_THRESHOLD = 40;

    /** Beat inducer. */
    private BeatInducer beatInducer;

    /**
     * Constructor.
     */
    public BeatRoot() {
        beatInducer = new BeatInducer();
    }

    /**
     * Estimate the beat for a collection of onsets.
     *
     * @param onsets
     *            Onset events, sorted by time.
     * @return
     */
    public Beat estimateBeat(List<Onset> onsets) {
        List<Double> induced = beatInducer.induceBeat(onsets);
        return trackBeat(onsets, induced);
    }

    /**
     * @param onsets Onsets to track
     * @param induced Induced beat durations
     * @return The highest scoring tracked beat, or null if no tracking could be made
     */
    private Beat trackBeat(List<Onset> onsets, List<Double> induced) {
        if (onsets.size() < 2 || induced.isEmpty()) {
            return null;
        }
        // Estimate number of onsets in the start window
        double onsetsWindow = onsets.get(onsets.size() - 1).getTimestamp() - onsets.get(0).getTimestamp();
        int nOnsetsIni = (int) Math.round(onsets.size() / Math.ceil(onsetsWindow / TRACKER_START_WINDOW));
        // Create trackers starting at initial onsets
        PriorityQueue<BeatTracker> trackers = new PriorityQueue<>(2 * nOnsetsIni * induced.size());
        long baseTimestamp = onsets.get(0).getTimestamp();
        Iterator<Onset> it = onsets.iterator();
        for (Onset onset = it.next();
                it.hasNext() && (onset.getTimestamp() - baseTimestamp) <= TRACKER_START_WINDOW;
                onset = it.next()) {
            for (double beatDuration : induced) {
                trackers.add(new BeatTracker(beatDuration, onset.getTimestamp()));
            }
        }

        // Iterate onsets
        it = onsets.iterator();
        List<BeatTracker> reinsertTrackers = new ArrayList<>();
        for (Onset onset = it.next(); it.hasNext() && !trackers.isEmpty(); onset = it.next()) {
            BeatTracker tracker = nextTracker(trackers);
            // Move past trackers forward
            while (tracker != null && tracker.isAfterOuterMargin(onset.getTimestamp())) {
                // If the tracker is not expired move to next beat and add to the queue again
                if (!tracker.isExpired(onset.getTimestamp())) {
                    tracker.nextBeat();
                    trackers.offer(tracker);
                }
                tracker = nextTracker(trackers);
            }
            // Check hits
            reinsertTrackers.clear();
            if (tracker != null) {
                reinsertTrackers.add(tracker);
            }
            while (tracker != null && tracker.mayHit(onset.getTimestamp())) {
                if (tracker.isHit(onset.getTimestamp())) {
                    // Hit
                    tracker.hit(onset.getTimestamp(), onset.getSalience());
                } else {
                    // Fork
                    BeatTracker fork = tracker.clone();
                    fork.hit(onset.getTimestamp(), onset.getSalience());
                    reinsertTrackers.add(fork);
                }
                // Check next
                tracker = nextTracker(trackers);
                if (tracker != null) {
                    reinsertTrackers.add(tracker);
                }
            }
            // Reintroduce polled and forked trackers
            trackers.addAll(reinsertTrackers);
        }

        if (trackers.isEmpty()) {
            return null;
        }

        // Find the best tracker
        BeatTracker bestTracker = trackers.peek();
        for (BeatTracker tracker : trackers) {
            if (tracker.getScore() > bestTracker.getScore()) {
                bestTracker = tracker;
            }
        }

        return bestTracker.getBeat();
    }

    /**
     * Get the next tracker removing every duplicate that follows.
     *
     * @param trackerQueue Trackers queue
     * @return The next tracker in the queue without duplicates, or null if the queue is empty
     */
    private BeatTracker nextTracker(Queue<BeatTracker> trackerQueue) {
        if (trackerQueue == null || trackerQueue.isEmpty()) {
            return null;
        }
        BeatTracker tracker = trackerQueue.poll();
        while (similarTrackers(tracker, trackerQueue.peek())) {
            // Duplicate found - keep the one with higher score
            BeatTracker other = trackerQueue.poll();
            if (tracker.getScore() < other.getScore()) {
                tracker = other;
            }
        }
        return tracker;
    }

    /**
     * @param tracker1 First tracker to compare
     * @param tracker2 Second tracker to compare
     * @return true if the trackers are similar, false otherwise
     */
    private boolean similarTrackers(BeatTracker tracker1, BeatTracker tracker2) {
        if (tracker1 == tracker2) {
            return true;
        } else if (tracker1 == null ^ tracker2 == null) {
            return false;
        } else if (Math.abs(tracker1.getBeatDuration() - tracker2.getBeatDuration()) > BEAT_DURATION_SIMILARITY_THRESHOLD) {
            return false;
        } else if (Math.abs(tracker1.getTimestamp() - tracker2.getTimestamp()) > BEAT_PHASE_SIMILARITY_THRESHOLD) {
            return false;
        } else {
            return true;
        }
    }
}
