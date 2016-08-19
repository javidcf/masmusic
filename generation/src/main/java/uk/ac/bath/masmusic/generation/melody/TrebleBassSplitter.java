package uk.ac.bath.masmusic.generation.melody;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import uk.ac.bath.masmusic.common.Onset;

/**
 * Treble/bass splitter.
 *
 * @author Javier Dehesa
 */
public class TrebleBassSplitter {

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
    public void split(List<Onset> onsets, List<Onset> bass, List<Onset> treble) {
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
