package uk.ac.bath.masmusic.mas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Structure;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Phrase;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.ScoreElement;
import uk.ac.bath.masmusic.common.TimeSignature;

/**
 * An agent that plays a randomly generated melody.
 *
 * @author Javier Dehesa
 */
@Component
public class MelodyGeneratorAgent extends MasMusicAbstractAgent {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(MelodyGeneratorAgent.class);

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/melodyGeneratorAgent.asl";

    @Autowired
    private MasMusic masMusic;

    @Autowired
    private MelodyGenerator melodyGenerator;

    public MelodyGeneratorAgent() {
        initAgent(ASL_PATH);
    }

    @Override
    protected boolean doAction(ActionExec action) {
        Structure actionTerm = action.getActionTerm();
        if (actionTerm.getFunctor().equalsIgnoreCase("play")) {
            int pitch = Integer.parseInt(actionTerm.getTerm(0).toString());
            long timestamp = System.currentTimeMillis();
            playNote(pitch, DEFAULT_VELOCITY, timestamp, DEFAULT_DURATION);
            return true;
        } else if (actionTerm.getFunctor().equalsIgnoreCase("compose")) {
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
            Phrase generated = melodyGenerator.generateMelody(scale, barBeats * bars);
            long baseTimestamp = rhythm.nextBar(start);
            int snapTolerance = Math.round(.125f * beat.getDuration());
            for (Phrase.Element phraseElement : generated) {
                double position = phraseElement.getPosition();
                ScoreElement scoreElement = phraseElement.getScoreElement();
                int elementDuration = (int) Math.round(scoreElement.getDuration() * beat.getDuration());
                long elementStart = baseTimestamp + Math.round(position * beat.getDuration());
                // Snap start to a beat if it is close
                long elementStartSnap = beat.closestBeat(elementStart);
                if (Math.abs(elementStartSnap - elementStart) < snapTolerance) {
                    elementStart = elementStartSnap;
                }
                for (int pitch : scoreElement.getPitches()) {
                    masMusic.play(pitch, MasMusic.DEFAULT_VELOCITY, elementStart, elementDuration);
                }
            }
            return true;
        } else {
            return false;
        }
    }

}
