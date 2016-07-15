package uk.ac.bath.masmusic.mas;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import uk.ac.bath.masmusic.cep.PhrasesTracker;
import uk.ac.bath.masmusic.cep.RhythmDetector;
import uk.ac.bath.masmusic.cep.ScaleTracker;
import uk.ac.bath.masmusic.common.Phrase;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.ScoreElement;
import uk.ac.bath.masmusic.generation.MarkovPitchGenerator;
import uk.ac.bath.masmusic.generation.MarkovPitchGeneratorTable;
import uk.ac.bath.masmusic.generation.MarkovPitchGeneratorTableReader;

/**
 * Melody generator.
 *
 * Generates music using learned Markov tables.
 *
 * @author Javier Dehesa
 */
public class MelodyGenerator {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MelodyGenerator.class);

    /** RNG. */
    private final static Random RNG = new Random();

    /** String format for table resources. */
    private static final String TABLE_RESOURCE_FORMAT = "classpath:generation/%s.tab";

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private RhythmDetector rhythmDetector;

    @Autowired
    private ScaleTracker scaleTracker;

    @Autowired
    private PhrasesTracker phrasesTracker;

    /** Loaded Markov tables. */
    private Map<String, MarkovPitchGeneratorTable> tables;

    public void generateMelody() {
        Rhythm rhythm = rhythmDetector.getDetectedRhtyhm();
        if (rhythm == null) {
            LOG.warn("Cannot generate melody: rhythm not available");
            return;
        }
        Scale scale = scaleTracker.getCurrentScale();
        if (scale == null) {
            LOG.warn("Cannot generate melody: scale not available");
            return;
        }
        List<Phrase> phrases = phrasesTracker.getExtractedPhrases();
        if (phrases.isEmpty()) {
            LOG.warn("Cannot generate melody: no phrases detected");
            return;
        }
        String scaleType = scale.getName();
        MarkovPitchGeneratorTable table = getMarkovTable(scaleType);
        if (table == null) {
            LOG.warn("Cannot generate melody: Markov table not available");
            return;
        }

        // Generate music
        MarkovPitchGenerator generator = new MarkovPitchGenerator(table, scale);
        Phrase newPhrase = new Phrase();
        // Pick random phrase
        Phrase basePhrase = phrases.get(RNG.nextInt(phrases.size()));
        // Copy the beginning of the phrase
        for (int i = 0; i < basePhrase.size(); i++) {
            ScoreElement element = basePhrase.getElementAt(i);
            float position = basePhrase.getPositionAt(i);
            if (i < table.getOrder()) {
                if (element.getPitches().size() != 1) {
                    LOG.warn("Phrase score elements should have one pitch");
                }
                int pitch = element.getPitches().iterator().next();
                newPhrase.addElement(element, position);
                generator.providePitch(pitch);
            } else {
                int newPitch = generator.generatePitch();
                ScoreElement newElement = new ScoreElement(element.getDuration(), Collections.singleton(newPitch));
                newPhrase.addElement(newElement, position);
            }
        }

        // TODO Output the music somewhere/somehow
    }

    /**
     * @param scaleType
     *            A scale type
     * @return The table corresponding to the given scale type, or null if the
     *         table does not exist
     */
    private MarkovPitchGeneratorTable getMarkovTable(String scaleType) {
        if (!tables.containsKey(scaleType)) {
            Resource tableRes = ctx.getResource(String.format(TABLE_RESOURCE_FORMAT, scaleType.toLowerCase()));
            if (tableRes.exists()) {
                try (MarkovPitchGeneratorTableReader reader = new MarkovPitchGeneratorTableReader(
                        tableRes.getInputStream())) {
                    tables.put(scaleType, reader.readTable());
                } catch (IOException e) {
                }
            }
        }
        return tables.get(scaleType);
    }
}
