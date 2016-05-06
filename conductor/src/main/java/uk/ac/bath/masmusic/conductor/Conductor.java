package uk.ac.bath.masmusic.conductor;

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

import uk.ac.bath.masmusic.conductor.cep.EsperMessageHandler;
import uk.ac.bath.masmusic.integration.ProtobufBase64MqttMessageConverter;
import uk.ac.bath.masmusic.protobuf.TimePointNote;

@SpringBootApplication
// @EnableIntegration  // Is this necessary?
public class Conductor implements CommandLineRunner {

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
    @Value("${mqtt.listen.topic}")
    private String mqttListenTopic;

    @Autowired
    private EsperMessageHandler messageHandler;

    @Override
    public void run(String... args) {
        System.out.println("Hello");
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Conductor.class, args);
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttMessageConverter messageConverter() {
        // return new ProtobufMqttMessageConverter<TimePointNote>(
        //        TimePointNote.class, mqttQos, mqttRetain);
        return new ProtobufBase64MqttMessageConverter<TimePointNote>(
                TimePointNote.class, mqttQos, mqttRetain);
    }

    @Bean
    public MessageProducerSupport mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                mqttUrl, mqttClientId, mqttListenTopic);
        adapter.setConverter(messageConverter());
        adapter.setQos(mqttQos);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    public IntegrationFlow mqttInFlow() {
        IntegrationFlows.from(mqttInbound());
        return IntegrationFlows.from(mqttInbound()).handle(messageHandler)
                .get();
    }

}
