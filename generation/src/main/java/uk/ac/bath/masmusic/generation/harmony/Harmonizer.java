package uk.ac.bath.masmusic.generation.harmony;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.bath.masmusic.common.Chord;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.TimeSignature;

/**
 * Music harmonizer based on learned probabilistic models.
 *
 * The harmonizer works based on the data in a {@link ChordBigramModel} and a
 * {@link PitchClassChordModel}. Once constructed, an harmonization must be
 * computed first by a successful call to {@link #harmonize}; the generated
 * harmonization can be then retrieved through {@link #getHarmony}.
 *
 * @author Javier Dehesa
 */
public class Harmonizer {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(Harmonizer.class);

    /** Number of measures in one harmonization period. */
    private final int harmonizationMeasuresPeriod;

    /** Chord bigram model. */
    private final ChordBigramModel chordBigramModel;

    /** Pitch class chord model. */
    private final PitchClassChordModel pitchClassChordModel;

    /** The rhythm used in the current harmonization. */
    private Rhythm rhythm;

    /** Base index for the harmonization segments. */
    private int baseIndex;

    /** Last produced harmonization. */
    private final List<Chord> harmonization;

    /**
     * Constructor.
     *
     * @param harmonizationMeasuresPeriod
     *            The number of measures in one harmonization period
     * @param chordBigramModel
     *            The chord bigram model
     * @param pitchClassChordModel
     *            The pitch class chord model
     */
    public Harmonizer(int harmonizationMeasuresPeriod, ChordBigramModel chordBigramModel,
            PitchClassChordModel pitchClassChordModel) {
        if (harmonizationMeasuresPeriod < 1) {
            throw new IllegalArgumentException("The harmonization period must be positive");
        }
        this.harmonizationMeasuresPeriod = harmonizationMeasuresPeriod;
        this.chordBigramModel = chordBigramModel;
        this.pitchClassChordModel = pitchClassChordModel;
        this.rhythm = null;
        this.baseIndex = -1;
        this.harmonization = new ArrayList<>();
    }

    /**
     * @return True if the harmonizer has an harmonization, false otherwise
     */
    public boolean hasHarmonization() {
        return !harmonization.isEmpty();
    }

    /**
     * Clears any existing harmonization in the harmonizer.
     */
    public void clearHarmonization() {
        harmonization.clear();
    }

    /**
     * Set the rhythm used by the harmonizer.
     *
     * If the harmonizer does not currently have any harmonization, then this
     * method just saves the given rhythm to be used on the next harmonization
     * process. However, if an harmonization exists, then it is adapted to match
     * the previous known rhythm as close as possible.
     *
     * @param rhythm
     *            The new rhythm for the harmonizer
     */
    public void setRhythm(Rhythm rhythm) {
        Objects.requireNonNull(rhythm);
        if (hasHarmonization()) {
            assert this.rhythm != null;
            if (!this.rhythm.getTimeSignature().equals(rhythm.getTimeSignature())) {
                throw new IllegalArgumentException(
                        "The time signature of the new rhythm must match the one used for hamornization");
            }
            long currentTime = System.currentTimeMillis();
            long currentReferenceBeat = this.rhythm.getBeat().closestBeat(currentTime);
            int currentDivisionId = getDivisionId(currentReferenceBeat, this.rhythm);
            long newReferenceBeat = rhythm.getBeat().closestBeat(currentReferenceBeat);
            int newDivisionId = getDivisionId(newReferenceBeat, this.rhythm);
            baseIndex = (baseIndex + (newDivisionId - currentDivisionId)) % harmonization.size();
        }
        this.rhythm = rhythm;
    }

    /**
     * Get the harmony for a music segment.
     *
     * Before calling this method, a harmonization must have been computed
     * before through a successful call to {@link #harmonize} (that is,
     * {@link #hasHarmonization} must be true).
     *
     * @param timestamp
     *            Timestamp of the first harmony bar; if the timestamp does not
     *            match exactly the beginning of a bar, then the next closest
     *            bar will be the first one
     * @param bars
     *            Length of the generated harmony in bars
     * @param octave
     *            Octave of the generated harmony
     * @param velocity
     *            The velocity value of the generated onsets
     * @return The generated harmony
     */
    public List<Onset> getHarmony(long timestamp, int bars, int octave, int velocity) {
        if (bars < 0) {
            throw new IllegalArgumentException("The number of bars cannot be negative");
        }
        if (!hasHarmonization()) {
            throw new IllegalStateException("An harmonization must have been computed first");
        }
        timestamp = rhythm.nextBar(timestamp - 1);
        int measureDivisions = getMeasureDivisions(rhythm.getTimeSignature());
        int divisionLength = rhythm.getBarDuration() / measureDivisions;
        List<Onset> harmony = new ArrayList<>();
        for (int iBar = 0; iBar < bars; iBar++) {
            for (int iDivision = 0; iDivision < measureDivisions; iDivision++) {
                int divisionId = getDivisionId(timestamp, rhythm);
                Chord chord = harmonization.get(divisionId);
                LOG.debug("divisonId: {}, {}", divisionId, chord);
                for (int pitch : chord.getPitches(octave)) {
                    harmony.add(new Onset(timestamp, divisionLength, pitch, velocity));
                }
                timestamp += divisionLength;
            }
            timestamp = rhythm.closestBar(timestamp);
        }

        return harmony;
    }

    /**
     * Harmonize a melody.
     *
     * Tries to create an harmonization for the given parameters. If a complete
     * harmonization can be estimated then it is stored for later harmony
     * generation, otherwise the status of the harmonizer remains the same.
     *
     * @param scale
     *            Scale of the melody
     * @param rhythm
     *            Rhythm of the melody
     * @param onsets
     *            Onsets containing the melody
     * @return true if the harmonization was successful, false otherwise
     */
    public boolean harmonize(Scale scale, Rhythm rhythm, Collection<Onset> onsets) {
        Objects.requireNonNull(scale);
        Objects.requireNonNull(rhythm);
        if (onsets == null || onsets.isEmpty()) {
            return false;
        }

        LOG.debug("Harmonizing {} onsets in {}", onsets.size(), scale);

        // Snap onsets
        List<Onset> snapOnsets = onsets.stream().map(o -> rhythm.getBeat().snap(o, 2)).collect(Collectors.toList());

        // Collect onsets in the same subdivision
        Map<Integer, List<Onset>> groupedMap = IntStream.range(0, snapOnsets.size()).boxed()
                .collect(Collectors.groupingBy(i -> getDivisionId(snapOnsets.get(i).getTimestamp(), rhythm)))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().stream().map(i -> snapOnsets.get(i)).collect(Collectors.toList())));
        // Move to a list
        int totalDivisions = getMeasureDivisions(rhythm.getTimeSignature()) * harmonizationMeasuresPeriod;
        List<List<Onset>> grouped = new ArrayList<>(Collections.nCopies(totalDivisions, null));
        for (Map.Entry<Integer, List<Onset>> entry : groupedMap.entrySet()) {
            grouped.set(entry.getKey(), entry.getValue());
        }
        if (grouped.stream().anyMatch(l -> l == null || l.isEmpty())) {
            return false;
        }
        // Produce harmonization
        Chord previousChord = null;
        boolean done = false;
        int repeat = 0;
        List<Chord> newHarmonization = new ArrayList<>(totalDivisions);
        while (!done && repeat < 4) { // Up to 4 repetitions (not too good algorithm here)
            newHarmonization.clear();
            for (List<Onset> divisionOnsets : grouped) {
                Chord newChord = harmonizeDivision(scale.getFundamental(), divisionOnsets, previousChord);
                newHarmonization.add(newChord);
                previousChord = newChord;
            }
            // Check cycle is coherent
            Chord cycleChord = harmonizeDivision(scale.getFundamental(), grouped.get(0), previousChord);
            if (Objects.equals(cycleChord, newHarmonization.get(0))) {
                done = true;
            }
            repeat++;
        }
        LOG.debug("Harmonization process repeated {} time(s)", repeat);

        // Check harmonization is complete
        if (newHarmonization.contains(null)) {
            return false;
        }
        this.harmonization.clear();
        this.harmonization.addAll(newHarmonization);
        this.rhythm = rhythm;
        this.baseIndex = 0;
        return true;
    }

    /**
     * Find the chord for a melody division.
     *
     * @param fundamental
     *            The fundamental of the scale
     * @param onset
     *            Onsets in the division
     * @param previousChord
     *            The previous chord, or null if there is no known previous
     *            chord
     * @return The selected chord, or null if no appropriate chord could be
     *         found
     */
    private Chord harmonizeDivision(Note fundamental, List<Onset> onsets, Chord previousChord) {
        if (onsets.isEmpty()) {
            return null;
        }
        List<Double> pitchModelChordsProbs = new ArrayList<>();
        List<Chord> pitchModelChords = pitchClassChordModel.estimateChords(fundamental, onsets, pitchModelChordsProbs);
        if (previousChord == null) {
            if (!pitchModelChords.isEmpty()) {
                return pitchModelChords.get(0);
            } else {
                return null;
            }
        }
        if (pitchModelChords.isEmpty()) {
            return previousChord;
        }
        List<Double> bigramModelChordsProbs = new ArrayList<>();
        List<Chord> bigramModelChords = new ArrayList<>(
                chordBigramModel.estimateChords(fundamental, previousChord, bigramModelChordsProbs));
        // Add current chord with "probability" of ???
        bigramModelChords.add(previousChord);
        // bigramModelChordsProbs.add(1.0);
        // bigramModelChordsProbs.add(0.0);
        bigramModelChordsProbs.add(bigramModelChordsProbs.stream().mapToDouble(d -> d).average().orElse(1.0));

        // Combine estimations
        Optional<ChordProbability> bestChord = Stream.concat(
                // Concatenate estimations
                IntStream.range(0, pitchModelChords.size())
                        .mapToObj(i -> new ChordProbability(pitchModelChords.get(i), pitchModelChordsProbs.get(i))),
                IntStream.range(0, bigramModelChords.size())
                        .mapToObj(i -> new ChordProbability(bigramModelChords.get(i), bigramModelChordsProbs.get(i))))
                // Group estimations by chord
                .collect(Collectors.groupingBy(cp -> cp.chord)).entrySet().stream()
                // Discard elements not present in estimations of both models
                .filter(e -> e.getValue().size() > 1)
                // Multiply both probabilities
                .map(e -> new ChordProbability(e.getKey(),
                        e.getValue().stream().mapToDouble(cp -> cp.probability).reduce(1.0, (x, y) -> x * y)))
                // Get best
                .max(Comparator.comparing(cp -> cp.probability));

        if (bestChord.isPresent()) {
            return bestChord.get().chord;
        } else {
            return null;
        }
    }

    /**
     * Compute the number of measure divisions for a given time signature.
     *
     * @param timeSignature
     *            The time signature
     * @return The number of measure divisions that should be considered for the
     *         given time signature
     */
    private static int getMeasureDivisions(TimeSignature timeSignature) {
        int measureDivisions = timeSignature.getBeats();
        if (measureDivisions % 3 == 0) {
            return measureDivisions / 3;
        } else if (measureDivisions % 2 == 0) {
            return measureDivisions / 2;
        } else {
            return 1;
        }
    }

    /**
     * Compute the division id corresponding to a timestamp
     *
     * @param timestamp
     *            The timestamp for which the division id is computed
     * @return The division id for the timestamp
     */
    private int getDivisionId(long timestamp, Rhythm rhythm) {
        int measureDivisions = getMeasureDivisions(rhythm.getTimeSignature());
        int totalDivisions = measureDivisions * this.harmonizationMeasuresPeriod;
        double divisionLength = rhythm.getBarDuration() / ((double) measureDivisions);
        int firstMeasureOffset = rhythm.getBeat().getPhase() + rhythm.getBeatOffset() * rhythm.getBeat().getDuration();
        return (int) ((Math.round((timestamp - firstMeasureOffset - divisionLength / 2) / (divisionLength)) + baseIndex)
                % totalDivisions);
    }

    private static class ChordProbability {
        final Chord chord;
        final double probability;

        ChordProbability(Chord chord, double probability) {
            this.chord = chord;
            this.probability = probability;
        }

        @Override
        public String toString() {
            return "ChordProbability [chord=" + chord + ", probability=" + probability + "]";
        }
    }
}
