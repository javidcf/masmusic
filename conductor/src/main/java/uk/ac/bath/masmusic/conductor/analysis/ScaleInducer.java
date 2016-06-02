package uk.ac.bath.masmusic.conductor.analysis;

import java.util.List;
import java.util.Set;

import uk.ac.bath.masmusic.common.Note;
import uk.ac.bath.masmusic.common.Scale;

/**
 * Scale inducer for onset events.
 *
 * @author Javier Dehesa
 */
public class ScaleInducer {

    /**
     * Induce a scale from a collection of notes.
     *
     * @param notes
     *            The list of notes from where the scale is induced
     * @return The induced scale
     */
    public Scale induceScale(List<Note> notes) {
        Set<Scale> allScales = Scale.getAllScales();
        Scale bestScale = null;
        int bestScore = Integer.MIN_VALUE;
        for (Scale scale : allScales) {
            int scaleScore = 0;
            for (Note note : notes) {
                scaleScore += scaleNoteScore(scale, note);
            }
            if (scaleScore > bestScore) {
                bestScore = scaleScore;
                bestScale = scale;
            }
        }
        return bestScale;
    }

    /**
     * @param scale
     *            A musical scale
     * @param note
     *            A note
     * @return The contribution to the scale score of the note
     */
    private int scaleNoteScore(Scale scale, Note note) {
        int degree = scale.degreeOf(note);
        if (degree >= 0) {
            if (degree == 0) {
                return 3;
            } else if (scale.size() == 7) {
                // Consider diferent degrees in heptatonic scales
                if (degree == 4 || degree == 5) {
                    return 2;
                } else {
                    return 2;
                }
            } else {
                return 1;
            }
        } else {
            return -1;
        }
    }

}
