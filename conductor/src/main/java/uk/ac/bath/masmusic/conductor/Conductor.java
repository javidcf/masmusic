package uk.ac.bath.masmusic.conductor;

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

import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.TimeSignature;
import uk.ac.bath.masmusic.conductor.analysis.ScaleInducer;
import uk.ac.bath.masmusic.conductor.analysis.beatroot.BeatRoot;
import uk.ac.bath.masmusic.conductor.cep.EsperMessageHandler;
import uk.ac.bath.masmusic.conductor.cep.RhythmDetector;
import uk.ac.bath.masmusic.conductor.cep.ScaleTracker;
import uk.ac.bath.masmusic.integration.ProtobufMqttMessageConverter;
import uk.ac.bath.masmusic.protobuf.Direction;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Conductor application.
 *
 * @author Javier Dehesa
 */
@SpringBootApplication
@IntegrationComponentScan
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
    @Value("${mqtt.hear.client.id}")
    private String mqttHearClientId;
    @Value("${mqtt.hear.topic}")
    private String mqttHearTopic;
    @Value("${mqtt.direction.client.id}")
    private String mqttDirectionClientId;
    @Value("${mqtt.direction.topic}")
    private String mqttDirectionTopic;

    @Value("${beat.tempo.min}")
    private int minTempo;
    @Value("${beat.tempo.max}")
    private int maxTempo;

    @Autowired
    private EsperMessageHandler messageHandler;

    @Autowired
    private RhythmDetector rhythmDetector;

    @Autowired
    private ScaleTracker scaleTracker;

    @Autowired
    private ConductorGateway conductorGateway;

    /**
     * Builder of {@link Direction} messages.
     */
    private final Direction.Builder directionBuilder = Direction.newBuilder();

    /**
     * Builder of {@link Scale} messages.
     */
    private final uk.ac.bath.masmusic.protobuf.Scale.Builder scaleBuilder = uk.ac.bath.masmusic.protobuf.Scale
            .newBuilder();

    /**
     * Builder of {@link Beat} messages.
     */
    private final uk.ac.bath.masmusic.protobuf.Beat.Builder beatBuilder = uk.ac.bath.masmusic.protobuf.Beat
            .newBuilder();

    /**
     * Builder of {@link TimeSignature} messages.
     */
    private final uk.ac.bath.masmusic.protobuf.TimeSignature.Builder timeSignatureBuilder = uk.ac.bath.masmusic.protobuf.TimeSignature
            .newBuilder();

    /**
     * Builder of {@link Rhythm} messages.
     */
    private final uk.ac.bath.masmusic.protobuf.Rhythm.Builder rhythmBuilder = uk.ac.bath.masmusic.protobuf.Rhythm
            .newBuilder();

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
     * Send directions with the most recent information.
     */
    public synchronized void conduct() {
        Rhythm rhythm = rhythmDetector.getDetectedRhtyhm();
        Scale scale = scaleTracker.getCurrentScale();
        if (rhythm != null) {
            Beat beat = rhythm.getBeat();
            TimeSignature timeSignature = rhythm.getTimeSignature();
            directionBuilder.setRhythm(rhythmBuilder
                    .setBeat(beatBuilder
                            .setDuration(beat.getDuration())
                            .setPhase(beat.getPhase()))
                    .setTimeSignature(timeSignatureBuilder
                            .setBeats(timeSignature.getBeats())
                            .setUnit(timeSignature.getUnit()))
                    .setBeatOffset(rhythm.getBeatOffset()));
        } else {
            directionBuilder.clearRhythm();
        }
        if (scale != null) {
            uk.ac.bath.masmusic.protobuf.Note fundamental = uk.ac.bath.masmusic.protobuf.Note
                    .valueOf(scale.getFundamental().value());
            directionBuilder.setScale(
                    scaleBuilder.setFundamental(fundamental)
                            .setType(scale.getName()));
        } else {
            directionBuilder.clearScale();
        }
        conductorGateway.direct(directionBuilder.build());
    }

    /**
     * @return The BeatRoot beat tracker.
     */
    @Bean
    public BeatRoot beatRoot() {
        return new BeatRoot(minTempo, maxTempo);
    }

    /**
     * @return The scale inducer
     */
    @Bean
    public ScaleInducer scaleInducer() {
        return new ScaleInducer();
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
                TimeSpanNote.class, mqttQos, mqttRetain));
        adapter.setQos(mqttQos);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * @return MQTT input flow
     */
    @Bean
    public IntegrationFlow mqttInputFlow() {
        IntegrationFlows.from(mqttInput());
        return IntegrationFlows.from(mqttInput()).handle(messageHandler).get();
    }

    /**
     * @return MQTT direction channel
     */
    @Bean
    public MessageChannel mqttDirectionChannel() {
        return new DirectChannel();
    }

    /**
     * @return MQTT direction message deliverer
     */
    @Bean
    public MessageHandler mqttDirection() {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                mqttDirectionClientId, mqttClientFactory());
        handler.setDefaultTopic(mqttDirectionTopic);
        handler.setDefaultQos(mqttQos);
        handler.setConverter(new ProtobufMqttMessageConverter(
                Direction.class, mqttQos, mqttRetain));
        return handler;
    }

    @Bean
    public IntegrationFlow mqttDirectionFlow() {
        return IntegrationFlows.from(mqttDirectionChannel())
                .handle(mqttDirection()).get();
    }

}
