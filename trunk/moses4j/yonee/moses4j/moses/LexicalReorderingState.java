package yonee.moses4j.moses;

import java.util.Arrays;
import java.util.List;

import yonee.moses4j.moses.LexicalReorderingConfiguration.Direction;


/**
 * 
 * @author YONEE
 * @OK
 */
public abstract class LexicalReorderingState implements FFState {

	public abstract int compare(final FFState o);

	public abstract LexicalReorderingState expand(final TranslationOption hypo, float[] scores);

	

	// typedef int ReorderingType;

	protected LexicalReorderingConfiguration m_configuration;
	// The following is the true direction of the object, which can be Backward or Forward even if
	// the Configuration has Bidirectional.
	protected LexicalReorderingConfiguration.Direction m_direction;
	protected int m_offset;
	protected List<Float> m_prevScore = null;

	protected LexicalReorderingState(final LexicalReorderingState prev, final TranslationOption topt) {
		m_configuration = prev.m_configuration;
		m_direction = prev.m_direction;
		m_offset = prev.m_offset;
		m_prevScore = topt.getCachedScores(m_configuration.getScoreProducer());
	}

	protected LexicalReorderingState(final LexicalReorderingConfiguration config,
			LexicalReorderingConfiguration.Direction dir, int offset) {
		m_configuration = config;
		m_direction = dir;
		m_offset = offset;
		m_prevScore = null;
	}

	// copy the right scores in the right places, taking into account forward/backward, offset,
	// collapse
	protected void copyScores(float[] scores, final TranslationOption topt, int reoType) {
		// don't call this on a bidirectional object
		assert (m_direction == Direction.Backward || m_direction == Direction.Forward);
		final List<Float> cachedScores = (m_direction == Direction.Backward) ? topt
				.getCachedScores(m_configuration.getScoreProducer()) : m_prevScore;

		// No scores available. TODO: Using a good prior distribution would be nicer.
		if (cachedScores == null)
			return;

		final List<Float> scoreSet = cachedScores;
		if (m_configuration.collapseScores())
			scores[m_offset] = scoreSet.get(m_offset + reoType);
		else {		
			Arrays.fill(scores, 0 + m_offset, 0 + m_offset
					+ m_configuration.getNumberOfTypes(), 0f);

			scores[m_offset + reoType] =  scoreSet.get(m_offset + reoType);
		}
	}

	protected void clearScores(float[] scores) {
		if (m_configuration.collapseScores())
			scores[m_offset] = 0.0f;
		else			
			Arrays.fill(scores, 0 + m_offset, 0 + m_offset
					+ m_configuration.getNumberOfTypes(), 0f);
	}

	protected int comparePrevScores(final List<Float> other) {
		if (m_prevScore == other)
			return 0;

		// The pointers are null if a phrase pair isn't found in the reordering table.
		if (other == null)
			return -1;
		if (m_prevScore == null)
			return 1;

		final List<Float> my = m_prevScore;
		final List<Float> their = other;
		for (int i = m_offset; i < m_offset + m_configuration.getNumberOfTypes(); i++)
			if (my.get(i) < their.get(i))
				return -1;
			else if (my.get(i) > their.get(i))
				return 1;

		return 0;
	}

	// finalants for the different type of reorderings (corresponding to indexes in the table file)
	protected static final int M = 0; // monotonic
	protected static final int NM = 1; // non-monotonic
	protected static final int S = 1; // swap
	protected static final int D = 2; // discontinuous
	protected static final int DL = 2; // discontinuous, left
	protected static final int DR = 3; // discontinuous, right
	protected static final int R = 0; // right
	protected static final int L = 1; // left

}
