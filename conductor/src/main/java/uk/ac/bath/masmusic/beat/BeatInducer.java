package uk.ac.bath.masmusic.beat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
    private static final int MIN_TEMPO = 60;

    /** Maximum tempo (bpm) */
    private static final int MAX_TEMPO = 200;

    /** Minimum interval between to onsets (ms) */
    private static final int MIN_INTERONSET_INTERVAL = 70;

    /** Maximum interval between to onsets (ms) */
    private static final int MAX_INTERONSET_INTERVAL = 2500;

    /** Number of best clusters considered for induction */
    private static final int NUM_BEST_CLUSTERS = 10;

    /**
     * Induce the beat for the given collection of onsets.
     *
     * @param onsets Onsets for which the beat is induced
     * @return A list of induced possible beat durations
     */
    public List<Double> induceBeat(List<Onset> onsets) {
        // Create base clusters
        List<BeatCluster> beatClusters = new LinkedList<>();
        // Consider every pair of onsets
        for (Onset onsetRef : onsets) {
            for (Onset onset : onsets) {
                // Update clusters if time offset is within range
                long timeDiff = Math.abs(onset.getTimestamp() - onsetRef.getTimestamp());
                if ((timeDiff >= MIN_INTERONSET_INTERVAL)
                        && (timeDiff <= MAX_INTERONSET_INTERVAL)) {
                    updateClusters(beatClusters, timeDiff);
                }
            }
        }
        // Merge similar clusters
        mergeClusters(beatClusters);
        // Score clusters
        int[] clusterScores = scoreClusters(beatClusters);
        // Induce
        List<Double> inducedBeat = induceFromClusters(beatClusters, clusterScores);
        return inducedBeat;
    }

    /**
     * Extract beat duration hypotheses from a collection of scored clusters.
     *
     * @param beatClusters Clusters collection
     * @param clusterScores Cluster scores
     * @return Beat duration hypotheses
     */
    private List<Double> induceFromClusters(List<BeatCluster> beatClusters, int[] clusterScores) {
        // Sort indices by cluster size
        List<BeatCluster> beatClustersArr = new ArrayList<>(beatClusters);
        List<Integer> beatClustersBySizeIdx = new ArrayList<>(beatClusters.size());
        for (int i = 0; i < beatClusters.size(); i++) {
            beatClustersBySizeIdx.add(i);
        }
        Collections.sort(beatClustersBySizeIdx, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) {
                int size1 = beatClustersArr.get(idx1).size;
                int size2 = beatClustersArr.get(idx2).size;
                return size1 > size2 ? 1 : size1 < size2 ? -1 : 0;
            }
        });

        // Just in case, check is not empty
        if (beatClustersBySizeIdx.isEmpty()) {
            return new ArrayList<>();
        }

        // Compute induced beat durations
        List<Double> inducedBeat = new ArrayList<>(NUM_BEST_CLUSTERS);
        // Adjust biggest clusters using the size of super- and sub-intervals
        Iterator<Integer> itBest = beatClustersBySizeIdx.iterator();
        int numBest = 0;
        for (int iBest = itBest.next();
                itBest.hasNext() && numBest < NUM_BEST_CLUSTERS;
                iBest = itBest.next(), numBest++) {
            BeatCluster best = beatClustersArr.get(iBest);
            double sum = best.beatDuration * clusterScores[iBest];
            int weight = clusterScores[iBest];
            // int count = best.size;

            // Compare each best cluster to every other one
            ListIterator<BeatCluster> itOther = beatClustersArr.listIterator();
            for (BeatCluster other = itOther.next();
                    itOther.hasNext();
                    other = itOther.next()) {
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
                        double err = Math.abs(other.beatDuration - best.beatDuration * degree);
                        if (err <= CLUSTER_WIDTH) {
                            sumAdd = clusterScores[iOther] * other.beatDuration / degree;
                            weightAdd = clusterScores[iOther];
                            // countAdd = other.size;
                        }
                    } else {
                        double err = Math.abs(best.beatDuration - other.beatDuration * degree);
                        if (err <= CLUSTER_WIDTH * degree) {
                            sumAdd = clusterScores[iOther] * other.beatDuration * degree;
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
     * @param beatClusters Collection of clusters
     * @param timeDiff Inter-onset interval
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
            it.add(new BeatCluster());
        }
        // Update cluster
        it.previous().update(timeDiff);
    }

    /**
     * Merge clusters that are too similar.
     *
     * @param beatClusters Collection of clusters to merge, modified in place
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
     * @param beatClusters Clusters to evaluate
     * @return Cluster scores
     */
    private int[] scoreClusters(List<BeatCluster> beatClusters) {
        int[] scores = new int[beatClusters.size()];
        if (beatClusters.isEmpty()) {
            return scores;
        }

        Iterator<BeatCluster> it1 = beatClusters.iterator();
        int idx1 = 0;
        for (BeatCluster cluster1 = it1.next();
                idx1 < (beatClusters.size() - 1) && it1.hasNext();
                cluster1 = it1.next(), idx1++) {
            // Assign base score
            scores[idx1] += cluster1.size * 10;
            // Iterate the rest of the list
            int idx2 = idx1 + 1;
            Iterator<BeatCluster> it2 = beatClusters.listIterator(idx2);
            for (BeatCluster cluster2 = it2.next();
                    idx2 < beatClusters.size() && it2.hasNext();
                    cluster2 = it2.next(), idx2++) {
                double ratio = cluster1.beatDuration / cluster2.beatDuration;
                int degree = (int) Math.round(1. / ratio);
                if (degree >= 2 && degree <= 8) {
                    double err = Math.abs(cluster1.beatDuration * degree - cluster2.beatDuration);
                    if (err <= CLUSTER_WIDTH) {
                        degree = degree >= 5 ? 1 : 6 - degree;
                        scores[idx1] += degree * cluster2.size;
                        scores[idx2] += degree * cluster1.size;
                    }
                }
            }
        }
        return scores;
    }

    /**
     * A beat duration hypothesis with the number of supporting inter-onset intervals.
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
                throw new IllegalArgumentException("Beat duration must be positive");
            }
            if (size < 0) {
                throw new IllegalArgumentException("Cluster size must be positive");
            }
            this.beatDuration = beatDuration;
            this.size = size;
        }

        void merge(BeatCluster cluster) {
            int newSize = this.size + cluster.size;
            if (newSize > 0) {
                beatDuration = ((beatDuration * size) + (cluster.beatDuration * cluster.size))
                        / newSize;
            } else {
                beatDuration = (beatDuration + cluster.beatDuration) / 2;
            }
            this.size = newSize;
        }

        void update(double beatDuration) {
            this.beatDuration = ((this.beatDuration * this.size) + beatDuration) / (this.size + 1);
            this.size++;
        }
    }

}
