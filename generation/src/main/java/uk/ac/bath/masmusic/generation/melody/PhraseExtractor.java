package uk.ac.bath.masmusic.generation.melody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.bath.masmusic.common.Beat;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Phrase;
import uk.ac.bath.masmusic.common.Rhythm;
import uk.ac.bath.masmusic.common.ScoreElement;

/**
 * Extracts {@link Phrase}s from sequences of {@link Onset}s.
 *
 * @author Javier Dehesa
 *
 */
public class PhraseExtractor {

    /** Allowed beat subdivision level for snapping onsets. */
    private final static int SUBDIVISIONS = 2;

    /**
     * Position distance to consider two elements to belong to a different
     * phrase.
     */
    private static final float PHRASE_POSITION_DISTANCE = 1f;

    /**
     * Pitch distance to consider two elements to belong to a different phrase.
     */
    private static final int PHRASE_PITCH_DISTANCE = 12;

    /** Allowed error for position matching. */
    private static final float POSITION_EPS = 1f / (1 << (SUBDIVISIONS + 2));

    /** Treble/bass splitter. */
    private final TrebleBassSplitter splitter = new TrebleBassSplitter();

    /**
     * Extract phrases from a sequence of onsets.
     *
     * @param onsets
     *            A sequence of onsets
     * @param rhythm
     *            Rhythm at which the onsets were played
     * @return Extracted phrases
     */
    public List<Phrase> extractPhrases(List<Onset> onsets, Rhythm rhythm) {
        // Snap onsets to the detected rhythm and sort
        List<Onset> onsetsSnap = new ArrayList<>(onsets.size());
        for (Onset onset : onsets) {
            Onset snapped = snapOnsetToBeat(onset, rhythm.getBeat(), SUBDIVISIONS);
            if (snapped.getDuration() > 0) {
                onsetsSnap.add(snapped);
            }
        }
        onsetsSnap.sort(Comparator.naturalOrder());
        List<Onset> bass = new ArrayList<>();
        List<Onset> treble = new ArrayList<>();
        splitter.split(onsetsSnap, bass, treble);

        // TODO Check whether this is okay
        // List<Onset> phrasesSource = onsetsSnap;
        List<Onset> phrasesSource = treble;

        if (phrasesSource.isEmpty()) {
            return Collections.emptyList();
        }

        // Do tracking
        List<Phrase> extractedPhrases = new ArrayList<>();
        Beat beat = rhythm.getBeat();
        long baseTimestamp = phrasesSource.get(0).getTimestamp();
        Set<Integer> pitches = new HashSet<>();
        ListIterator<Onset> it = phrasesSource.listIterator();
        PriorityQueue<PhraseExtractorTracker> trackers = new PriorityQueue<>();
        List<PhraseExtractorTracker> reintroduceTrackers = new ArrayList<>();
        Onset onset = it.next();
        do {
            // Aggregate similar onsets in one score element
            long timestamp = onset.getTimestamp();
            int duration = onset.getDuration();
            int basePitch = onset.getPitch();
            // NOTE "similarity" logic may need to be reconsidered
            while ((onset != null)
                    && (onset.getTimestamp() == timestamp)
                    && (onset.getDuration() == duration)
                    && (onset.getPitch() - basePitch < PHRASE_PITCH_DISTANCE)) {
                pitches.add(onset.getPitch());
                onset = it.hasNext() ? it.next() : null;
            }
            double elementPosition = (timestamp - baseTimestamp) / ((double) beat.getDuration());
            double elementDuration = duration / ((double) beat.getDuration());
            ScoreElement scoreElement = new ScoreElement(elementDuration, pitches);
            pitches.clear();
            // Finalize trackers further than 1 beat behind
            PhraseExtractorTracker tracker = trackers.poll();
            while ((tracker != null)
                    && ((elementPosition - tracker.getPosition()) > (PHRASE_POSITION_DISTANCE + POSITION_EPS))) {
                extractedPhrases.add(tracker.getPhrase());
                tracker = trackers.poll();
            }
            // Skip trackers that are behind but do not match the pitch
            while ((tracker != null)
                    && (tracker.getPosition() <= (elementPosition + POSITION_EPS))
                    && (Math.abs(basePitch - tracker.getLowestPitch())) > PHRASE_PITCH_DISTANCE) {
                reintroduceTrackers.add(tracker);
                tracker = trackers.poll();
            }
            // If there are no trackers behind available
            if ((tracker == null) || (tracker.getPosition() > (elementPosition + POSITION_EPS))) {
                if (tracker != null) {
                    reintroduceTrackers.add(tracker);
                }
                // New tracker
                tracker = new PhraseExtractorTracker(elementPosition);
            }
            reintroduceTrackers.add(tracker);
            tracker.addElement(scoreElement, elementPosition);
            trackers.addAll(reintroduceTrackers);
            reintroduceTrackers.clear();
        } while (it.hasNext());

        // Add remaining phrases
        while (!trackers.isEmpty()) {
            extractedPhrases.add(trackers.poll().getPhrase());
        }

        // Return only phrases of at least one bar
        double barBeats = rhythm.getTimeSignature().getBeats() - POSITION_EPS;
        return extractedPhrases.stream().filter(p -> p.getDuration() > barBeats).collect(Collectors.toList());
    }

    /**
     * Create a new onset resulting of snapping the given onset to the closest
     * beat subdivision.
     *
     * The resulting onset has a timestamp and duration that matches exactly
     * some beat subdivisions.
     *
     * @param onset
     *            Onset to snap
     * @param beat
     *            Beat to which the onset is snapped
     * @param subdivision
     *            Number of beat subdivisons allowed (0 = full beat, 1 = half
     *            beat, 2 = quarter beat, etc.)
     * @return
     */
    public static Onset snapOnsetToBeat(Onset onset, Beat beat, int subdivision) {
        if (subdivision < 0) {
            throw new IllegalArgumentException("The allowed subdivision level cannot be negative");
        }
        long begin = beat.closestSubbeat(onset.getTimestamp(), subdivision);
        long end = beat.closestSubbeat(onset.getTimestamp() + onset.getDuration(), subdivision);
        int subbeatDuration = Math.round(beat.getDuration() / (float) (1 << subdivision));
        int duration = Math.round((end - begin) / ((float) subbeatDuration)) * subbeatDuration;
        return new Onset(begin, duration, onset.getPitch(), onset.getVelocity());
    }
}
