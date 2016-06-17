
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */
// +hear(PITCH, VELOCITY) <- play(PITCH).

+rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
    <- metronome(16,
                 BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET).

+perform(PERFORM_START, PERFORM_BARS)
    :  rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
     & scale(FUNDAMENTAL, SCALE)
    <- compose(PERFORM_START, PERFORM_BARS,
               BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET,
               FUNDAMENTAL, SCALE).
