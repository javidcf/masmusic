package uk.ac.bath.masmusic.orchestra.mas;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.orchestra.MusicGateway;
import uk.ac.bath.masmusic.protobuf.Note;
import uk.ac.bath.masmusic.protobuf.Pitch;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * MasMusic orchestra multi-agent system.
 *
 * @author Javier Dehesa
 */
@Component
public class MasMusic implements MessageHandler, Runnable {

    /** MasMusic agents. */
    @Autowired
    private List<MasMusicAbstractAgent> agents;

    /** Output message channel. */
    @Autowired
    private MusicGateway musicPlayer;

    /** Builder for {@link TimeSpanNote} objects. */
    private final TimeSpanNote.Builder timeSpanNoteBuilder;

    /** Builder for {@link Pitch} objects. */
    private final Pitch.Builder pitchBuilder;

    /** Whether the system is running. */
    private final AtomicBoolean started;

    /** Whether the system must finish. */
    private final AtomicBoolean finish;

    /**
     * Constructor.
     */
    public MasMusic() {
        timeSpanNoteBuilder = TimeSpanNote.newBuilder();
        pitchBuilder = Pitch.newBuilder();
        started = new AtomicBoolean(false);
        finish = new AtomicBoolean(false);
    }

    /**
     * Start the system.
     */
    @PostConstruct
    public void start() {
        if (!started.getAndSet(true)) {
            finish.set(false);
            new Thread(this).start();
        }
    }

    /**
     * Stop the system.
     */
    public void stop() {
        finish.set(true);
    }

    @Override
    public void run() {
        while (!finish.get()) {
            reason();
        }
        started.set(false);
    }

    /**
     * Perform a reasoning cycle of the agents.
     */
    public void reason() {
        for (MasMusicAbstractAgent agent : agents) {
            agent.reason();
        }
    }

    private static boolean first = true;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        // TEST
        for (MasMusicAbstractAgent agent : agents) {
            if (first) {
                agent.setBeat(500, 0);  // 120 bpm
                agent.setScale("c", "major");
                agent.perform(System.currentTimeMillis() + 500, 20000);
                first = false;
            }
        }
        // TEST

        /*
        Object payload = message.getPayload();
        if (!(payload instanceof TimePointNote)) {
            return;
        }
        TimePointNote note = (TimePointNote) payload;
        int octave = note.getPitch().getOctave();
        int baseNoteValue = note.getPitch().getNote().getNumber();
        int pitchValue = (octave + 1) * 12 + baseNoteValue;
        int velocity = note.getVelocity();
        long timestamp = note.getTimestamp();
        for (MasMusicAbstractAgent agent : agents) {
            agent.hear(pitchValue, velocity, timestamp);
        }
        */
    }

    /**
     * @param pitch
     *            Pitch of the played note
     * @param velocity
     *            Velocity of the played note
     * @param timestamp
     *            Start time of the played note
     * @param duration
     *            Duration of the played note
     */
    protected void play(int pitch, int velocity, long timestamp, int duration) {
        Note baseNote = Note.valueOf(pitch % 12);
        int octave = (pitch / 12) - 1;
        TimeSpanNote timeSpanNote = timeSpanNoteBuilder
                .setPitch(pitchBuilder.setNote(baseNote).setOctave(octave))
                .setVelocity(velocity).setTimestamp(timestamp)
                .setDuration(duration).build();
        musicPlayer.play(timeSpanNote);
    }
}
