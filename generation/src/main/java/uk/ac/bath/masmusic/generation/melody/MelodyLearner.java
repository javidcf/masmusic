package uk.ac.bath.masmusic.generation.melody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.bath.masmusic.common.EvictingCircularBuffer;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Phrase;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.ScoreElement;

/**
 * Melody learner.
 *
 * @author Javier Dehesa
 */
public class MelodyLearner {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MelodyLearner.class);

    /** Musical phrases stored by bar class */
    private final List<EvictingCircularBuffer<Phrase>> phrases;

    /** Base index for the phrase bar classes. */
    private int baseIndex;

    /** The currently used rhythm. */
    private Rhythm rhythm;

    /** Treble/bass splitter. */
    private final TrebleBassSplitter splitter;

    /** Timestamp of the last learned music. */
    private long lastLearned;

    /**
     * Constructor.
     *
     * @param numBars
     *            Number of bar classes considered (a power of 2 value is
     *            advised)
     * @param numBarPhrases
     *            Number of phrases remembered for each bar class (the bigger
     *            the more memory about played music)
     */
    public MelodyLearner(int numBars, int numBarPhrases) {
        phrases = new ArrayList<>(numBars);
        for (int i = 0; i < numBars; i++) {
            phrases.add(new EvictingCircularBuffer<>(numBarPhrases));
        }
        baseIndex = 0;
        rhythm = null;
        splitter = new TrebleBassSplitter();
        lastLearned = -1;
    }

    /**
     * Updates the rhythm used by the copycat.
     *
     * @param rhythm
     *            The new rhythm for the copycat
     */
    public void setRhythm(Rhythm rhythm) {
        Objects.requireNonNull(rhythm);
        if (this.rhythm != null) {
            long currentTime = System.currentTimeMillis();
            long currentReferenceBeat = this.rhythm.getBeat().closestBeat(currentTime);
            int currentBarClass = getBarClass(currentReferenceBeat, this.rhythm);
            long newReferenceBeat = rhythm.getBeat().closestBeat(currentReferenceBeat);
            int newbarClass = getBarClass(newReferenceBeat, rhythm);
            baseIndex = Math.floorMod(baseIndex + (newbarClass - currentBarClass), phrases.size());
            if (lastLearned > 0) {
                lastLearned = rhythm.getBeat().closestBeat(lastLearned);
            }
        }
        this.rhythm = rhythm;
    }

    /**
     * Learn patterns from the given music.
     *
     * @param onsets
     *            Sequence of played notes
     */
    public void learn(List<Onset> onsets) {
        if (onsets.isEmpty() || rhythm == null) {
            return;
        }
        // Snap to beat and discard first and last bars
        long firstBar = rhythm.nextBar(onsets.get(0).getTimestamp());
        long lastBar = rhythm.currentBar(onsets.get(onsets.size() - 1).getTimestamp());
        List<Onset> snapOnsets = onsets.stream()
                .map(o -> rhythm.getBeat().snap(o, 2, 1))
                .filter(o -> o.getTimestamp() >= lastLearned
                        && o.getTimestamp() >= firstBar
                        && o.getTimestamp() < lastBar
                        && o.getDuration() > 0)
                .collect(Collectors.toList());
        lastLearned = lastBar;

        // Split
        List<Onset> bass = new ArrayList<>();
        List<Onset> treble = new ArrayList<>();
        splitter.split(snapOnsets, bass, treble);

        long currentBar = -1L;
        Phrase currentPhrase = null;
        double beatDuration = rhythm.getBeat().getDuration();
        for (Onset onset : treble) {
            long bar = rhythm.currentBar(onset.getTimestamp());
            if (bar != currentBar) {
                if (currentPhrase != null) {
                    int barClass = getBarClass(currentBar, rhythm);
                    LOG.debug("Learning a bar in class {}", barClass);
                    phrases.get(barClass).add(currentPhrase);
                }
                currentPhrase = new Phrase();
            }
            currentBar = bar;
            double position = (onset.getTimestamp() - currentBar) / beatDuration;
            double duration = (onset.getDuration()) / beatDuration;
            ScoreElement scoreElement = new ScoreElement(duration, Collections.singleton(onset.getPitch()));
            currentPhrase.addElement(scoreElement, position);
        }
        if (currentPhrase != null) {
            int barClass = getBarClass(currentBar, rhythm);
            LOG.debug("Learning a bar in class {}", barClass);
            phrases.get(barClass).add(currentPhrase);
        }
    }

    /**
     * Get a number of bars selected randomly from the learned music.
     *
     * @param timestamp
     *            Timestamp of the first melody bar; if the timestamp does not
     *            match exactly the beginning of a bar, then the next closest
     *            bar will be the first one
     * @param bars
     *            The number of bars
     * @param velocity
     *            The velocity of the generated notes
     * @param random
     *            RNG
     * @return The list of notes
     */
    public List<Onset> getRandomBars(long timestamp, int bars, int velocity, Random random) {
        if (bars < 0) {
            throw new IllegalArgumentException("The number of bars cannot be negative");
        }
        if (rhythm == null) {
            throw new IllegalStateException("No rhythm has been set");
        }
        timestamp = rhythm.nextBar(timestamp - 1);
        List<Onset> notes = new ArrayList<>();
        double beatDuration = rhythm.getBeat().getDuration();
        for (int i = 0; i < bars; i++) {
            int barClass = getBarClass(timestamp, rhythm);
            List<Phrase> barPhrases = phrases.get(barClass);
            if (!barPhrases.isEmpty()) {
                Phrase phrase = barPhrases.get(random.nextInt(barPhrases.size()));
                for (Phrase.Element phraseElement : phrase) {
                    long position = Math.round(timestamp + phraseElement.getPosition() * beatDuration);
                    int duration = Math.toIntExact(
                            Math.round(phraseElement.getScoreElement().getDuration() * beatDuration));
                    for (int pitch : phraseElement.getScoreElement().getPitches()) {
                        notes.add(new Onset(position, duration, pitch, velocity));
                    }
                }
            }
            timestamp = rhythm.nextBar(timestamp);
        }
        return notes;
    }

    /**
     * @param timestamp
     *            The timestamp
     * @param rhythm
     *            The rhythm
     * @return The bar class corresponding to the given timestamp and rhythm
     */
    private int getBarClass(long timestamp, Rhythm rhythm) {
        return (int) (((rhythm.currentBar(timestamp) - rhythm.getFirstBarOffset()) / rhythm.getBarDuration()
                + baseIndex) % phrases.size());
    }

}
