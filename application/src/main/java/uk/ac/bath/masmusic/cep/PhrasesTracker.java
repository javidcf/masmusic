package uk.ac.bath.masmusic.cep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.analysis.phrase.PhraseExtractor;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Phrase;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Extract phrases from Esper {@link TimeSpanNote} events.
 *
 * @author Javier Dehesa
 */
@Component
public class PhrasesTracker extends EsperStatementSubscriber {

    /** Quantization step size (ms) */
    private static final int QUANTIZATION = 40; // TODO Use this?

    /** Window size for beat analysis (ms) */
    private static final int ANALYSIS_WINDOW = 60000;

    /** Frequency of beat analysis (ms) */
    private static final int ANALYSIS_FREQUENCY = 5000;

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(PhrasesTracker.class);

    @Autowired
    private RhythmDetector rhythmDetector;

    /** Phrase extractor. */
    private final PhraseExtractor phraseExtractor;

    /** Events in the last analyzed window (sorted by time) */
    private final ArrayList<Onset> onsets;

    /** Extracted phrases. */
    private final AtomicReference<List<Phrase>> extractedPhrases;

    /**
     * Constructor.
     */
    public PhrasesTracker() {
        phraseExtractor = new PhraseExtractor();
        onsets = new ArrayList<>();
        extractedPhrases = new AtomicReference<>(Collections.emptyList());
    }

    /**
     * @return The list of extracted phrases
     */
    public List<Phrase> getExtractedPhrases() {
        return extractedPhrases.get();
    }

    /*** Esper ***/

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatementQuery() {
        return "select"
                // + " Math.round(avg(timestamp)) as timestamp"
                + " noteOnset(*) as onset" + " from TimeSpanNote.win:time(" + ANALYSIS_WINDOW + " msec) "
                // + " group by Math.round(timestamp / " + QUANTIZATION + ")"
                + " output snapshot every " + ANALYSIS_FREQUENCY + " msec" + " order by timestamp asc";
    }

    /**
     * Start new event delivery.
     *
     * @param countNew
     *            Number of elements in the new delivery
     * @param countOld
     *            Number of elements in the previous delivery
     */
    public void updateStart(int countNew, int countOld) {
        onsets.clear();
        onsets.ensureCapacity(countNew);
    }

    /**
     * Receive query event.
     *
     * @param eventMap
     *            Query event data
     */
    public void update(Map<String, Onset> eventMap) {
        onsets.add(eventMap.get("onset"));
    }

    /**
     * Finish event delivery.
     */
    public void updateEnd() {
        if (onsets.isEmpty()) {
            return;
        }
        Rhythm rhythm = rhythmDetector.getDetectedRhtyhm();
        if (rhythm == null) {
            return;
        }

        List<Phrase> phrases = phraseExtractor.extractPhrases(onsets, rhythm);
        extractedPhrases.set(Collections.unmodifiableList(phrases));

        onsets.clear();
    }

}
