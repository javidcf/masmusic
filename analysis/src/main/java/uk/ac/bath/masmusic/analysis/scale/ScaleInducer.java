package uk.ac.bath.masmusic.analysis.scale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Onset;
import uk.ac.bath.masmusic.common.Scale;

/**
 * Scale inducer for onset events.
 *
 * Implements Krumhansl-Schmuckler key determination algorithm using
 * Aarden-Essen weights.
 *
 * @author Javier Dehesa
 */
public class ScaleInducer {

    /** Aarden-Essen pitch weights. */
    private static final Map<String, double[]> PITCH_WEIGHTS;
    static {
        PITCH_WEIGHTS = new HashMap<>();
        PITCH_WEIGHTS.put("major", new double[] { 17.7661, 0.145624, 14.9265, 0.160186, 19.8049, 11.3587, 0.291248,
                22.062, 0.145624, 8.15494, 0.232998, 4.95122 });
        PITCH_WEIGHTS.put("minor", new double[] { 18.2648, 0.737619, 14.0499, 16.8599, 0.702494, 14.4362, 0.702494,
                18.6161, 4.56621, 1.93186, 7.37619, 1.75623 });
    }

    /**
     * Induce a scale from a collection of onsets.
     *
     * @param onsets
     *            The list of onsets from where the scale is induced
     * @return The induced scale
     */
    public Scale induceScale(List<Onset> onsets) {
        // Induce scale with a significant amount of notes
        if (onsets.size() < 20) {
            return null;
        }
        Set<Scale> allScales = Scale.getAllScales();
        Scale bestScale = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Scale scale : allScales) {
            String scaleType = scale.getType().trim().toLowerCase();
            if (!PITCH_WEIGHTS.containsKey(scaleType)) {
                continue;
            }
            double[] pitchWeights = PITCH_WEIGHTS.get(scaleType);
            double scaleScore = .0;
            Note fundamental = scale.getFundamental();
            for (Onset onset : onsets) {
                int scalePitchClass = fundamental.ascendingDistanceTo(Note.fromValue(onset.getPitch()));
                scaleScore += pitchWeights[scalePitchClass] * onset.getDuration();
            }
            if (scaleScore > bestScore) {
                bestScore = scaleScore;
                bestScale = scale;
            }
        }
        return bestScale;
    }

}
