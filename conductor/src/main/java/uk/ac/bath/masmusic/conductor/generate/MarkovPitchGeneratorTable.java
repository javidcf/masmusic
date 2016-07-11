package uk.ac.bath.masmusic.conductor.generate;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A Markov table for pitch generation.
 *
 * @author Javier Dehesa
 */
public class MarkovPitchGeneratorTable {

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
    MarkovPitchGeneratorTable(int order) {
        this.order = order;
        this.table = new HashMap<>();
    }

    /**
     * Set an entry in the table.
     *
     * @param degree
     *            Scale degree
     * @param alteration
     *            Degree alteration in half steps
     * @param ngram
     *            Sequence of last intervals
     * @param transitions
     *            Map of transitions given as a map with the keys being the
     *            pitch transition in half steps and the values the number of
     *            occurrences
     */
    public void setEntry(int degree, int alteration, List<Integer> ngram,
            Map<Integer, Integer> transitions) {
        if (degree < 1) {
            throw new InvalidParameterException("The degree must be positive");
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
        Status status = new Status(degree, alteration, ngram);
        // Create transitions computing relative probabilities
        List<Integer> steps = new ArrayList<>(transitions.size());
        List<Double> probabilities = new ArrayList<>(transitions.size());
        float totalHits = 0f;
        for (Map.Entry<Integer, Integer> entry : transitions.entrySet()) {
            steps.add(entry.getKey());
            probabilities.add((double) entry.getValue().intValue());
            totalHits += entry.getValue();
        }
        for (int i = 0; i < probabilities.size(); i++) {
            probabilities.set(i, probabilities.get(i) / totalHits);
        }
        Transitions trans = new Transitions(steps, probabilities);
        // Save to table
        table.put(status, trans);
    }

    /**
     * Pick a step from a status in the table.
     *
     * @param degree
     *            Scale degree
     * @param alteration
     *            Degree alteration in half steps
     * @param ngram
     *            Sequence of last intervals
     * @param value
     *            A value in the range [0, 1]
     * @return The step corresponding to the given status and value, or null if
     *         the status does not exist in the table.
     */
    public Integer pickStep(int degree, int alteration, List<Integer> ngram,
            double value) {
        if (ngram.size() != order) {
            throw new InvalidParameterException(
                    "The size of the n-gram must match the order of the table");
        }
        Status status = new Status(degree, alteration, ngram);
        Transitions trans = table.get(status);
        if (trans == null) {
            return null;
        }
        return trans.pickStep(value);
    }

    /**
     * @return The table order
     */
    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return "MarkovPitchGeneratorTable [order=" + order + ", table=" + table
                + "]";
    }

    /** Markov chain status. */
    private static class Status {

        /** Scale degree. */
        final int degree;
        /** Degree alteration in half steps. */
        final int alteration;
        /** Sequence of last intervals. */
        final List<Integer> ngram;

        /**
         * Constructor.
         *
         * @param degree
         *            Scale degree
         * @param alteration
         *            Degree alteration in half steps
         * @param ngram
         *            Sequence of last intervals
         */
        Status(int degree, int alteration, List<Integer> ngram) {
            this.degree = degree;
            this.alteration = alteration;
            this.ngram = Collections
                    .unmodifiableList(new ArrayList<Integer>(ngram));
        }

        @Override
        public String toString() {
            return "Status [degree=" + degree + ", alteration=" + alteration
                    + ", ngram=" + ngram + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + alteration;
            result = prime * result + degree;
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
            if (alteration != other.alteration) {
                return false;
            }
            if (degree != other.degree) {
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
    static class Transitions {
        private final List<Integer> steps;
        private final List<Double> probabilities;

        Transitions(List<Integer> steps, List<Double> probabilities) {
            assert (steps.size() == probabilities.size());
            assert (steps.size() > 0);
            this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
            this.probabilities = Collections
                    .unmodifiableList(new ArrayList<>(probabilities));
        }

        /**
         * @param value
         *            A value in the range [0, 1]
         * @return The step corresponding to the given value
         */
        public int pickStep(double value) {
            assert (value >= 0.0 && value <= 1.0);
            double cumProb = 0.0;
            for (int i = 0; i < probabilities.size(); i++) {
                cumProb += probabilities.get(i);
                if (cumProb > value) {
                    return steps.get(i);
                }
            }
            return 0;
        }

        @Override
        public String toString() {
            String str = "Transitions [";
            str += IntStream.range(0, steps.size())
                    .mapToObj(i -> String.format("%d (%.0f%%)",
                            steps.get(i), probabilities.get(i) * 100))
                    .collect(Collectors.joining(", "));
            str += "]";
            return str;
        }
    }

}
