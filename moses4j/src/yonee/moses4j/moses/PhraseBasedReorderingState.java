package yonee.moses4j.moses;

import yonee.moses4j.moses.LexicalReorderingConfiguration.Direction;
import yonee.moses4j.moses.LexicalReorderingConfiguration.ModelType;
import yonee.utils.ASSERT;

/**
 * 
 * @author YONEE
 * @OK
 */
public class PhraseBasedReorderingState extends LexicalReorderingState {

	private WordsRange m_prevRange;
	private boolean m_first;

	public PhraseBasedReorderingState(final LexicalReorderingConfiguration config, Direction dir,
			int offset) {
		super(config, dir, offset);
		m_prevRange = new WordsRange(TypeDef.NOT_FOUND, TypeDef.NOT_FOUND);
		m_first = (true);
	}

	public PhraseBasedReorderingState(final PhraseBasedReorderingState prev,
			final TranslationOption topt) {
		super(prev, topt);
		m_prevRange = (topt.getSourceWordsRange());
		m_first = (false);
	}

	public int compare(final FFState o) {
		if (o == this)
			return 0;

		final PhraseBasedReorderingState other = (PhraseBasedReorderingState) (o);
		ASSERT.a(other != null);
		if (m_prevRange == other.m_prevRange) {
			if (m_direction == Direction.Forward) {
				return comparePrevScores(other.m_prevScore);
			} else {
				return 0;
			}
		} else if (m_prevRange.less(other.m_prevRange)) {
			return -1;
		}
		return 1;
	}

	public LexicalReorderingState expand(final TranslationOption topt, float[] scores) {
		int reoType;
		final WordsRange currWordsRange = topt.getSourceWordsRange();
		final ModelType modelType = m_configuration.getModelType();

		if (m_direction == Direction.Forward && m_first) {
			clearScores(scores);
		} else {
			if (modelType == ModelType.MSD) {
				reoType = getOrientationTypeMSD(currWordsRange);
			} else if (modelType == ModelType.MSLR) {
				reoType = getOrientationTypeMSLR(currWordsRange);
			} else if (modelType == ModelType.Monotonic) {
				reoType = getOrientationTypeMonotonic(currWordsRange);
			} else {
				reoType = getOrientationTypeLeftRight(currWordsRange);
			}
			copyScores(scores, topt, reoType);
		}

		return new PhraseBasedReorderingState(this, topt);
	}

	public int getOrientationTypeMSD(WordsRange currRange) {
		if (m_first) {
			if (currRange.getStartPos() == 0) {
				return M;
			} else {
				return D;
			}
		}
		if (m_prevRange.getEndPos() == currRange.getStartPos() - 1) {
			return M;
		} else if (m_prevRange.getStartPos() == currRange.getEndPos() + 1) {
			return S;
		}
		return D;
	}

	public int getOrientationTypeMSLR(WordsRange currRange) {
		if (m_first) {
			if (currRange.getStartPos() == 0) {
				return M;
			} else {
				return DR;
			}
		}
		if (m_prevRange.getEndPos() == currRange.getStartPos() - 1) {
			return M;
		} else if (m_prevRange.getStartPos() == currRange.getEndPos() + 1) {
			return S;
		} else if (m_prevRange.getEndPos() < currRange.getStartPos()) {
			return DR;
		}
		return DL;
	}

	public int getOrientationTypeMonotonic(WordsRange currRange) {
		if ((m_first && currRange.getStartPos() == 0)
				|| (m_prevRange.getEndPos() == currRange.getStartPos() - 1)) {
			return M;
		}
		return NM;
	}

	public int getOrientationTypeLeftRight(WordsRange currRange) {
		if (m_first || (m_prevRange.getEndPos() <= currRange.getStartPos())) {
			return R;
		}
		return L;
	}

}
