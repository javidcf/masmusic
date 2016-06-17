
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */
+hear(PITCH, VELOCITY) <- play(PITCH).

+perform(PERFORM_START, PERFORM_DURATION)
    :  rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
     & scale(FUNDAMENTAL, SCALE)
    <- compose(PERFORM_START, PERFORM_DURATION,
               BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET,
               FUNDAMENTAL, SCALE).

