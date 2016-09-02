
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */

+!perform(PERFORM_START, PERFORM_BARS, HEAR_TIMESTAMP)
    :  lastHeardNote(HEAR_TIMESTAMP)
     & rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET)
     & scale(FUNDAMENTAL, SCALE)
    <- imitate(PERFORM_START, PERFORM_BARS,
               BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET,
               FUNDAMENTAL, SCALE);
       WAIT = BEAT_DURATION * BAR_BEATS * PERFORM_BARS;
       .wait(math.max(PERFORM_START + WAIT - system.time, 1));
       !!perform(PERFORM_START + WAIT, PERFORM_BARS, HEAR_TIMESTAMP).

// No-op
+!perform(_, _, _).

+hear(PITCH, VELOCITY)
    <- T = system.time;
       -+lastHeardNote(T);
       !programPerformance(T).

+!programPerformance(HEAR_TIMESTAMP)
    : lastHeardNote(HEAR_TIMESTAMP)
    <- .wait(math.max(HEAR_TIMESTAMP + 2000 - system.time, 1));
       !!perform(system.time, 1, HEAR_TIMESTAMP).

// No-op
+!programPerformance(_).      
