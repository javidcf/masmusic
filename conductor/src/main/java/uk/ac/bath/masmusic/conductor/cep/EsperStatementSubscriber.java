package uk.ac.bath.masmusic.conductor.cep;

/**
 * Common interface for Esper subscribers that provide a custom query.
 *
 * Implementing classes should also include a void update method receiving a
 * {@link java.util.Map} with {@link String} as key and the expected type as
 * value according to the defined statement.
 *
 * @author Javier Dehesa
 */
public interface EsperStatementSubscriber {

    /**
     * Get the EPL statement the subscriber will listen to.
     *
     * @return The EPL statement
     */
    public String getStatement();
}
