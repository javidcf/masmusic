package uk.ac.bath.masmusic.conductor.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.stereotype.Component;

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
@Component
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
        splitBassAndTrebble(onsetsSnap, bass, treble);
        // TODO Make use of split parts in phrase extraction

        if (onsetsSnap.isEmpty()) {
            return Collections.emptyList();
        }

        // Do tracking
        List<Phrase> extractedPhrases = new ArrayList<>();
        Beat beat = rhythm.getBeat();
        long baseTimestamp = onsetsSnap.get(0).getTimestamp();
        Set<Integer> pitches = new HashSet<>();
        ListIterator<Onset> it = onsetsSnap.listIterator();
        PriorityQueue<PhraseTracker> trackers = new PriorityQueue<>();
        List<PhraseTracker> reintroduceTrackers = new ArrayList<>();
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
                    && (onset.getPitch() - basePitch <= PHRASE_PITCH_DISTANCE)) {
                pitches.add(onset.getPitch());
                onset = it.hasNext() ? it.next() : null;
            }
            float elementPosition = (timestamp - baseTimestamp) / ((float) beat.getDuration());
            float elementDuration = duration / ((float) beat.getDuration());
            ScoreElement scoreElement = new ScoreElement(elementDuration, pitches);
            pitches.clear();
            // Finalize trackers further than 1 beat behind
            PhraseTracker tracker = trackers.poll();
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
                tracker = new PhraseTracker(elementPosition);
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

        return extractedPhrases;
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

    /**
     * Splits the bass and treble parts of a list of onsets.
     *
     * @param onsets
     *            Sequence of onsets to split
     * @param bass
     *            The split bass part
     * @param treble
     *            The split treble part
     */
    public void splitBassAndTrebble(List<Onset> onsets, List<Onset> bass, List<Onset> treble) {
        if (onsets.isEmpty()) {
            bass.clear();
            treble.clear();
            return;
        }

        // Separate onsets that are more than one octave apart
        List<List<Onset>> parts = new ArrayList<>();
        for (Onset onset : onsets) {
            // Look for a part fitting the onset
            int pitchDifference = Integer.MAX_VALUE;
            List<Onset> onsetPart = null;
            for (List<Onset> part : parts) {
                int partPitchDifference = Math.abs(part.get(part.size() - 1).getPitch() - onset.getPitch());
                if (partPitchDifference < pitchDifference) {
                    pitchDifference = partPitchDifference;
                    onsetPart = part;
                }
            }
            // Check whether a valid part has been found that is close enough
            if ((onsetPart == null) || (pitchDifference > 7)) {
                onsetPart = new ArrayList<>();
                parts.add(onsetPart);
            }
            onsetPart.add(onset);
        }

        // Compute average pitch of all the parts and keep track of the index
        float[] avgPitches = new float[parts.size()];
        float minAvgPitch = Float.POSITIVE_INFINITY;
        float maxAvgPitch = Float.NEGATIVE_INFINITY;
        float totalAvgPitch = 0f;
        int i = 0;
        for (List<Onset> part : parts) {
            float partAvgPitch = 0f;
            for (Onset onset : part) {
                partAvgPitch += onset.getPitch();
                totalAvgPitch += onset.getPitch();
            }
            partAvgPitch /= part.size();
            if (minAvgPitch > partAvgPitch) {
                minAvgPitch = partAvgPitch;
            }
            if (maxAvgPitch < partAvgPitch) {
                maxAvgPitch = partAvgPitch;
            }
            avgPitches[i] = partAvgPitch;
            i++;
        }
        totalAvgPitch /= onsets.size();

        bass.clear();
        treble.clear();
        // Check if extreme parts are far apart enough
        if (maxAvgPitch - minAvgPitch > 12) {
            // Merge everything into the two parts
            i = 0;
            for (List<Onset> part : parts) {
                if (avgPitches[i] < totalAvgPitch) {
                    bass.addAll(part);
                } else {
                    treble.addAll(part);
                }
                i++;
            }
            bass.sort(Comparator.naturalOrder());
            treble.sort(Comparator.naturalOrder());
        } else {
            // Put everything into one of the parts
            List<Onset> mainPart;
            if (totalAvgPitch < 60) {
                mainPart = bass;
            } else {
                mainPart = treble;
            }
            mainPart.addAll(onsets);
            mainPart.sort(Comparator.naturalOrder());
        }
    }
}
