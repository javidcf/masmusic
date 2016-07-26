
/* Initial beliefs and rules */

/* Initial goals */


/* Plans */

+!perform(PERFORM_START, PERFORM_BARS)
    <- ?rhythm(BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET);
       ?scale(FUNDAMENTAL, SCALE);
       compose(PERFORM_START, PERFORM_BARS,
               BEAT_DURATION, BEAT_PHASE, BAR_BEATS, BAR_UNIT, BAR_BEAT_OFFSET,
               FUNDAMENTAL, SCALE);
       !!programPerformance.

+hear(PITCH, VELOCITY)
    <- -+lastHeardNote(system.time);
       !programPerformance.

+!programPerformance
    : lastHeardNote(T)
    <- .wait(math.max(system.time + 2000 - T, 0));
       ?lastHeardNote(T);
       !perform(system.time, 1).

       // ?lastHeardNote(T);
       // .wait(math.max(system.time + 3000 - T, 0));
       // .at("now +3 s", {+!perform(T + 3000, 1)});
