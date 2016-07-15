package uk.ac.bath.masmusic.cep;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;

import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Esper-related application configuration.
 *
 * @author Javier Dehesa
 */
@Configuration
public class EsperConfiguration {

    /**
     * @return Esper configuration
     */
    @Bean
    public com.espertech.esper.client.Configuration epConfiguration() {
        com.espertech.esper.client.Configuration config = new com.espertech.esper.client.Configuration();
        config.addEventTypeAutoName("uk.ac.bath.masmusic.protobuf");
        config.addPlugInSingleRowFunction(
                "noteOnset", "uk.ac.bath.masmusic.cep.EsperConfiguration", "noteOnset");
        return config;
    }

    /**
     * Esper service provider.
     *
     * The service provider is configured with every
     * {@link EsperStatementSubscriber} available.
     *
     * @param subscribers
     *            Available statement subscribers
     * @return Esper service provider
     */
    @Bean
    public EPServiceProvider epService(
            com.espertech.esper.client.Configuration config, List<EsperStatementSubscriber> subscribers) {
        // Get Esper provider
        EPServiceProvider provider = EPServiceProviderManager.getDefaultProvider(config);
        // Add statements and listeners
        for (EsperStatementSubscriber subscriber : subscribers) {
            EPStatement statement = provider.getEPAdministrator().createEPL(subscriber.getStatementQuery());
            statement.setSubscriber(subscriber);
            subscriber.setStatement(statement);
        }
        return provider;
    }

    /**
     * @param note
     *            A spanned note message
     * @return An onset representing the note message
     */
    public static Onset noteOnset(TimeSpanNote note) {
        int absolutePitch = note.getPitch().getNote().getNumber() + (note.getPitch().getOctave() + 1) * 12;
        return new Onset(note.getTimestamp(), note.getDuration(), absolutePitch, note.getVelocity());
    }
}
