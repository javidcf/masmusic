package uk.ac.bath.masmusic.integration;

import org.springframework.integration.annotation.MessagingGateway;

import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * Messaging gateway for {@link TimeSpanNote}s.
 *
 * @author Javier Dehesa
 */
@MessagingGateway(defaultRequestChannel = "playChannel")
public interface MusicGateway {

    void play(TimeSpanNote note);

}
