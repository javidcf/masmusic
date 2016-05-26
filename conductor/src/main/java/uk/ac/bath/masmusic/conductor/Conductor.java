package uk.ac.bath.masmusic.conductor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.messaging.MessageChannel;

import uk.ac.bath.masmusic.beat.BeatRoot;
import uk.ac.bath.masmusic.conductor.cep.EsperMessageHandler;
import uk.ac.bath.masmusic.integration.ProtobufBase64MqttMessageConverter;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Conductor application.
 *
 * @author Javier Dehesa
 */
@SpringBootApplication
// @EnableIntegration  // Is this necessary?
public class Conductor implements CommandLineRunner {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(Conductor.class);

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
    @Value("${mqtt.client.id}")
    private String mqttClientId;
    @Value("${mqtt.hear.topic}")
    private String mqttHearTopic;

    @Value("${beat.tempo.min}")
    private int minTempo;
    @Value("${beat.tempo.max}")
    private int maxTempo;

    @Autowired
    private EsperMessageHandler messageHandler;

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
        SpringApplication.run(Conductor.class, args);
    }

    /**
     * @return MQTT channel
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * @return MQTT message converter for Protocol Buffers message
     */
    @Bean
    public MqttMessageConverter messageConverter() {
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
                mqttUrl, mqttClientId, mqttHearTopic);
        adapter.setConverter(messageConverter());
        adapter.setQos(mqttQos);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * @return MQTT flow
     */
    @Bean
    public IntegrationFlow mqttInFlow() {
        IntegrationFlows.from(mqttInbound());
        return IntegrationFlows.from(mqttInbound()).handle(messageHandler)
                .get();
    }

    /**
     * @return The BeatRoot beat tracker.
     */
    @Bean
    public BeatRoot beatRoot() {
        return new BeatRoot(minTempo, maxTempo);
    }

}
