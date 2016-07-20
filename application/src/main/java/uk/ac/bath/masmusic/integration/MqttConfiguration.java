package uk.ac.bath.masmusic.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import uk.ac.bath.masmusic.cep.EsperMessageHandler;
import uk.ac.bath.masmusic.mas.MasMusic;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * MQTT integration configuration.
 *
 * @author Javier Dehesa
 */
@Configuration
@IntegrationComponentScan
//@EnableIntegration // Is this necessary?
public class MqttConfiguration {

    @Value("${mqtt.username}")
    private String  mqttUsername;
    @Value("${mqtt.password}")
    private String  mqttPassword;
    @Value("${mqtt.url}")
    private String  mqttUrl;
    @Value("${mqtt.qos}")
    private int     mqttQos;
    @Value("${mqtt.retain}")
    private boolean mqttRetain;
    @Value("${mqtt.hear.client.id}")
    private String  mqttHearClientId;
    @Value("${mqtt.hear.topic}")
    private String  mqttHearTopic;
    @Value("${mqtt.play.client.id}")
    private String  mqttPlayClientId;
    @Value("${mqtt.play.topic}")
    private String  mqttPlayTopic;

    @Autowired
    private EsperMessageHandler esperMessageHandler;

    @Autowired
    private MasMusic masMusic;

    /**
     * @return MQTT client factory
     */
    @Bean
    public MqttPahoClientFactory clientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setServerURIs(mqttUrl);
        factory.setUserName(mqttUsername);
        factory.setPassword(mqttPassword);
        return factory;
    }

    /**
     * @return MQTT hear channel
     */
    @Bean
    public MessageChannel hearChannel() {
        return new PublishSubscribeChannel();
    }

    /**
     * @return MQTT hear message producer
     */
    @Bean
    public MessageProducerSupport hearProducer() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqttHearClientId, clientFactory(), mqttHearTopic);
        adapter.setConverter(new ProtobufMqttMessageConverter(TimeSpanNote.class, mqttQos, mqttRetain));
        adapter.setQos(mqttQos);
        adapter.setOutputChannel(hearChannel());
        return adapter;
    }

    /**
     * @return MQTT hear MasMusic flow
     */
    @Bean
    public IntegrationFlow hearMasMusicFlow() {
        return IntegrationFlows.from(hearChannel()).handle(masMusic).get();
    }

    /**
     * @return MQTT hear Esper flow
     */
    @Bean
    public IntegrationFlow hearEsperFlow() {
        return IntegrationFlows.from(hearChannel()).handle(esperMessageHandler).get();
    }

    /**
     * @return MQTT message deliverer
     */
    @Bean
    public MessageHandler playConsumer() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(mqttPlayClientId, clientFactory());
        handler.setDefaultTopic(mqttPlayTopic);
        handler.setDefaultQos(mqttQos);
        handler.setConverter(new ProtobufMqttMessageConverter(TimeSpanNote.class, mqttQos, mqttRetain));
        return handler;
    }

    /**
     * @return MQTT play channel
     */
    @Bean
    public MessageChannel playChannel() {
        return new DirectChannel();
    }

    /**
     * @return MQTT play flow
     */
    @Bean
    public IntegrationFlow playFlow() {
        return IntegrationFlows.from(playChannel()).handle(playConsumer()).get();
    }

}
