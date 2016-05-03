package uk.ac.bath.masmusic;

import java.io.IOException;

import edu.bath.sensorframework.ProtobufDataReading;
import edu.bath.sensorframework.sensor.Sensor;
import jm.music.data.Note;

public class MusicPlayer extends Sensor {

    private static final int QOS = 0;

    private Pitch.Builder pitchBuilder;
    private TimePointNote.Builder noteBuilder;

    public MusicPlayer(String serverAddress, String clientName, String topic) {
        super(serverAddress, clientName, "", topic, true, QOS);
        pitchBuilder = Pitch.newBuilder();
        noteBuilder = TimePointNote.newBuilder();
    }

    public void publishMusic(Note note) {
        publishMusic(note, MasMusic.DEFAULT_VELOCITY,
                System.currentTimeMillis());
    }

    public void publishMusic(Note note, int velocity) {
        publishMusic(note, velocity, System.currentTimeMillis());
    }

    public void publishMusic(Note note, int velocity, long timestamp) {
        uk.ac.bath.masmusic.Note baseNote = uk.ac.bath.masmusic.Note
                .valueOf(note.getPitch() % 12);
        int octave = (note.getPitch() / 12) - 1;
        TimePointNote timePointNote = noteBuilder
                .setPitch(pitchBuilder
                        .setNote(baseNote)
                        .setOctave(octave))
                .setVelocity(velocity)
                .setTimestamp(timestamp)
                .build();
        try {
            publish(new ProtobufDataReading<TimePointNote>(timePointNote));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
