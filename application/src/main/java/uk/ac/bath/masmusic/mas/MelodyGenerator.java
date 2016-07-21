package uk.ac.bath.masmusic.mas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.ScoreElement;
import uk.ac.bath.masmusic.generation.MarkovDurationGeneratorTable;
import uk.ac.bath.masmusic.generation.MarkovDurationGeneratorTableReader;
import uk.ac.bath.masmusic.generation.MarkovMelodyGenerator;
import uk.ac.bath.masmusic.generation.MarkovPitchGeneratorTable;
import uk.ac.bath.masmusic.generation.MarkovPitchGeneratorTableReader;

/**
 * Melody generator.
 *
 * Generates music using learned Markov tables.
 *
 * @author Javier Dehesa
 */
@Component
public class MelodyGenerator {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MelodyGenerator.class);

    /** String format for pitch table resources. */
    private static final String PITCH_TABLE_RESOURCE_FORMAT = "classpath:generation/%s.pit";

    /** String format for duration table resources. */
    private static final String DURATION_TABLE_RESOURCE_FORMAT = "classpath:generation/%s.dur";

    /** Low bound for pitch values. */
    private final static int PITCH_BOUND_LOW = 36;

    /** High bound for pitch values. */
    private final static int PITCH_BOUND_HIGH = 84;

    @Autowired
    private ApplicationContext ctx;

    /** Loaded pitch Markov tables. */
    private final Map<String, MarkovPitchGeneratorTable> pitchTables;

    /** Loaded duration Markov tables. */
    private final Map<String, MarkovDurationGeneratorTable> durationTables;

    /**
     * Constructor.
     */
    public MelodyGenerator() {
        pitchTables = new HashMap<>();
        durationTables = new HashMap<>();
    }

    /**
     * Generate a melody of a fixed length.
     *
     * @param scale
     *            Scale of the generated melody
     * @param beats
     *            Duration of the melody in beats
     */
    public List<ScoreElement> generateMelody(Scale scale, int beats) {
        LOG.debug("Generating {} beats of melody in scale {}", beats, scale);
        String scaleType = scale.getName();
        MarkovPitchGeneratorTable pitchTable = getPitchTable(scaleType);
        if (pitchTable == null) {
            throw new IllegalArgumentException("No pitch table available for scale type '" + scaleType + "'");
        }
        MarkovDurationGeneratorTable durationTable = getDurationTable(scaleType);
        if (durationTable == null) {
            throw new IllegalArgumentException("No duration table available for scale type '" + scaleType + "'");
        }

        // Generate music
        MarkovMelodyGenerator melodyGenerator = new MarkovMelodyGenerator(pitchTable, durationTable, scale);
        melodyGenerator.setPitchBounds(PITCH_BOUND_LOW, PITCH_BOUND_HIGH);
        double generatedLength = .0;
        List<ScoreElement> generatedElements = new ArrayList<>();
        ScoreElement generated = melodyGenerator.getCurrentElement();
        while (generatedLength + generated.getDuration() <= beats) {
            generatedElements.add(generated);
            generatedLength += generated.getDuration();
            generated = melodyGenerator.generateElement();
        }
        double gap = beats - generatedLength;
        if (gap >= .25) {
            // Add the last generated element to fill the gap
            ScoreElement filler = new ScoreElement(gap, generated.getPitches());
            generatedElements.add(filler);
        }
        return generatedElements;
    }

    /**
     * @param scaleType
     *            A scale type
     * @return The pitch table corresponding to the given scale type, or null if
     *         the table does not exist
     */
    private MarkovPitchGeneratorTable getPitchTable(String scaleType) {
        if (!pitchTables.containsKey(scaleType)) {
            Resource tableRes = ctx.getResource(String.format(PITCH_TABLE_RESOURCE_FORMAT, scaleType.toLowerCase()));
            if (tableRes.exists()) {
                try (MarkovPitchGeneratorTableReader reader = new MarkovPitchGeneratorTableReader(
                        tableRes.getInputStream())) {
                    pitchTables.put(scaleType, reader.readTable(true));
                } catch (IOException e) {
                }
            }
        }
        return pitchTables.get(scaleType);
    }

    /**
     * @param scaleType
     *            A scale type
     * @return The duration table corresponding to the given scale type, or null
     *         if the table does not exist
     */
    private MarkovDurationGeneratorTable getDurationTable(String scaleType) {
        if (!durationTables.containsKey(scaleType)) {
            Resource tableRes = ctx.getResource(String.format(DURATION_TABLE_RESOURCE_FORMAT, scaleType.toLowerCase()));
            if (tableRes.exists()) {
                try (MarkovDurationGeneratorTableReader reader = new MarkovDurationGeneratorTableReader(
                        tableRes.getInputStream())) {
                    durationTables.put(scaleType, reader.readTable());
                } catch (IOException e) {
                }
            }
        }
        return durationTables.get(scaleType);
    }
}
