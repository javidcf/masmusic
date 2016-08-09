
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */

+rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
    <- !metronomeBar(system.time, BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET).

+!metronomeBar(T, BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
    :  rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
    <- metronome(1, BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET);
       NEXT_BEAT = T + BEAT_DURATION;
       .wait(math.max(NEXT_BEAT - system.time - BEAT_DURATION * .5, 1));
       !!metronomeBar(NEXT_BEAT, BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET).

+!metronomeBar(_, _, _, _, _, _).
