package uk.ac.bath.masmusic.mas;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Structure;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.TimeSignature;
import uk.ac.bath.masmusic.events.MusicInputBufferUpdatedEvent;

/**
 * An agent that imitates the received melody.
 *
 * @author Javier Dehesa
 */
//@Component
public class MelodyCopycatAgent extends MasMusicAbstractAgent {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MelodyCopycatAgent.class);

    /** Instrument. */
    private static final int INSTRUMENT = 110;

    /** Velocity. */
    private static final int VELOCITY = 70;

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/melodyCopycatAgent.asl";

    @Autowired
    private MasMusic masMusic;

    @Autowired
    private MelodyCopycat melodyCopycat;

    public MelodyCopycatAgent() {
        initAgent(ASL_PATH);
    }

    @Override
    protected boolean doAction(ActionExec action) {
        Structure actionTerm = action.getActionTerm();
        if (actionTerm.getFunctor().equalsIgnoreCase("play")) {
            int pitch = Integer.parseInt(actionTerm.getTerm(0).toString());
            long timestamp = System.currentTimeMillis();
            playNote(pitch, DEFAULT_VELOCITY, timestamp, DEFAULT_DURATION, INSTRUMENT);
            return true;
        } else if (actionTerm.getFunctor().equalsIgnoreCase("imitate")) {
            // Read parameters
            long start = Long.parseLong(actionTerm.getTerm(0).toString());
            int bars = Integer.parseInt(actionTerm.getTerm(1).toString());
            int beatDuration = Integer.parseInt(actionTerm.getTerm(2).toString());
            int beatPhase = Integer.parseInt(actionTerm.getTerm(3).toString());
            int barBeats = Integer.parseInt(actionTerm.getTerm(4).toString());
            int barUnit = Integer.parseInt(actionTerm.getTerm(5).toString());
            int barBeatOffset = Integer.parseInt(actionTerm.getTerm(6).toString());
            int fundamental = Integer.parseInt(actionTerm.getTerm(7).toString());
            String scaleName = actionTerm.getTerm(8).toString();
            Beat beat = new Beat(beatDuration, beatPhase);
            TimeSignature timeSignature = new TimeSignature(barBeats, barUnit);
            Rhythm rhythm = new Rhythm(beat, timeSignature, barBeatOffset);
            Scale scale = new Scale(Note.fromValue(fundamental), scaleName);

            // Generate melody and play it
            List<Onset> harmony = melodyCopycat.getRandomBars(scale, rhythm, start, bars);
            for (Onset onset : harmony) {
                playNote(onset.getPitch(), VELOCITY, onset.getTimestamp(), onset.getDuration(), INSTRUMENT);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Handle an input update event.
     *
     * @param event
     *            The input update event
     */
    @EventListener
    public void onInputUpdated(MusicInputBufferUpdatedEvent event) {
        Scale scale = masMusic.getScale();
        Rhythm rhythm = masMusic.getRhythm();
        if (scale != null && rhythm != null) {
            melodyCopycat.learn(scale, rhythm, event.getInputBuffer());
        }
    }

}
