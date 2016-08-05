package uk.ac.bath.masmusic.generation.harmony;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import uk.ac.bath.masmusic.common.Chord;
import uk.ac.bath.masmusic.common.Note;

/**
 * A model representing the probability of a chord given the current one.
 *
 * @author Javier Dehesa
 */
public class ChordBigramModel {

    /** Model data. */
    private final Map<ScaleRelativeChord, Entry> model;

    /**
     * Constructor.
     */
    public ChordBigramModel() {
        this.model = new HashMap<>();
    }

    /**
     * Set a model value.
     *
     * @param prevChord
     *            The first chord of the bigram
     * @param nextChords
     *            The second possible chords of the bigram and their weights
     */
    public void setModelEntry(ScaleRelativeChord prevChord, Map<ScaleRelativeChord, Double> nextChords) {
        if (nextChords.isEmpty()) {
            throw new IllegalArgumentException("The model entry data must not be empty");
        }
        List<ScaleRelativeChord> entryChords = new ArrayList<>(nextChords.size());
        List<Double> entryProbabilities = new ArrayList<>(nextChords.size());
        double totalWeight = .0;
        for (Map.Entry<ScaleRelativeChord, Double> chord : nextChords.entrySet()) {
            entryChords.add(chord.getKey());
            entryProbabilities.add(chord.getValue());
            totalWeight += chord.getValue();
        }
        for (int i = 0; i < entryProbabilities.size(); i++) {
            entryProbabilities.set(i, entryProbabilities.get(i) / totalWeight);
        }
        model.put(prevChord, new Entry(entryChords, entryProbabilities));
    }

    /**
     * Estimate the possible chords for a given previous chord.
     *
     * This method may return an empty list if the given chord is not in the
     * model.
     *
     * @param fundamental
     *            The fundamental of the scale
     * @param previousChord
     *            The previous chord
     * @return A list of estimated chords sorted by probability
     */
    public List<Chord> estimateChords(Note fundamental, Chord previousChord) {
        return estimateChords(fundamental, previousChord, null);
    }

    /**
     * Estimate the possible chords for a given previous chord.
     *
     * This method may return an empty list if the given chord is not in the
     * model.
     *
     * @param fundamental
     *            The fundamental of the scale
     * @param previousChord
     *            The previous chord
     * @param probabilities
     *            If not null then the given list is cleared and filled with the
     *            probabilities of the returned chords
     * @return A list of estimated chords sorted by probability
     */
    public List<Chord> estimateChords(Note fundamental, Chord previousChord, List<Double> probabilities) {
        ScaleRelativeChord prevRelativeChord = ScaleRelativeChord.fromChord(fundamental, previousChord);
        Entry entry = model.get(prevRelativeChord);
        if (probabilities != null) {
            probabilities.clear();
        }
        if (entry != null) {
            if (probabilities != null) {
                probabilities.addAll(entry != null ? entry.probabilities : Collections.emptyList());
            }
            return entry.chords.stream().map(c -> c.getChord(fundamental)).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Pick a chord from an entry in the model.
     *
     * @param prevChord
     *            Previous chord
     * @param value
     *            A value in the range [0, 1]
     * @return The chord corresponding to the given entry and value, or null if
     *         the entry does not exist in the model.
     */
    ScaleRelativeChord pickChord(ScaleRelativeChord prevChord, double value) {
        Entry entry = model.get(prevChord);
        if (entry == null) {
            return null;
        }
        return entry.pickChord(value);
    }

    @Override
    public String toString() {
        return "ChordBigramModel " + model;
    }

    /**
     * An entry of the model.
     */
    private static class Entry {
        final List<ScaleRelativeChord> chords;
        final List<Double> probabilities;

        Entry(List<ScaleRelativeChord> chords, List<Double> probabilities) {
            if (chords.size() != probabilities.size()) {
                throw new IllegalArgumentException("Chords and probabilities must have the same size.");
            }
            if (chords.isEmpty()) {
                throw new IllegalArgumentException("Entry data cannot be empty");
            }
            // Reorder by probability (greater to smaller)
            List<Integer> indices = IntStream.range(0, chords.size()).boxed()
                    .sorted(Comparator.comparingDouble(i -> -probabilities.get(i))).collect(Collectors.toList());
            this.chords = indices.stream()
                    .map(chords::get)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
            this.probabilities = indices.stream()
                    .map(probabilities::get)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        }

        ScaleRelativeChord pickChord(double value) {
            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("The value must be between 0 and 1");
            }
            double cumProb = .0;
            for (int i = 0; i < chords.size(); i++) {
                cumProb += probabilities.get(i);
                if (cumProb > value) {
                    return chords.get(i);
                }
            }
            return chords.get(chords.size() - 1);
        }

        int size() {
            return chords.size();
        }

        @Override
        public String toString() {
            return "Entry [chords=" + chords + ", probabilities=" + probabilities + "]";
        }

    }

}
