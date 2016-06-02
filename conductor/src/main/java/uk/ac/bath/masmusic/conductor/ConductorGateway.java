package uk.ac.bath.masmusic.conductor;

import org.springframework.integration.annotation.MessagingGateway;

import uk.ac.bath.masmusic.protobuf.Direction;

/**
 * Messaging gateway for {@link Direction}s.
 *
 * @author Javier Dehesa
 */
@MessagingGateway(defaultRequestChannel = "mqttDirectionChannel")
public interface ConductorGateway {

    void direct(Direction direction);

}
