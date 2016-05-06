package uk.ac.bath.masmusic;

import jason.asSyntax.Literal;

/**
 * An entity able to receive Jason perception literals.
 *
 * @author Javier Dehesa
 *
 */
public interface Perceiver {

    /**
     * Perceive a new literal.
     * 
     * @param literal
     *            Perceived literal
     */
    public void addPercept(Literal literal);
}
