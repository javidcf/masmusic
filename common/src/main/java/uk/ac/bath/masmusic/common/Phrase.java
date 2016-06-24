package uk.ac.bath.masmusic.common;

import java.util.ArrayList;
import java.util.List;

/**
 * A musical phrase.
 *
 * A phrase consists on a sequence of notes.
 *
 * @author Javier Dehesa
 *
 */
public class Phrase {

    private final List<PhraseElement> elements;

    /**
     * An element of a phrase.
     *
     * @author Javier Dehesa
     */
    private static class PhraseElement {
        /** Position of the element in the phrase in beats. */
        final float position;
        /** Score element. */
        final ScoreElement scoreElement;

        /**
         * Constructor.
         *
         * @param position
         *            Position of the element in the phrase in beats
         * @param scoreElement
         *            Score element
         */
        PhraseElement(float position, ScoreElement scoreElement) {
            this.position = position;
            this.scoreElement = scoreElement;
        }
    }

    public Phrase() {
        this.elements = new ArrayList<>();
    }

}
