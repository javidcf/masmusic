package uk.ac.bath.masmusic.orchestra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import uk.ac.bath.masmusic.integration.ProtobufBase64MqttMessageConverter;
import uk.ac.bath.masmusic.orchestra.mas.MasMusic;
import uk.ac.bath.masmusic.protobuf.TimePointNote;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Conductor application.
 *
 * @author Javier Dehesa
 */
@SpringBootApplication
@IntegrationComponentScan
// @EnableIntegration  // Is this necessary?
public class Orchestra implements CommandLineRunner {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(Orchestra.class);

    @Value("${mqtt.username}")
    private String mqttUsername;
    @Value("${mqtt.password}")
    private String mqttPassword;
    @Value("${mqtt.url}")
    private String mqttUrl;
    @Value("${mqtt.qos}")
    private int mqttQos;
    @Value("${mqtt.retain}")
    private boolean mqttRetain;
    @Value("${mqtt.hear.client.id}")
    private String mqttHearClientId;
    @Value("${mqtt.hear.topic}")
    private String mqttHearTopic;
    @Value("${mqtt.play.client.id}")
    private String mqttPlayClientId;
    @Value("${mqtt.play.topic}")
    private String mqttPlayTopic;

    @Autowired
    private MasMusic masMusic;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args) {
        LOG.info("Conductor started, press Ctrl+C to finish...");
    }

    /**
     * Start the conductor.
     *
     * @param args
     *            Command-line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SpringApplication.run(Orchestra.class, args);
    }

    /**
     * @return MQTT client factory
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setServerURIs(mqttUrl);
        factory.setUserName(mqttUsername);
        factory.setPassword(mqttPassword);
        return factory;
    }

    /**
     * @return MQTT input channel
     */
    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * @return MQTT output channel
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * @return MQTT input message converter for Protocol Buffers message
     */
    @Bean
    public MqttMessageConverter inConverter() {
        // return new ProtobufMqttMessageConverter<TimePointNote>(
        //        TimePointNote.class, mqttQos, mqttRetain);
        return new ProtobufBase64MqttMessageConverter<TimePointNote>(
                TimePointNote.class, mqttQos, mqttRetain);
    }

    /**
     * @return MQTT output message converter for Protocol Buffers message
     */
    @Bean
    public MqttMessageConverter outConverter() {
        // return new ProtobufMqttMessageConverter<TimeSpanNote>(
        //        TimeSpanNote.class, mqttQos, mqttRetain);
        return new ProtobufBase64MqttMessageConverter<TimeSpanNote>(
                TimeSpanNote.class, mqttQos, mqttRetain);
    }

    /**
     * @return MQTT message producer
     */
    @Bean
    public MessageProducerSupport mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqttHearClientId, mqttClientFactory(), mqttHearTopic);
        adapter.setConverter(inConverter());
        adapter.setQos(mqttQos);
        adapter.setOutputChannel(mqttInboundChannel());
        return adapter;
    }

    /**
     * @return MQTT message deliverer
     */
    @Bean
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                mqttPlayClientId, mqttClientFactory());
        handler.setDefaultTopic(mqttPlayTopic);
        handler.setDefaultQos(mqttQos);
        handler.setConverter(outConverter());
        return handler;
    }

    /**
     * @return MQTT input flow
     */
    @Bean
    public IntegrationFlow mqttInFlow() {
        return IntegrationFlows.from(mqttInboundChannel()).handle(masMusic)
                .get();
    }

    @Bean
    public IntegrationFlow mqttOutFlow() {
        // MessagingGateway
        return IntegrationFlows.from(mqttOutboundChannel())
                .handle(mqttOutbound()).get();
    }

}
