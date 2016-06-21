package uk.ac.bath.masmusic.orchestra.mas;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jason.asSemantics.ActionExec;
import jason.asSyntax.Structure;
import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.Scale;
import uk.ac.bath.masmusic.common.TimeSignature;

/**
 * An agent that replays the received notes.
 *
 * @author Javier Dehesa
 */
@Component
public class ReplayAgent extends MasMusicAbstractAgent {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(ReplayAgent.class);

    /** Agent ASL source path. */
    private static final String ASL_PATH = "/asl/replayAgent.asl";

    /** Random number generator. */
    private Random random = new Random();

    public ReplayAgent() {
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
            long start = Long.parseLong(actionTerm.getTerm(0).toString());
            int bars = Integer.parseInt(actionTerm.getTerm(1).toString());
            int beatDuration = Integer
                    .parseInt(actionTerm.getTerm(2).toString());
            int beatPhase = Integer.parseInt(actionTerm.getTerm(3).toString());
            int barBeats = Integer.parseInt(actionTerm.getTerm(4).toString());
            int barUnit = Integer.parseInt(actionTerm.getTerm(5).toString());
            int barBeatOffset = Integer
                    .parseInt(actionTerm.getTerm(6).toString());
            int fundamental = Integer
                    .parseInt(actionTerm.getTerm(7).toString());
            String scaleName = actionTerm.getTerm(8).toString();
            Rhythm rhythm = new Rhythm(new Beat(beatDuration, beatPhase),
                    new TimeSignature(barBeats, barUnit), barBeatOffset);
            Scale scale = new Scale(Note.fromValue(fundamental), scaleName);
            compose(start, bars, rhythm, scale);
            // composeBasicRock(start, bars, rhythm, scale);
            return true;
        } else {
            return false;
        }
    }

    private void compose(long start, int bars, Rhythm rhythm, Scale scale) {
        // Come up with some random melody
        Beat beat = rhythm.getBeat();
        long currentBeat = rhythm.nextBar(start);
        long lastBeat = rhythm.nextBar(start, bars);
        long currentTimestamp = currentBeat;
        // Middle fundamental
        int basePitch = 60 + scale.getFundamental().value();
        int currentDegree = 0;
        // Generate notes with random rhythmic patterns
        while (currentBeat < lastBeat) {
            final int DIVISIONS = 1;
            final int SLOTS = 1 << DIVISIONS;
            int filled = 0;
            while (filled < SLOTS) {
                // Choose note duration
                int durationSlotsExp = random
                        .nextInt(log2Int(SLOTS - filled) + 1);
                int durationSlots = 1 << durationSlotsExp;
                int noteDuration = (beat.getDuration() * durationSlots) / SLOTS;
                filled += durationSlots;

                // Choose note pitch
                int degreeStep = (random.nextBoolean() ? 1 : -1)
                        * (random.nextInt(3) + 1);
                currentDegree += degreeStep;
                // Change direction if going too far
                if ((currentDegree < 0 && basePitch <= 36)
                        || (currentDegree >= scale.size() && basePitch >= 72)) {
                    currentDegree -= degreeStep * 2;
                }
                // Adjust current octave
                while (currentDegree < 0) {
                    currentDegree += scale.size();
                    basePitch -= 12;
                }
                while (currentDegree >= scale.size()) {
                    currentDegree -= scale.size();
                    basePitch += 12;
                }
                int pitch = basePitch + scale.getInterval(currentDegree);

                // Play note
                playNote(pitch, DEFAULT_VELOCITY, currentTimestamp,
                        noteDuration);
                currentTimestamp += noteDuration;
            }
            currentBeat = beat.nextBeat(currentBeat);
            currentTimestamp = currentBeat;
        }
    }

    private static int log2Int(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.SIZE - 1 - Integer.numberOfLeadingZeros(value);
    }

    private void composeBasicRock(long start, int bars, Rhythm rhythm,
            Scale scale) {
        long currentBar = rhythm.nextBar(start);
        int beats = rhythm.getTimeSignature().getBeats();
        int beatDuration = rhythm.getBeat().getDuration();
        int basePitch = 48 + scale.getFundamental().value();
        int step = 0;
        for (int iBar = 0; iBar < bars; iBar++) {
            switch (step) {
            case 0:
            case 1:
            case 3:
            case 6:
            case 7:
                playArpeggiated7thBar(basePitch, currentBar, beats,
                        beatDuration);
                break;
            case 2:
            case 5:
                playArpeggiated7thBar(basePitch + 5, currentBar, beats,
                        beatDuration);
                break;
            case 4:
                playArpeggiated7thBar(basePitch + 7, currentBar, beats,
                        beatDuration);
                break;
            }
            step = (step + 1) % 8;
            currentBar = rhythm.nextBar(currentBar);
        }
    }

    private void playArpeggiated7thBar(int basePitch, long timestamp, int beats,
            int beatDuration) {
        int semiBeatDuration = beatDuration / 2;
        int thirdBeatDuration = beatDuration / 3;
        int quarterBeatDuration = beatDuration / 4;
        if (beats % 4 == 0) {
            for (int iBeat = 0; iBeat < beats; iBeat += 4) {
                playNote(basePitch, DEFAULT_VELOCITY, timestamp,
                        semiBeatDuration);
                timestamp += semiBeatDuration;
                playNote(basePitch + 4, DEFAULT_VELOCITY, timestamp,
                        semiBeatDuration);
                timestamp += semiBeatDuration;
                playNote(basePitch + 7, DEFAULT_VELOCITY, timestamp,
                        semiBeatDuration);
                timestamp += semiBeatDuration;
                playNote(basePitch + 9, DEFAULT_VELOCITY, timestamp,
                        quarterBeatDuration);
                timestamp += quarterBeatDuration;
                playNote(basePitch + 10, DEFAULT_VELOCITY, timestamp,
                        semiBeatDuration + quarterBeatDuration);
                timestamp += semiBeatDuration + quarterBeatDuration;
                playNote(basePitch + 9, DEFAULT_VELOCITY, timestamp,
                        semiBeatDuration);
                timestamp += semiBeatDuration;
                playNote(basePitch + 7, DEFAULT_VELOCITY, timestamp,
                        semiBeatDuration);
                timestamp += semiBeatDuration;
                playNote(basePitch + 9, DEFAULT_VELOCITY, timestamp,
                        semiBeatDuration);
                timestamp += semiBeatDuration;
            }
        } else if (beats % 3 == 0) {
            for (int iBeat = 0; iBeat < beats; iBeat += 3) {
                playNote(basePitch, DEFAULT_VELOCITY, timestamp,
                        2 * thirdBeatDuration);
                timestamp += 2 * thirdBeatDuration;
                playNote(basePitch + 7, DEFAULT_VELOCITY, timestamp,
                        thirdBeatDuration);
                timestamp += thirdBeatDuration;
                playNote(basePitch + 9, DEFAULT_VELOCITY, timestamp,
                        2 * thirdBeatDuration);
                timestamp += 2 * thirdBeatDuration;
                playNote(basePitch + 10, DEFAULT_VELOCITY, timestamp,
                        thirdBeatDuration);
                timestamp += thirdBeatDuration;
                playNote(basePitch + 9, DEFAULT_VELOCITY, timestamp,
                        2 * thirdBeatDuration);
                timestamp += 2 * thirdBeatDuration;
                playNote(basePitch + 7, DEFAULT_VELOCITY, timestamp,
                        thirdBeatDuration);
                timestamp += thirdBeatDuration;
            }
        } else {
            int iBeat = 0;
            while (iBeat < beats) {
                playNote(basePitch, DEFAULT_VELOCITY, timestamp,
                        2 * thirdBeatDuration);
                timestamp += 2 * thirdBeatDuration;
                playNote(basePitch + 7, DEFAULT_VELOCITY, timestamp,
                        thirdBeatDuration);
                timestamp += thirdBeatDuration;
                iBeat++;
                if (iBeat < beats) {
                    playNote(basePitch + 9, DEFAULT_VELOCITY, timestamp,
                            2 * thirdBeatDuration);
                    timestamp += 2 * thirdBeatDuration;
                    playNote(basePitch + 10, DEFAULT_VELOCITY, timestamp,
                            thirdBeatDuration);
                    timestamp += thirdBeatDuration;
                    iBeat++;
                }
            }
        }
    }

}
