
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */

+!perform(PERFORM_START, PERFORM_BARS, HEAR_TIMESTAMP)
   : lastHeardNote(HEAR_TIMESTAMP)
    <- ?rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET);
       ?scale(FUNDAMENTAL, SCALE);
       compose(PERFORM_START, PERFORM_BARS,
               BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET,
               FUNDAMENTAL, SCALE);
       .wait(BEAT_DURATION * BAR_BEATS * PERFORM_BARS);
       !!perform(system.time, PERFORM_BARS, HEAR_TIMESTAMP).

// No-op
+!perform(_, _, _).       
       

+hear(PITCH, VELOCITY)
    <- T = system.time;
       -+lastHeardNote(T);
       !programPerformance(T).

+!programPerformance(HEAR_TIMESTAMP)
    : lastHeardNote(HEAR_TIMESTAMP)
    <- .wait(math.max(system.time + 2000 - HEAR_TIMESTAMP, 0));
       !!perform(system.time, 16, HEAR_TIMESTAMP).

// No-op
+!programPerformance(_).      
