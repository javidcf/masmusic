package uk.ac.bath.masmusic.mas;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.TimeSignature;
import uk.ac.bath.masmusic.generation.harmony.ChordBigramModel;
import uk.ac.bath.masmusic.generation.harmony.ChordBigramModelReader;
import uk.ac.bath.masmusic.generation.harmony.Harmonizer;
import uk.ac.bath.masmusic.generation.harmony.PitchClassChordModel;
import uk.ac.bath.masmusic.generation.harmony.PitchClassChordModelReader;

/**
 * Harmony generator.
 *
 * Generates harmony using an {@link Harmonizer}.
 *
 * @author Javier Dehesa
 */
@Component
public class HarmonyGenerator {

    private static final int HARMONIZATION_MEASURES_PERIOD = 8;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(HarmonyGenerator.class);

    /** String format for chord bigram model resources. */
    private static final String CHORD_BIGRAM_MODEL_RESOURCE_FORMAT = "classpath:generation/%s.cbm";

    /** String format for pitch class model resources. */
    private static final String PITCH_CLASS_CHORD_MODEL_RESOURCE_FORMAT = "classpath:generation/%s.pcm";

    @Autowired
    private ApplicationContext ctx;

    /** Loaded chord bigram models. */
    private final Map<String, ChordBigramModel> chordBigramModels;

    /** Loaded pitch class chord models. */
    private final Map<String, PitchClassChordModel> pitchClassChordModels;

    /** The harmonization scale. */
    private Scale scale;

    /** The harmonization time signature. */
    private TimeSignature timeSignature;

    /** The harmonization beat offset. */
    private int beatOffset;

    /** Harmonizer. */
    private Harmonizer harmonizer;

    /**
     * Constructor.
     */
    public HarmonyGenerator() {
        chordBigramModels = new HashMap<>();
        pitchClassChordModels = new HashMap<>();
        scale = null;
        timeSignature = null;
        beatOffset = -1;
        harmonizer = null;
    }

    /**
     * @return True if the harmonizer has an harmonization, false otherwise
     */
    public synchronized boolean hasHarmonization() {
        return harmonizer != null && harmonizer.hasHarmonization();
    }

    /**
     * Get the harmony for a music segment.
     *
     * @param rhythm
     *            The rhythm of the generated harmony
     * @param scale
     *            The scale of the generated harmony
     * @param timestamp
     *            Timestamp of the first harmony bar; if the timestamp does not
     *            match exactly the beginning of a bar, then the next closest
     *            bar will be the first one
     * @param bars
     *            Length of the generated harmony in bars
     * @return The generated harmony, or an empty list if no harmony could be
     *         generated
     */
    public synchronized List<Onset> getHarmony(Rhythm rhythm, Scale scale, long timestamp, int bars) {
        Objects.requireNonNull(rhythm);
        Objects.requireNonNull(scale);
        if (bars < 0) {
            throw new IllegalArgumentException("The number of bars cannot be negative");
        }
        if (rhythm.getTimeSignature().equals(timeSignature)
                && rhythm.getBeatOffset() == beatOffset
                && scale.equals(this.scale)
                && harmonizer.hasHarmonization()) {
            return harmonizer.getHarmony(rhythm, timestamp, bars, 3, MasMusic.DEFAULT_VELOCITY);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Harmonize a melody.
     *
     * @param scale
     *            Scale of the melody
     * @param rhythm
     *            Rhythm of the melody
     * @param onsets
     *            Onsets containing the melody
     */
    public synchronized void harmonize(Scale scale, Rhythm rhythm, List<Onset> onsets) {
        Objects.requireNonNull(scale);
        Objects.requireNonNull(rhythm);
        Objects.requireNonNull(onsets);
        LOG.debug("Harmonizing melody in {}", scale);
        // Create a new harmonizer if the scale type or the time signature has changed
        if (this.scale == null
                || !scale.getType().equals(this.scale.getType())
                || this.timeSignature == null
                || !rhythm.getTimeSignature().equals(this.timeSignature)
                || this.beatOffset != rhythm.getBeatOffset()) {
            String scaleType = scale.getType();
            ChordBigramModel chordBigramModel = getChordBigramModel(scaleType);
            if (chordBigramModel == null) {
                throw new IllegalArgumentException(
                        "No chord bigram model available for scale type '" + scaleType + "'");
            }
            PitchClassChordModel pitchClassChordModel = getPitchClassChordModel(scaleType);
            if (pitchClassChordModel == null) {
                throw new IllegalArgumentException(
                        "No pitch class chord model available for scale type '" + scaleType + "'");
            }
            harmonizer = new Harmonizer(HARMONIZATION_MEASURES_PERIOD, chordBigramModel, pitchClassChordModel);
        }
        boolean harmonized = harmonizer.harmonize(scale, rhythm, onsets);
        if (harmonized) {
            this.scale = scale;
            this.timeSignature = rhythm.getTimeSignature();
            this.beatOffset = rhythm.getBeatOffset();
        } else {
            LOG.debug("Could not perform harmonization");
        }
    }

    /**
     * @param scaleType
     *            A scale type
     * @return The chord bigram model corresponding to the given scale type, or
     *         null if the table does not exist
     */
    private ChordBigramModel getChordBigramModel(String scaleType) {
        if (!chordBigramModels.containsKey(scaleType)) {
            Resource tableRes = ctx
                    .getResource(String.format(CHORD_BIGRAM_MODEL_RESOURCE_FORMAT, scaleType.toLowerCase()));
            if (tableRes.exists()) {
                try (ChordBigramModelReader reader = new ChordBigramModelReader(
                        tableRes.getInputStream())) {
                    chordBigramModels.put(scaleType, reader.readModel());
                } catch (IOException e) {
                }
            }
        }
        return chordBigramModels.get(scaleType);
    }

    /**
     * @param scaleType
     *            A scale type
     * @return The pitch class chord model corresponding to the given scale
     *         type, or null if the table does not exist
     */
    private PitchClassChordModel getPitchClassChordModel(String scaleType) {
        if (!pitchClassChordModels.containsKey(scaleType)) {
            Resource tableRes = ctx
                    .getResource(String.format(PITCH_CLASS_CHORD_MODEL_RESOURCE_FORMAT, scaleType.toLowerCase()));
            if (tableRes.exists()) {
                try (PitchClassChordModelReader reader = new PitchClassChordModelReader(
                        tableRes.getInputStream())) {
                    pitchClassChordModels.put(scaleType, reader.readModel());
                } catch (IOException e) {
                }
            }
        }
        return pitchClassChordModels.get(scaleType);
    }
}
