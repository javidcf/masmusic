package uk.ac.bath.masmusic.conductor.cep;

import com.espertech.esper.client.EPStatement;

/**
 * Common interface for Esper subscribers that provide a custom query.
 *
 * Implementing classes should also include a void update method receiving a
 * {@link java.util.Map} with {@link String} as key and the expected type as
 * value according to the defined statement.
 *
 * @author Javier Dehesa
 */
public abstract class EsperStatementSubscriber {

    private EPStatement statement;

    public void setStatement(EPStatement statement) {
        this.statement = statement;
    }

    public EPStatement getStatement() {
        return statement;
    }

    /**
     * Get the EPL statement query the subscriber will listen to.
     *
     * @return The EPL statement query
     */
    public abstract String getStatementQuery();
}
