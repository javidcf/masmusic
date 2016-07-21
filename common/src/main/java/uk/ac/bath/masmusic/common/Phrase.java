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
        final double position;
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
        PhraseElement(double position, ScoreElement scoreElement) {
            this.position = position;
            this.scoreElement = scoreElement;
        }

        @Override
        public String toString() {
            return String.format("%.2f: %s", position, scoreElement.toString());
        }
    }

    /**
     * Constructor.
     */
    public Phrase() {
        this.elements = new ArrayList<>();
    }

    /**
     * @return Number of elements in the phrase
     */
    public int size() {
        return elements.size();
    }

    /**
     * Add an element to the phrase.
     *
     * @param element
     *            Element to add
     * @param position
     *            Position of the new element in beats
     */
    public void addElement(ScoreElement element, double position) {
        int index;
        for (index = elements.size(); index > 0 && elements.get(index - 1).position > position; index--) {
        }
        elements.add(index, new PhraseElement(position, element));
    }

    /**
     * Retrieve the position of an element in the phrase.
     *
     * @param index
     *            Index of the element to query
     * @return Position of the element at the given index in beats
     */
    public double getPositionAt(int index) {
        return elements.get(index).position;
    }

    /**
     * Retrieve an element in the phrase.
     *
     * @param index
     *            Index of the element to query
     * @return Element at the given index
     */
    public ScoreElement getElementAt(int index) {
        return elements.get(index).scoreElement;
    }

    @Override
    public String toString() {
        return "Phrase " + elements;
    }

}
