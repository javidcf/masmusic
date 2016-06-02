package uk.ac.bath.masmusic.orchestra;

import org.springframework.integration.annotation.MessagingGateway;

import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Messaging gateway for {@link TimeSpanNote}s.
 *
 * @author Javier Dehesa
 */
@MessagingGateway(defaultRequestChannel = "mqttOutputChannel")
public interface MusicGateway {

    void play(TimeSpanNote note);

}
