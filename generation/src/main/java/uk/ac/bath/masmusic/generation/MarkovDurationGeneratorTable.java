package uk.ac.bath.masmusic.generation;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A Markov table for duration generation.
 *
 * @author Javier Dehesa
 */
public class MarkovDurationGeneratorTable {

    /** Table order. */
    private final int order;

    /** Status table. */
    private final Map<Status, Transitions> table;

    /**
     * Constructor.
     *
     * @param order
     *            Table order
     */
    MarkovDurationGeneratorTable(int order) {
        this.order = order;
        this.table = new HashMap<>();
    }

    /**
     * Set an entry in the table.
     *
     * @param relPitch
     *            Pitch class of the note relative to the scale tonic
     * @param ngram
     *            Sequence of last durations
     * @param transitions
     *            Map of transitions given as a map with the keys being the new
     *            duration and the values the number of occurrences
     */
    public void setEntry(int relPitch, List<Double> ngram, Map<Double, Integer> transitions) {
        if (relPitch < 0 || relPitch >= 12) {
            throw new InvalidParameterException(
                    "The relative pitch class must be between 0 and 11");
        }
        if (ngram.size() != order) {
            throw new InvalidParameterException(
                    "The size of the n-gram must match the order of the table");
        }
        if (transitions.isEmpty()) {
            throw new InvalidParameterException(
                    "The transitions cannot be empty");
        }
        // Create status
        Status status = new Status(relPitch, ngram);
        // Create transitions computing relative probabilities
        List<Double> durations = new ArrayList<>(transitions.size());
        List<Double> probabilities = new ArrayList<>(transitions.size());
        double totalHits = 0f;
        for (Map.Entry<Double, Integer> entry : transitions.entrySet()) {
            durations.add(entry.getKey());
            probabilities.add((double) entry.getValue().intValue());
            totalHits += entry.getValue();
        }
        for (int i = 0; i < probabilities.size(); i++) {
            probabilities.set(i, probabilities.get(i) / totalHits);
        }
        Transitions trans = new Transitions(durations, probabilities);
        // Save to table
        table.put(status, trans);
    }

    /**
     * Pick a duration from a status in the table.
     *
     * @param relPitch
     *            Pitch class of the note relative to the scale tonic
     * @param ngram
     *            Sequence of last durations
     * @param value
     *            A value in the range [0, 1]
     * @return The duration corresponding to the given status and value, or null
     *         if the status does not exist in the table.
     */
    public Double pickDuration(int relPitch, double[] ngram, double value) {
        return pickDuration(relPitch, Arrays.stream(ngram).boxed().collect(Collectors.toList()), value);
    }

    /**
     * Pick a duration from a status in the table.
     *
     * @param relPitch
     *            Pitch class of the note relative to the scale tonic
     * @param ngram
     *            Sequence of last durations
     * @param value
     *            A value in the range [0, 1]
     * @return The duration corresponding to the given status and value, or null
     *         if the status does not exist in the table.
     */
    public Double pickDuration(int relPitch, List<Double> ngram, double value) {
        if (ngram.size() < order) {
            return null;
        }
        if (ngram.size() > order) {
            ngram = ngram.subList(ngram.size() - order, ngram.size());
        }
        Status status = new Status(relPitch, ngram);
        Transitions trans = table.get(status);
        if (trans == null) {
            return null;
        }
        return trans.pickDuration(value);
    }

    /**
     * @return The table order
     */
    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "MarkovDurationGeneratorTable [order=" + order + ", table="
                + table
                + "]";
    }

    /** Markov chain status. */
    private static class Status {

        /** Relative pitch class. */
        final int relPitch;
        /** Sequence of last durations. */
        final List<Double> ngram;

        /**
         * Constructor.
         *
         * @param relPitch
         *            Relative pitch class
         * @param ngram
         *            Sequence of last durations
         */
        Status(int relPitch, List<Double> ngram) {
            this.relPitch = relPitch;
            this.ngram = Collections.unmodifiableList(new ArrayList<Double>(ngram));
        }

        @Override
        public String toString() {
            return "Status [relPitch=" + relPitch + ", ngram=" + ngram + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + relPitch;
            result = prime * result + ((ngram == null) ? 0 : ngram.hashCode());
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
            Status other = (Status) obj;
            if (relPitch != other.relPitch) {
                return false;
            }
            if (ngram == null) {
                if (other.ngram != null) {
                    return false;
                }
            } else if (!ngram.equals(other.ngram)) {
                return false;
            }
            return true;
        }
    }

    /** Markov chain transitions. */
    private static class Transitions {
        private final List<Double> durations;
        private final List<Double> probabilities;

        Transitions(List<Double> durations, List<Double> probabilities) {
            assert (durations.size() == probabilities.size());
            assert (durations.size() > 0);
            this.durations = Collections.unmodifiableList(new ArrayList<>(durations));
            this.probabilities = Collections.unmodifiableList(new ArrayList<>(probabilities));
        }

        /**
         * @param value
         *            A value in the range [0, 1]
         * @return The duration corresponding to the given value
         */
        public double pickDuration(double value) {
            assert (value >= 0.0 && value <= 1.0);
            double cumProb = 0.0;
            for (int i = 0; i < probabilities.size(); i++) {
                cumProb += probabilities.get(i);
                if (cumProb > value) {
                    return durations.get(i);
                }
            }
            return 0;
        }

        @Override
        public String toString() {
            String str = "Transitions [";
            str += IntStream.range(0, durations.size())
                    .mapToObj(i -> String.format("%f (%.0f%%)", durations.get(i), probabilities.get(i) * 100))
                    .collect(Collectors.joining(", "));
            str += "]";
            return str;
        }
    }

}
