package uk.ac.bath.masmusic.cep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.espertech.esper.client.EPServiceProvider;

/**
 * Message handler that publishes the received message to the Esper runtime.
 *
 * @author Javier Dehesa
 */
@Component
public class EsperMessageHandler implements MessageHandler {

    @Autowired
    private EPServiceProvider epService;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        Assert.notNull(message, "Message must not be null");
        Object payload = message.getPayload();
        Assert.notNull(payload, "Message payload must not be null");
        epService.getEPRuntime().sendEvent(payload);
    }

}
