package uk.ac.bath.masmusic.conductor.cep;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;

@Configuration
public class EsperConfiguration {

    @Bean
    public EPServiceProvider epService() {
        com.espertech.esper.client.Configuration config = new com.espertech.esper.client.Configuration();
        config.addEventTypeAutoName("uk.ac.bath.masmusic.protobuf");
        return EPServiceProviderManager.getDefaultProvider(config);
    }
}
