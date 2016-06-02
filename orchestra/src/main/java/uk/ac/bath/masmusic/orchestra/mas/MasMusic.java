package uk.ac.bath.masmusic.orchestra.mas;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.orchestra.MusicGateway;
import uk.ac.bath.masmusic.protobuf.Beat;
import uk.ac.bath.masmusic.protobuf.Direction;
import uk.ac.bath.masmusic.protobuf.Note;
import uk.ac.bath.masmusic.protobuf.Pitch;
import uk.ac.bath.masmusic.protobuf.Scale;
import uk.ac.bath.masmusic.protobuf.TimePointNote;
import uk.ac.bath.masmusic.protobuf.TimeSpanNote;

/**
 * MasMusic orchestra multi-agent system.
 *
 * @author Javier Dehesa
 */
@Component
public class MasMusic implements MessageHandler, Runnable {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MasMusic.class);

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

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        Object payload = message.getPayload();
        if (payload instanceof TimePointNote) {
            handleNoteMessage((TimePointNote) payload);
        } else if (payload instanceof Direction) {
            handleDirectionMessage((Direction) payload);
        }

    }

    /**
     * Handle a new note message.
     *
     * @param note
     *            Received note message
     */
    private void handleNoteMessage(TimePointNote note) {
        int octave = note.getPitch().getOctave();
        int baseNoteValue = note.getPitch().getNote().getNumber();
        int pitchValue = (octave +
                1) * 12 + baseNoteValue;
        int velocity = note.getVelocity();
        long timestamp = note.getTimestamp();
        for (MasMusicAbstractAgent agent : agents) {
            agent.hear(pitchValue, velocity, timestamp);
        }

        /// TEST
        for (MasMusicAbstractAgent agent : agents) {
            agent.perform(System.currentTimeMillis(), 1000);
        }
        /// TEST
    }

    /**
     * Handle a new direction message.
     *
     * @param direction
     *            Received direction message
     */
    private void handleDirectionMessage(Direction direction) {
        Beat beat = direction.getBeat();
        Scale scale = direction.getScale();
        for (MasMusicAbstractAgent agent : agents) {
            if (beat.isInitialized()) {
                agent.setBeat(beat.getDuration(), beat.getPhase());
            }
            if (scale.isInitialized()) {
                agent.setScale(scale.getFundamental().getNumber(), scale.getType());
            }
        }
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
