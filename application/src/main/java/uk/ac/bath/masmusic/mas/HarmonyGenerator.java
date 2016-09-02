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
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.events.RhythmUpdatedEvent;
import uk.ac.bath.masmusic.events.ScaleUpdatedEvent;
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

    private static final int HARMONIZATION_MEASURES_PERIOD = 4;

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

    /** Current rhythm. */
    private Rhythm rhythm;

    /** Current rhythm. */
    private Scale scale;

    /** Harmonizer. */
    private Harmonizer harmonizer;

    /**
     * Constructor.
     */
    public HarmonyGenerator() {
        chordBigramModels = new HashMap<>();
        pitchClassChordModels = new HashMap<>();
        rhythm = null;
        scale = null;
    }

    /**
     * Handle a rhythm update event.
     *
     * @param event
     *            The rhythm update event
     */
    @EventListener
    public synchronized void onRhythmUpdated(RhythmUpdatedEvent event) {
        setRhythm(event.getRhythm());
    }

    /**
     * Set the harmonization rhythm.
     *
     * @param rhythm
     *            The new rhythm
     */
    private synchronized void setRhythm(Rhythm rhythm) {
        if (this.rhythm != null && !this.rhythm.getTimeSignature().equals(rhythm.getTimeSignature())) {
            harmonizer.clearHarmonization();
        }
        this.rhythm = rhythm;
        if (harmonizer != null) {
            harmonizer.setRhythm(rhythm);
        }
    }

    /**
     * Handle a scale update event.
     *
     * @param event
     *            The scale update event
     */
    @EventListener
    public void onScaleUpdated(ScaleUpdatedEvent event) {
        setScale(event.getScale());
    }

    /**
     * Set the harmonization scale.
     *
     * @param scale
     *            The new scale
     */
    private synchronized void setScale(Scale scale) {
        // Create new harmonizer on new scale type
        if (this.scale == null || !this.scale.getType().equalsIgnoreCase(scale.getType())) {
            String scaleType = scale.getType().toLowerCase();
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
            if (this.rhythm != null) {
                harmonizer.setRhythm(rhythm);
            }
        }
        // Clear harmonization on new scale
        if (!scale.equals(this.scale)) {
            harmonizer.clearHarmonization();
        }
        this.scale = scale;
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
     * @param scale
     *            The scale of the harmony
     * @param rhythm
     *            The rhythm of the harmony
     * @param timestamp
     *            Timestamp of the first harmony bar; if the timestamp does not
     *            match exactly the beginning of a bar, then the next closest
     *            bar will be the first one
     * @param bars
     *            Length of the generated harmony in bars
     * @return The generated harmony, or an empty list if no harmony could be
     *         generated
     */
    public synchronized List<Onset> getHarmony(Scale scale, Rhythm rhythm, long timestamp, int bars) {
        Objects.requireNonNull(scale);
        Objects.requireNonNull(rhythm);
        setScale(scale);
        setRhythm(rhythm);
        return getHarmony(timestamp, bars);
    }

    /**
     * Get the harmony for a music segment.
     *
     * @param timestamp
     *            Timestamp of the first harmony bar; if the timestamp does not
     *            match exactly the beginning of a bar, then the next closest
     *            bar will be the first one
     * @param bars
     *            Length of the generated harmony in bars
     * @return The generated harmony, or an empty list if no harmony could be
     *         generated
     */
    public synchronized List<Onset> getHarmony(long timestamp, int bars) {
        if (bars < 0) {
            throw new IllegalArgumentException("The number of bars cannot be negative");
        }
        if (rhythm != null && scale != null && harmonizer.hasHarmonization()) {
            return harmonizer.getHarmony(timestamp, bars, 3, MasMusic.DEFAULT_VELOCITY);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Harmonize a melody.
     *
     * @param scale
     *            The harmonization scale
     * @param rhythm
     *            The harmonization rhythm
     * @param onsets
     *            Onsets containing the melody
     */
    public synchronized void harmonize(Scale scale, Rhythm rhythm, List<Onset> onsets) {
        Objects.requireNonNull(scale);
        Objects.requireNonNull(rhythm);
        setScale(scale);
        setRhythm(rhythm);
        harmonize(onsets);
    }

    /**
     * Harmonize a melody.
     *
     * @param onsets
     *            Onsets containing the melody
     */
    public synchronized void harmonize(List<Onset> onsets) {
        LOG.debug("Harmonizing melody in {}", scale);
        if (scale == null || rhythm == null || harmonizer == null) {
            return;
        }
        boolean harmonized = harmonizer.harmonize(scale, rhythm, onsets);
        if (!harmonized) {
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
        scaleType = scaleType.toLowerCase();
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
        scaleType = scaleType.toLowerCase();
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
