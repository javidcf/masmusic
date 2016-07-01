package uk.ac.bath.masmusic.conductor.analysis;

import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.bath.masmusic.common.Phrase;
import uk.ac.bath.masmusic.common.ScoreElement;

/**
 * Tracks the creation of a {@link Phrase}.
 *
 * @author Javier Dehesa
 */
public class PhraseTracker implements Comparable<PhraseTracker> {

    /** Phrase being created. */
    private final Phrase phrase;

    /** Initial position of the tracker in beats. */
    private float initialPosition;

    /** Position of the tracker in beats. */
    private float position;

    /** Lowest pitch in the tracked phrase. */
    private int lowestPitch;

    /** Tracker id. */
    private final int id;

    /** Tracker id generator. */
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    /**
     * Constructor.
     */
    public PhraseTracker() {
        this(0f);
    }

    /**
     * Constructor.
     *
     * @param initialPosition
     *            Initial position of the tracker in beats
     */
    public PhraseTracker(float initialPosition) {
        this.phrase = new Phrase();
        this.initialPosition = initialPosition;
        this.position = initialPosition;
        this.lowestPitch = Integer.MAX_VALUE;
        this.id = ID_GENERATOR.getAndIncrement();
    }

    /**
     * @return The phrase being created
     */
    public Phrase getPhrase() {
        return phrase;
    }

    /**
     * @return The position of the tracker in beats
     */
    public float getPosition() {
        return position;
    }

    /**
     * @return The lowest pitch in the tracked phrase
     */
    public int getLowestPitch() {
        return lowestPitch;
    }

    /**
     * Add an score element at the end of the tracked phrase.
     *
     * @param element
     *            Element to add
     */
    public void addElement(ScoreElement element) {
        addElement(element, position);
    }

    /**
     * Add an score element to the tracked phrase.
     *
     * @param element
     *            Element to add
     * @param elementPosition
     *            Position of the added element in beats
     */
    public void addElement(ScoreElement element, float elementPosition) {
        float phraseElementPosition = elementPosition - initialPosition;
        phrase.addElement(element, phraseElementPosition);
        position = elementPosition + element.getDuration();
        for (int pitch : element.getPitches()) {
            if (pitch < lowestPitch) {
                lowestPitch = pitch;
            }
        }
    }

    @Override
    public int compareTo(PhraseTracker that) {
        int cmp = Float.compare(this.position, that.position);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(this.lowestPitch, that.lowestPitch);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(this.id, that.id);
    }
}
