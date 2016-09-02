package uk.ac.bath.masmusic.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A musical phrase.
 *
 * A phrase consists on a sequence of notes.
 *
 * @author Javier Dehesa
 *
 */
public class Phrase implements Iterable<Phrase.Element> {

    private final List<Element> elements;

    /**
     * An element of a phrase.
     *
     * @author Javier Dehesa
     */
    public static class Element {
        /** Position of the element in the phrase in beats. */
        private final double       position;
        /** Score element. */
        private final ScoreElement scoreElement;

        /**
         * Constructor.
         *
         * @param position
         *            Position of the element in the phrase in beats
         * @param scoreElement
         *            Score element
         */
        Element(double position, ScoreElement scoreElement) {
            this.position = position;
            this.scoreElement = scoreElement;
        }

        /**
         * @return The position of the element in the phrase in beats
         */
        public double getPosition() {
            return position;
        }

        /**
         * @return The score element
         */
        public ScoreElement getScoreElement() {
            return scoreElement;
        }

        @Override
        public String toString() {
            return String.format("%.2f: %s", position, scoreElement.toString());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(position);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + ((scoreElement == null) ? 0 : scoreElement.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Element other = (Element) obj;
            if (Double.doubleToLongBits(position) != Double.doubleToLongBits(other.position)) {
                return false;
            }
            if (scoreElement == null) {
                if (other.scoreElement != null) {
                    return false;
                }
            } else if (!scoreElement.equals(other.scoreElement)) {
                return false;
            }
            return true;
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
     * @return The duration of the phrase in beats
     */
    public double getDuration() {
        if (size() < 1) {
            return 0;
        }
        double max = Double.NEGATIVE_INFINITY;
        for (Element element : this) {
            double current = element.getPosition() + element.getScoreElement().getDuration();
            if (current > max) {
                max = current;
            }
        }
        return max;
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
        elements.add(index, new Element(position, element));
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
    public Iterator<Element> iterator() {
        return elements.iterator();
    }

    @Override
    public String toString() {
        return "Phrase " + elements;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((elements == null) ? 0 : elements.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Phrase other = (Phrase) obj;
        if (elements == null) {
            if (other.elements != null) {
                return false;
            }
        } else if (!elements.equals(other.elements)) {
            return false;
        }
        return true;
    }

}
