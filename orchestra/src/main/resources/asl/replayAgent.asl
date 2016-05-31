
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */
// +hear(PITCH, VELOCITY) <- play(PITCH).

+perform(T_START, T_END) : beat(BEAT_DURATION, BEAT_PHASE)?, scale(FUNDAMENTAL, SCALE) <-
    compose(T_START, T_END, BEAT_DURATION, BEAT_PHASE, FUNDAMENTAL, SCALE).
