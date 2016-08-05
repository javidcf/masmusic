package uk.ac.bath.masmusic.generation.harmony;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import uk.ac.bath.masmusic.common.Chord;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Onset;

/**
 * A model representing the probability of a chord given some pitch class.
 *
 * @author Javier Dehesa
 */
public class PitchClassChordModel {

    /** Model data. */
    private final Map<Integer, Entry> model;

    /**
     * Constructor.
     */
    public PitchClassChordModel() {
        this.model = new HashMap<>();
    }

    /**
     * Set a value of the model.
     *
     * @param pitchClass
     *            Pitch class relative to the scale fundamental
     * @param chords
     *            The possible chords for the pitch class and their weights
     */
    public void setModelEntry(int pitchClass, Map<ScaleRelativeChord, Double> chords) {
        if (chords.isEmpty()) {
            throw new IllegalArgumentException("The model entry data must not be empty");
        }
        pitchClass = Math.floorMod(pitchClass, 12);
        List<ScaleRelativeChord> entryChords = new ArrayList<>(chords.size());
        List<Double> entryProbabilities = new ArrayList<>(chords.size());
        double totalWeight = .0;
        for (Map.Entry<ScaleRelativeChord, Double> chord : chords.entrySet()) {
            entryChords.add(chord.getKey());
            entryProbabilities.add(chord.getValue());
            totalWeight += chord.getValue();
        }
        for (int i = 0; i < entryProbabilities.size(); i++) {
            entryProbabilities.set(i, entryProbabilities.get(i) / totalWeight);
        }
        model.put(pitchClass, new Entry(entryChords, entryProbabilities));
    }

    /**
     * Estimate the possible chords for a collection of onsets.
     *
     * @param fundamental
     *            The fundamental of the scale
     * @param onsets
     *            A collection of onsets
     * @return A list of estimated chords sorted by probability
     */
    public List<Chord> estimateChords(Note fundamental, List<Onset> onsets) {
        return estimateChords(fundamental, onsets, null);
    }

    /**
     * Estimate the possible chords for a collection of onsets.
     *
     * @param fundamental
     *            The fundamental of the scale
     * @param onsets
     *            A collection of onsets
     * @param probabilities
     *            If not null then the given list is cleared and filled with the
     *            probabilities of the returned chords
     * @return A list of estimated chords sorted by probability
     */
    public List<Chord> estimateChords(Note fundamental, List<Onset> onsets, List<Double> probabilities) {
        // Discard short onsets
        final int MIN_DURATION = 100;
        double totalDuration = onsets.stream()
                .filter(o -> o.getDuration() > MIN_DURATION)
                .mapToDouble(o -> o.getDuration())
                .sum();
        Iterator<Onset> relevantOnsets = onsets.stream()
                .filter(o -> o.getDuration() > MIN_DURATION)
                .iterator();
        // Finish if empty
        if (!relevantOnsets.hasNext()) {
            if (probabilities != null) {
                probabilities.clear();
            }
            return Collections.emptyList();
        }
        // Collect chord probabilities of first onset
        Map<ScaleRelativeChord, Double> chordProbabilitiesMap = new HashMap<>();
        Onset onset = relevantOnsets.next();
        Entry entry = model.get(fundamental.ascendingDistanceTo(Note.fromValue(onset.getPitch())));
        double onsetContribution = onset.getDuration() / totalDuration;
        for (int i = 0; i < entry.size(); i++) {
            ScaleRelativeChord chord = entry.chords.get(i);
            double probability = Math.log(entry.probabilities.get(i)) * onsetContribution;
            chordProbabilitiesMap.put(chord, probability);
        }
        // Compose probabilities
        while (relevantOnsets.hasNext()) {
            onset = relevantOnsets.next();
            entry = model.get(fundamental.ascendingDistanceTo(Note.fromValue(onset.getPitch())));
            onsetContribution = onset.getDuration() / totalDuration;
            Iterator<Map.Entry<ScaleRelativeChord, Double>> it = chordProbabilitiesMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ScaleRelativeChord, Double> chordProbability = it.next();
                int idx = entry.chords.indexOf(chordProbability.getKey());
                if (idx >= 0) {
                    double probabilityContribution = Math.log(entry.probabilities.get(idx)) * onsetContribution;
                    chordProbability.setValue(chordProbability.getValue() + probabilityContribution);
                } else {
                    // Discard the chord if it is not valid for the pitch
                    it.remove();
                }
            }
        }

        // Sort by probability
        List<Chord> chords = new ArrayList<>(chordProbabilitiesMap.size());
        if (probabilities != null) {
            probabilities.clear();
        }
        chordProbabilitiesMap.entrySet().stream()
                .sorted(Comparator.comparingDouble(e -> -e.getValue()))  // Greater to smaller
                .forEachOrdered(e -> {
                    chords.add(e.getKey().getChord(fundamental));
                    if (probabilities != null) {
                        probabilities.add(Math.exp(e.getValue()));
                    }
                });
        return chords;
    }

    /**
     * Pick a chord from an entry in the model.
     *
     * @param relativePitchClass
     *            Pitch class relative to the scale fundamental
     * @param value
     *            A value in the range [0, 1]
     * @return The chord corresponding to the given entry and value, or null if
     *         the entry does not exist in the model.
     */
    ScaleRelativeChord pickChord(int relativePitchClass, double value) {
        Entry entry = model.get(relativePitchClass);
        if (entry == null) {
            return null;
        }
        return entry.pickChord(value);
    }

    @Override
    public String toString() {
        return "PitchClassChordModel " + model;
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
            // Reorder by probability
            List<Integer> indices = IntStream.range(0, chords.size()).boxed()
                    .sorted(Comparator.comparingDouble(probabilities::get)).collect(Collectors.toList());
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
