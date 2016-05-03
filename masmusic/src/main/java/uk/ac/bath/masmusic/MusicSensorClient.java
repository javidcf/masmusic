package uk.ac.bath.masmusic;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import edu.bath.sensorframework.client.ReadingHandler;
import edu.bath.sensorframework.client.SensorClient;
import edu.bath.sensorframework.client.SensorMQTTClient;
import jason.asSyntax.Literal;
import jm.music.data.Note;

/**
 * A sensor client that receives music events from a MQTT BSF network.
 *
 * @author Javier Dehesa
 *
 */
public class MusicSensorClient {

    /** BSF sensor client. */
    private final SensorClient sensorClient;
    /** Perceivers that receive the percepted events. */
    private final List<Perceiver> perceivers;

    /**
     * Constructor.
     *
     * @param serverAddress
     *            MQTT server address
     * @param clientName
     *            MQTT client name
     * @param topic
     *            MQTT topic
     */
    public MusicSensorClient(String serverAddress, String clientName,
            String topic) {
        sensorClient = new SensorMQTTClient(serverAddress, clientName);
        perceivers = new ArrayList<Perceiver>();
        sensorClient.addHandler(topic, new ReadingHandler() {
            public void handleIncomingReading(String topic, String data) {
                try {
                    // TODO Base64 decode
                    byte[] decoded = Base64.decodeBase64(data);
                    TimePointNote message = TimePointNote.parseFrom(decoded);

                    int octave = message.getPitch().getOctave();
                    int baseNoteValue = message.getPitch().getNote()
                            .getNumber();
                    int pitchValue = (octave + 1) * 12 + baseNoteValue;
                    int velocity = message.getVelocity();

                    Literal literal = Literal.parseLiteral(
                            String.format("%s(%d, %d)", MasMusic.HEAR_EVENT,
                                    pitchValue, velocity));

                    for (Perceiver perceiver : perceivers) {
                        perceiver.addPercept(literal);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        try {
            sensorClient.subscribe(topic);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Add a new perceiver to recevie music events.
     *
     * @param perceiver
     *            New perceiver
     */
    public void addPerceiver(Perceiver perceiver) {
        perceivers.add(perceiver);
    }

    /**
     * Transform a pitch value into a jMusic note.
     *
     * If the pitch is out of the MIDI range then the closest unison is
     * returned.
     *
     * @param pitch
     *            Pitch to convert
     * @return The converted Note value
     */
    public static Note pitchToNote(Pitch pitch) {
        int semitone = pitch.getNote().getNumber();
        int octave = Math.min(Math.max(pitch.getOctave(), -1), 9);
        int pitchValue = semitone + 12 * (octave + 1);
        if (pitchValue > 127) {
            pitchValue -= 12;
        }
        return new Note(pitchValue, 1);
    }

}
