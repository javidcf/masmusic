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
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import uk.ac.bath.masmusic.integration.ProtobufMqttMessageConverter;
import uk.ac.bath.masmusic.orchestra.mas.MasMusic;
import uk.ac.bath.masmusic.protobuf.Direction;
import uk.ac.bath.masmusic.protobuf.TimePointNote;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Conductor application.
 *
 * @author Javier Dehesa
 */
@SpringBootApplication
@IntegrationComponentScan
// @EnableIntegration // Is this necessary?
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
    @Value("${mqtt.direction.client.id}")
    private String mqttDirectionClientId;
    @Value("${mqtt.direction.topic}")
    private String mqttDirectionTopic;

    @Autowired
    private MasMusic masMusic;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String... args) {
        LOG.info("Orchestra started, press Ctrl+C to finish...");
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
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * @return MQTT message producer
     */
    @Bean
    public MessageProducerSupport mqttInput() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqttHearClientId, mqttClientFactory(), mqttHearTopic);
        adapter.setConverter(new ProtobufMqttMessageConverter(
                TimePointNote.class, mqttQos, mqttRetain));
        adapter.setQos(mqttQos);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * @return MQTT input flow
     */
    @Bean
    public IntegrationFlow mqttInputFlow() {
        return IntegrationFlows.from(mqttInputChannel()).handle(masMusic).get();
    }

    /**
     * @return MQTT message deliverer
     */
    @Bean
    public MessageHandler mqttOutput() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                mqttPlayClientId, mqttClientFactory());
        handler.setDefaultTopic(mqttPlayTopic);
        handler.setDefaultQos(mqttQos);
        handler.setConverter(new ProtobufMqttMessageConverter(
                TimeSpanNote.class, mqttQos, mqttRetain));
        return handler;
    }

    /**
     * @return MQTT output channel
     */
    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    /**
     * @return MQTT output flow
     */
    @Bean
    public IntegrationFlow mqttOutputFlow() {
        return IntegrationFlows.from(mqttOutputChannel()).handle(mqttOutput())
                .get();
    }

    /**
     * @return MQTT direction channel
     */
    @Bean
    public MessageChannel mqttDirectionChannel() {
        return new DirectChannel();
    }

    /**
     * @return MQTT message producer
     */
    @Bean
    public MessageProducerSupport mqttDirection() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqttDirectionClientId, mqttClientFactory(), mqttDirectionTopic);
        adapter.setConverter(new ProtobufMqttMessageConverter(
                Direction.class, mqttQos, mqttRetain));
        adapter.setQos(mqttQos);
        adapter.setOutputChannel(mqttDirectionChannel());
        return adapter;
    }

    /**
     * @return MQTT input flow
     */
    @Bean
    public IntegrationFlow mqttDirectionFlow() {
        return IntegrationFlows.from(mqttDirectionChannel()).handle(masMusic)
                .get();
    }

}
