
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */
// +hear(PITCH, VELOCITY) <- play(PITCH).

+perform(PERFORM_START, PERFORM_DURATION)
    :  beat(BEAT_DURATION, BEAT_PHASE) & scale(FUNDAMENTAL, SCALE)
    <- compose(PERFORM_START, PERFORM_DURATION,
               BEAT_DURATION, BEAT_PHASE,
               FUNDAMENTAL, SCALE).

