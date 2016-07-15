
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */

+rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
    <- !metronomeBar(system.time).

+!metronomeBar(T)
    :  rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
    <- .wait(math.max(T - system.time - 100, 0));
       metronome(1, BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET);
       !!metronomeBar(T + (BEAT_DURATION * BAR_BEATS)).
