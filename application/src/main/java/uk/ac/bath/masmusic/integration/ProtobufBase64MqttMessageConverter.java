package uk.ac.bath.masmusic.integration;

import java.lang.reflect.Method;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.util.Base64Utils;

import com.google.protobuf.MessageLite;

/**
 * MQTT message converter for Base64-encoded Protocol Buffers messages.
 *
 * @author Javier Dehesa
 *
 * @param <E>
 *            Protocol Buffers message type
 */
public class ProtobufBase64MqttMessageConverter extends DefaultPahoMessageConverter {

    /** The Protocol Buffers message class */
    private Class<? extends MessageLite> messageClass;

    /** The byte array parsing method */
    private Method parseMethod;

    /**
     * Construct a converter with default settings.
     *
     * @param messageClass
     *            The Protocol Buffers message class
     *
     * @see DefaultPahoMessageConverter#DefaultPahoMessageConverter()
     */
    public ProtobufBase64MqttMessageConverter(Class<? extends MessageLite> messageClass) {
        this(messageClass, 0, false);
    }

    /**
     * Construct a converter with the given default QoS and retain policy and
     * default charset.
     *
     * @see DefaultPahoMessageConverter#DefaultPahoMessageConverter(int,
     *      boolean)
     *
     * @param messageClass
     *            The Protocol Buffers message class
     * @param defaultQos
     *            Default QoS
     * @param defaultRetain
     *            Default retain policy
     */
    public ProtobufBase64MqttMessageConverter(Class<? extends MessageLite> messageClass,
            int defaultQos, boolean defaultRetain) {
        this(messageClass, defaultQos, defaultRetain, "UTF-8");
    }

    /**
     * Construct a converter with the given charset and the default QoS and
     * retain policy settings.
     *
     * @see DefaultPahoMessageConverter#DefaultPahoMessageConverter(String)
     *
     * @param messageClass
     *            The Protocol Buffers message class
     * @param charset
     *            The charset used in the conversion
     */
    public ProtobufBase64MqttMessageConverter(Class<? extends MessageLite> messageClass,
            String charset) {
        this(messageClass, 0, false, charset);
    }

    /**
     * Construct a converter with the given default QoS retain policy, and
     * charset.
     *
     * @see DefaultPahoMessageConverter#DefaultPahoMessageConverter(int,
     *      boolean, String)
     *
     * @param messageClass
     *            The Protocol Buffers message class
     * @param defaultQos
     *            Default QoS
     * @param defaultRetain
     *            Default retain policy
     * @param charset
     *            The charset used in the conversion
     */
    public ProtobufBase64MqttMessageConverter(Class<? extends MessageLite> messageClass,
            int defaultQos, boolean defaultRetained, String charset) {
        super(defaultQos, defaultRetained, charset);
        this.messageClass = messageClass;
        try {
            this.parseMethod = this.messageClass.getMethod("parseFrom",
                    byte[].class);
        } catch (NoSuchMethodException e) {
            assert false : "Invalid Protocol Buffers message class";
        } catch (SecurityException e) {
            assert false : "Invalid Protocol Buffers message class";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MessageLite mqttBytesToPayload(MqttMessage mqttMessage) throws Exception {
        byte[] decodedPayload = Base64Utils.decode(mqttMessage.getPayload());
        Object obj = parseMethod.invoke(null, decodedPayload);
        return messageClass.cast(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte[] messageToMqttBytes(Message<?> message) {
        byte[] encodedPayload = Base64Utils
                .encode(((MessageLite) message.getPayload()).toByteArray());
        return encodedPayload;
    }

}
