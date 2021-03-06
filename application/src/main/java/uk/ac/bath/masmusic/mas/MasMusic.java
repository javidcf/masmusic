package uk.ac.bath.masmusic.mas;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.events.RhythmUpdatedEvent;
import uk.ac.bath.masmusic.events.ScaleUpdatedEvent;
import uk.ac.bath.masmusic.integration.MusicGateway;
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

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MasMusic.class);

    /** Default note velocity. */
    public static final int DEFAULT_VELOCITY = 64;

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

    /** The current rhythm. */
    private final AtomicReference<Rhythm> rhythm;

    /** The current scale. */
    private final AtomicReference<Scale> scale;

    /**
     * Constructor.
     */
    public MasMusic() {
        timeSpanNoteBuilder = TimeSpanNote.newBuilder();
        pitchBuilder = Pitch.newBuilder();
        started = new AtomicBoolean(false);
        finish = new AtomicBoolean(false);
        rhythm = new AtomicReference<>();
        scale = new AtomicReference<>();
    }

    /**
     * @return The current scale, of null if no scale has been set
     */
    public Scale getScale() {
        return scale.get();
    }

    /**
     * Handle a scale update event.
     *
     * @param event
     *            The scale update event
     */
    @EventListener
    public void onScaleUpdated(ScaleUpdatedEvent event) {
        scale.set(event.getScale());
    }

    /**
     * @return The current rhythm, of null if no rhythm has been set
     */
    public Rhythm getRhythm() {
        return rhythm.get();
    }

    /**
     * Handle a rhythm update event.
     *
     * @param event
     *            The rhythm update event
     */
    @EventListener
    public void onRhythmUpdated(RhythmUpdatedEvent event) {
        rhythm.set(event.getRhythm());
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
        if (!(payload instanceof TimeSpanNote)) {
            LOG.error("Unexpected message type");
            return;
        }
        TimeSpanNote timeSpanNote = (TimeSpanNote) payload;
        int octave = timeSpanNote.getPitch().getOctave();
        int baseNoteValue = timeSpanNote.getPitch().getNote().getNumber();
        int pitchValue = (octave + 1) * 12 + baseNoteValue;
        int velocity = timeSpanNote.getVelocity();
        long timestamp = timeSpanNote.getTimestamp();
        for (MasMusicAbstractAgent agent : agents) {
            agent.hear(pitchValue, velocity, timestamp);
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
     * @param instrument
     *            Instrument used to played the note (should be in the range
     *            0-127)
     */
    protected void play(int pitch, int velocity, long timestamp, int duration, int instrument) {
        if (instrument < 0 || instrument > 127) {
            throw new IllegalArgumentException("Invalid instrument value");
        }
        Note baseNote = Note.valueOf(pitch % 12);
        int octave = (pitch / 12) - 1;
        TimeSpanNote timeSpanNote = timeSpanNoteBuilder
                .setPitch(pitchBuilder
                        .setNote(baseNote)
                        .setOctave(octave))
                .setVelocity(velocity)
                .setTimestamp(timestamp)
                .setDuration(duration)
                .setInstrument(instrument)
                .build();
        musicPlayer.play(timeSpanNote);
    }
}
