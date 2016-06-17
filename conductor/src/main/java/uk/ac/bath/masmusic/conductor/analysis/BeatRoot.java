package uk.ac.bath.masmusic.conductor.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Onset;

/**
 * BeatRoot beat detection.
 *
 * Implements the BeatRoot beat tracking algorithm.
 *
 * @see <a href="https://doi.org/10.1080/09298210701653310">Evaluation of the
 *      Audio Beat Tracking System BeatRoot (Dixon, 2007)</a>
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
     *
     * @param minTempo
     *            Minimum tempo that may be estimated
     * @param maxTempo
     *            Maximum tempo that may be estimated
     * @throws IllegalArgumentException
     *             If the tempo range is not valid
     */
    public BeatRoot(int minTempo, int maxTempo) {
        if (minTempo <= 0 || minTempo > maxTempo) {
            throw new IllegalArgumentException("Invalid tempo range");
        }
        beatInducer = new BeatInducer(minTempo, maxTempo);
    }

    /**
     * Estimate the beat for a collection of onsets.
     *
     * @param onsets
     *            Onset events, sorted by time
     * @return The estimated beat, or null if no estimation could be done
     */
    public Beat estimateBeat(List<Onset> onsets) {
        List<Double> induced = beatInducer.induceBeat(onsets);
        // System.out.print("Induced beat: [ ");
        // induced.forEach(d -> System.out.printf("%.0f ", 60000 / d));
        // System.out.println("]");
        return trackBeat(onsets, induced);
    }

    /**
     * @param onsets
     *            Onsets to track
     * @param induced
     *            Induced beat durations
     * @return The highest scoring tracked beat, or null if no tracking could be
     *         made
     */
    private Beat trackBeat(List<Onset> onsets, List<Double> induced) {
        if (onsets.isEmpty() || induced.isEmpty()) {
            return null;
        }
        // Estimate number of trackers
        double onsetsWindow = onsets.get(onsets.size() - 1).getTimestamp()
                - onsets.get(0).getTimestamp();
        int nOnsetsIni = (int) Math.round(
                onsets.size() / Math.ceil(onsetsWindow / TRACKER_START_WINDOW));

        // Create trackers for every onset in the start window
        PriorityQueue<BeatTracker> trackers = new PriorityQueue<>(
                2 * nOnsetsIni * induced.size());
        long baseTimestamp = onsets.get(0).getTimestamp();
        Iterator<Onset> it = onsets.iterator();
        while (it.hasNext()) {
            Onset onset = it.next();
            long timestamp = onset.getTimestamp();
            if (timestamp - baseTimestamp <= TRACKER_START_WINDOW) {
                for (double beatDuration : induced) {
                    trackers.add(new BeatTracker(beatDuration, timestamp));
                }
            } else {
                break;
            }
        }

        // Iterate onsets
        List<BeatTracker> reinsertTrackers = new ArrayList<>();
        it = onsets.iterator();
        while (it.hasNext()) {
            Onset onset = it.next();
            long timestamp = onset.getTimestamp();

            // DEBUG
            BeatTracker btr = trackers.peek();
            for (BeatTracker tr : trackers) {
                if (tr.getScore() > btr.getScore()) {
                    btr = tr;
                }
            }
            //System.out.println(btr);
            // DEBUG

            // Move ahead trackers behind the onset
            BeatTracker tracker = pollTracker(trackers);
            while (tracker != null
                    && tracker.isMarginBehind(timestamp)) {
                // If the tracker is not expired move it to the next beat
                // and add it to the queue again
                if (!tracker.isExpired(timestamp)) {
                    tracker.nextBeat();
                    trackers.offer(tracker);
                }
                tracker = pollTracker(trackers);
            }

            // Check hits
            reinsertTrackers.clear();
            if (tracker != null) {
                reinsertTrackers.add(tracker);
            }
            while (tracker != null && tracker.mayHit(timestamp)) {
                if (tracker.isHit(onset.getTimestamp())) {
                    // Hit
                    tracker.hit(onset.getTimestamp(), onset.getSalience());
                } else {
                    // Fork
                    BeatTracker fork = tracker.clone();
                    fork.hit(timestamp, onset.getSalience());
                    reinsertTrackers.add(fork);
                }
                // Check next
                tracker = pollTracker(trackers);
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
     * @param trackerQueue
     *            Trackers queue
     * @return The next tracker in the queue without duplicates, or null if the
     *         queue is empty
     */
    private BeatTracker pollTracker(Queue<BeatTracker> trackerQueue) {
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
     * @param tracker1
     *            First tracker to compare
     * @param tracker2
     *            Second tracker to compare
     * @return true if the trackers are similar, false otherwise
     */
    private boolean similarTrackers(BeatTracker tracker1,
            BeatTracker tracker2) {
        if (tracker1 == tracker2) {
            return true;
        } else if (tracker1 == null ^ tracker2 == null) {
            return false;
        } else if (Math.abs(tracker1.getBeatDuration() - tracker2
                .getBeatDuration()) > BEAT_DURATION_SIMILARITY_THRESHOLD) {
            return false;
        } else if (Math.abs(tracker1.getTimestamp()
                - tracker2.getTimestamp()) > BEAT_PHASE_SIMILARITY_THRESHOLD) {
            return false;
        } else {
            return true;
        }
    }
}