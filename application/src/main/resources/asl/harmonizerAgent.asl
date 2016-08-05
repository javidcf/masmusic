
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */

+harmonizationAvailable
    <- !perform(system.time, 1).

+!perform(PERFORM_START, PERFORM_BARS)
    :  harmonizationAvailable
     & rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
     & scale(FUNDAMENTAL, SCALE)
    <- harmonize(PERFORM_START, PERFORM_BARS,
                 BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET,
                 FUNDAMENTAL, SCALE);
       WAIT = BEAT_DURATION * BAR_BEATS * PERFORM_BARS;
       .wait(math.max(PERFORM_START + WAIT - system.time, 1));
       !!perform(PERFORM_START + WAIT, PERFORM_BARS).

// No-op
+!perform(_, _, _).       
