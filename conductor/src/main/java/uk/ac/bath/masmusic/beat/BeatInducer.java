package uk.ac.bath.masmusic.beat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Implements the beat induction stage of BeatRoot.
 *
 * @author Javier Dehesa
 */
class BeatInducer {

    /** Beat cluster width (ms) */
    private static final int CLUSTER_WIDTH = 25;

    /** Minimum tempo (bpm) */
    private static final int MIN_TEMPO = 80;

    /** Maximum tempo (bpm) */
    private static final int MAX_TEMPO = 160;

    /** Minimum interval between to onsets (ms) */
    private static final int MIN_INTERONSET_INTERVAL = 70;

    /** Maximum interval between to onsets (ms) */
    private static final int MAX_INTERONSET_INTERVAL = 2500;

    /** Number of best clusters considered for induction */
    private static final int NUM_BEST_CLUSTERS = 10;

    /**
     * Induce the beat for the given collection of onsets.
     *
     * @param onsets
     *            Onsets for which the beat is induced
     * @return A list of induced possible beat durations
     */
    public List<Double> induceBeat(List<Onset> onsets) {
        if (onsets.size() < 2) {
            return new ArrayList<Double>(0);
        }
        // Create base clusters
        List<BeatCluster> beatClusters = new LinkedList<>();
        // Consider every pair of onsets
        ListIterator<Onset> itRef = onsets.listIterator();
        while (itRef.hasNext()) {
            Onset onsetRef = itRef.next();
            ListIterator<Onset> itOther = onsets
                    .listIterator(itRef.nextIndex());
            while (itOther.hasNext()) {
                Onset onset = itOther.next();
                // Update clusters if time offset is within range
                long timeDiff = Math
                        .abs(onset.getTimestamp() - onsetRef.getTimestamp());
                if (timeDiff < MIN_INTERONSET_INTERVAL) {
                    // Too short
                    continue;
                }
                if (timeDiff > MAX_INTERONSET_INTERVAL) {
                    // Too long - go to next onsetRef
                    break;
                }
                updateClusters(beatClusters, timeDiff);
            }
        }
        // Merge similar clusters
        mergeClusters(beatClusters);
        // Score clusters
        int[] clusterScores = scoreClusters(beatClusters);
        // Induce
        List<Double> inducedBeat = induceFromClusters(beatClusters,
                clusterScores);
        return inducedBeat;
    }

    /**
     * Extract beat duration hypotheses from a collection of scored clusters.
     *
     * @param beatClusters
     *            Clusters collection
     * @param clusterScores
     *            Cluster scores
     * @return Beat duration hypotheses
     */
    private List<Double> induceFromClusters(List<BeatCluster> beatClusters,
            int[] clusterScores) {
        if (beatClusters.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort indices by cluster size
        List<BeatCluster> beatClustersArr = new ArrayList<>(beatClusters);
        List<Integer> beatClustersBySizeIdx = new ArrayList<>(
                beatClusters.size());
        for (int i = 0; i < beatClusters.size(); i++) {
            beatClustersBySizeIdx.add(i);
        }
        Collections.sort(beatClustersBySizeIdx, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) {
                // Bigger sizes first in list
                int size1 = beatClustersArr.get(idx1).size;
                int size2 = beatClustersArr.get(idx2).size;
                return size2 - size1;
            }
        });

        // Compute induced beat durations
        List<Double> inducedBeat = new ArrayList<>(NUM_BEST_CLUSTERS);
        // Adjust biggest clusters using the size of super- and sub-intervals
        ListIterator<Integer> itBest = beatClustersBySizeIdx.listIterator();
        while (itBest.hasNext() && itBest.nextIndex() < NUM_BEST_CLUSTERS) {
            int iBest = itBest.next();
            BeatCluster best = beatClustersArr.get(iBest);
            double sum = best.beatDuration * clusterScores[iBest];
            int weight = clusterScores[iBest];
            // int count = best.size;

            // Compare each best cluster to every other one
            ListIterator<BeatCluster> itOther = beatClustersArr.listIterator();
            while (itOther.hasNext()) {
                BeatCluster other = itOther.next();
                int iOther = itOther.previousIndex();
                // Ignore self
                if (iOther == iBest) {
                    continue;
                }
                // Compute the ratio between clusters
                double ratio = best.beatDuration / other.beatDuration;
                boolean subMult = ratio < 1;
                int degree = (int) Math.round(subMult ? 1 / ratio : ratio);
                // Check sub-/super-cluster relationships
                if ((degree >= 2) && (degree <= 8)) {
                    double sumAdd = .0;
                    // int countAdd = 0;
                    int weightAdd = 0;
                    if (subMult) {
                        double err = Math.abs(other.beatDuration
                                - best.beatDuration * degree);
                        if (err <= CLUSTER_WIDTH) {
                            sumAdd = clusterScores[iOther] * other.beatDuration
                                    / degree;
                            weightAdd = clusterScores[iOther];
                            // countAdd = other.size;
                        }
                    } else {
                        double err = Math.abs(best.beatDuration
                                - other.beatDuration * degree);
                        if (err <= CLUSTER_WIDTH * degree) {
                            sumAdd = clusterScores[iOther] * other.beatDuration
                                    * degree;
                            weightAdd = clusterScores[iOther];
                            // countAdd = other.size;
                        }
                    }
                    sum += sumAdd;
                    weight += weightAdd;
                    // count += countAdd;
                }
            }
            // Induced beat
            double beat = sum / weight;
            // Put within range (assumes grouping is not ternary)
            while ((60000 / beat) < MIN_TEMPO) {
                beat /= 2;
            }
            while ((60000 / beat) > MAX_TEMPO) {
                beat *= 2;
            }
            if ((60000 / beat) >= MIN_TEMPO) {
                inducedBeat.add(beat);
            }
        }

        return inducedBeat;
    }

    /**
     * Update a cluster collection by introducing a new inter-onset interval
     *
     * @param beatClusters
     *            Collection of clusters
     * @param timeDiff
     *            Inter-onset interval
     */
    private void updateClusters(List<BeatCluster> beatClusters, long timeDiff) {
        // Find iterator to cluster
        ListIterator<BeatCluster> it = beatClusters.listIterator();
        BeatCluster currentCluster = it.hasNext() ? it.next() : null;
        while (currentCluster != null
                && timeDiff > (currentCluster.beatDuration + CLUSTER_WIDTH)) {
            currentCluster = it.hasNext() ? it.next() : null;
        }
        // Deviation from selected cluster
        double clusterDiff;
        if (currentCluster != null) {
            clusterDiff = Math.abs(timeDiff - currentCluster.beatDuration);
        } else {
            clusterDiff = Double.POSITIVE_INFINITY;
        }
        // Check if next cluster is better
        if (it.hasNext()) {
            double prevClusterDiff = clusterDiff;
            currentCluster = it.next();
            clusterDiff = Math.abs(timeDiff - currentCluster.beatDuration);
            if (prevClusterDiff < clusterDiff) {
                currentCluster = it.previous();
                clusterDiff = prevClusterDiff;
            }
        }
        // Create new cluster if necessary
        if (currentCluster == null || clusterDiff > CLUSTER_WIDTH) {
            // If is not bigger than every cluster add before current
            if (currentCluster != null && it.hasPrevious()) {
                it.previous();
            }
            currentCluster = new BeatCluster();
            it.add(currentCluster);
        }
        // Update cluster
        currentCluster.update(timeDiff);
    }

    /**
     * Merge clusters that are too similar.
     *
     * @param beatClusters
     *            Collection of clusters to merge, modified in place
     */
    private void mergeClusters(List<BeatCluster> beatClusters) {
        if (beatClusters.size() > 1) {
            boolean change;
            do {
                change = false;
                int idxSmallestDiff = -1;
                double smallestDiff = Double.POSITIVE_INFINITY;
                BeatCluster smallestDiffPrev = null;
                BeatCluster smallestDiffNext = null;

                ListIterator<BeatCluster> it = beatClusters.listIterator();
                BeatCluster prev = it.next();
                while (it.hasNext()) {
                    BeatCluster next = it.next();
                    double diff = next.beatDuration - prev.beatDuration;
                    if (diff < smallestDiff) {
                        smallestDiff = diff;
                        idxSmallestDiff = it.previousIndex();
                        smallestDiffPrev = prev;
                        smallestDiffNext = next;
                    }
                    prev = next;
                }
                if (smallestDiff < CLUSTER_WIDTH) {
                    smallestDiffPrev.merge(smallestDiffNext);
                    beatClusters.remove(idxSmallestDiff);
                    change = true;
                }
            } while (change);
        }
    }

    /**
     * @param beatClusters
     *            Clusters to evaluate
     * @return Cluster scores
     */
    private int[] scoreClusters(List<BeatCluster> beatClusters) {
        int[] scores = new int[beatClusters.size()];
        if (beatClusters.isEmpty()) {
            return scores;
        }

        ListIterator<BeatCluster> it1 = beatClusters.listIterator();
        while (it1.hasNext() && it1.nextIndex() < (beatClusters.size() - 1)) {
            BeatCluster cluster1 = it1.next();
            // Assign base score
            scores[it1.previousIndex()] += cluster1.size * 10;
            // Iterate the rest of the list
            ListIterator<BeatCluster> it2 = beatClusters
                    .listIterator(it1.previousIndex() + 1);
            while (it2.hasNext()) {
                BeatCluster cluster2 = it2.next();
                double ratio = cluster1.beatDuration / cluster2.beatDuration;
                int degree = (int) Math.round(1. / ratio);
                if (degree >= 2 && degree <= 8) {
                    double err = Math.abs(cluster1.beatDuration * degree
                            - cluster2.beatDuration);
                    if (err <= CLUSTER_WIDTH) {
                        degree = degree >= 5 ? 1 : 6 - degree;
                        scores[it1.previousIndex()] += degree * cluster2.size;
                        scores[it2.previousIndex()] += degree * cluster1.size;
                    }
                }
            }
        }
        return scores;
    }

    /**
     * A beat duration hypothesis with the number of supporting inter-onset
     * intervals.
     */
    private static class BeatCluster {
        double beatDuration;
        int size;

        BeatCluster() {
            this(.0);
        }

        BeatCluster(double beatDuration) {
            this(beatDuration, 0);
        }

        BeatCluster(double beatDuration, int size) {
            if (beatDuration < .0) {
                throw new IllegalArgumentException(
                        "Beat duration must be positive");
            }
            if (size < 0) {
                throw new IllegalArgumentException(
                        "Cluster size must be positive");
            }
            this.beatDuration = beatDuration;
            this.size = size;
        }

        void merge(BeatCluster cluster) {
            int newSize = this.size + cluster.size;
            if (newSize > 0) {
                beatDuration = ((beatDuration * size)
                        + (cluster.beatDuration * cluster.size))
                        / newSize;
            } else {
                beatDuration = (beatDuration + cluster.beatDuration) / 2;
            }
            this.size = newSize;
        }

        void update(double beatDuration) {
            this.beatDuration = ((this.beatDuration * this.size) + beatDuration)
                    / (this.size + 1);
            this.size++;
        }

        @Override
        public String toString() {
            return "BeatCluster [beatDuration=" + beatDuration + ", size="
                    + size + "]";
        }
    }

}
