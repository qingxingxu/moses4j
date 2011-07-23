package yonee.moses4j.moses;

import yonee.moses4j.moses.LexicalReorderingConfiguration.Direction;
import yonee.moses4j.moses.LexicalReorderingConfiguration.ModelType;
import yonee.utils.ASSERT;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class HierarchicalReorderingForwardState extends LexicalReorderingState {

	private boolean m_first;
	private WordsRange m_prevRange;
	private WordsBitmap m_coverage;

	public HierarchicalReorderingForwardState(final LexicalReorderingConfiguration config,
			int size, int offset) {
		super(config, Direction.Forward, offset);
		m_first = true;
		m_prevRange = new WordsRange(TypeDef.NOT_FOUND, TypeDef.NOT_FOUND);
		m_coverage = new WordsBitmap(size);
	}

	public HierarchicalReorderingForwardState(final HierarchicalReorderingForwardState prev,
			final TranslationOption topt) {
		super(prev, topt);

		m_first = false;
		m_prevRange = new WordsRange(topt.getSourceWordsRange());
		m_coverage = prev.m_coverage;
		final WordsRange currWordsRange = topt.getSourceWordsRange();
		m_coverage.setValue(currWordsRange.getStartPos(), currWordsRange.getEndPos(), true);
	}

	public int compare(final FFState o) {
		if (o == this)
			return 0;

		final HierarchicalReorderingForwardState other = (HierarchicalReorderingForwardState) (o);
		ASSERT.a(other != null);
		if (m_prevRange == other.m_prevRange) {
			return comparePrevScores(other.m_prevScore);
		} else if (m_prevRange.less(other.m_prevRange)) {
			return -1;
		}
		return 1;
	}

	public LexicalReorderingState expand(final TranslationOption topt, float[] scores) {
		final ModelType modelType = m_configuration.getModelType();
		final WordsRange currWordsRange = topt.getSourceWordsRange();
		// keep track of the current coverage ourselves so we don't need the hypothesis
		WordsBitmap coverage = m_coverage;
		coverage.setValue(currWordsRange.getStartPos(), currWordsRange.getEndPos(), true);

		int reoType;

		if (m_first) {
			clearScores(scores);
		} else {
			if (modelType == ModelType.MSD) {
				reoType = getOrientationTypeMSD(currWordsRange, coverage);
			} else if (modelType == ModelType.MSLR) {
				reoType = getOrientationTypeMSLR(currWordsRange, coverage);
			} else if (modelType == ModelType.Monotonic) {
				reoType = getOrientationTypeMonotonic(currWordsRange, coverage);
			} else {
				reoType = getOrientationTypeLeftRight(currWordsRange, coverage);
			}

			copyScores(scores, topt, reoType);
		}

		return new HierarchicalReorderingForwardState(this, topt);
	}

	private int getOrientationTypeMSD(WordsRange currRange, WordsBitmap coverage) {
		if (currRange.getStartPos() > m_prevRange.getEndPos()
				&& (!coverage.getValue(m_prevRange.getEndPos() + 1) || currRange.getStartPos() == m_prevRange
						.getEndPos() + 1)) {
			return M;
		} else if (currRange.getEndPos() < m_prevRange.getStartPos()
				&& (!coverage.getValue(m_prevRange.getStartPos() - 1) || currRange.getEndPos() == m_prevRange
						.getStartPos() - 1)) {
			return S;
		}
		return D;
	}

	private int getOrientationTypeMSLR(WordsRange currRange, WordsBitmap coverage) {
		if (currRange.getStartPos() > m_prevRange.getEndPos()
				&& (!coverage.getValue(m_prevRange.getEndPos() + 1) || currRange.getStartPos() == m_prevRange
						.getEndPos() + 1)) {
			return M;
		} else if (currRange.getEndPos() < m_prevRange.getStartPos()
				&& (!coverage.getValue(m_prevRange.getStartPos() - 1) || currRange.getEndPos() == m_prevRange
						.getStartPos() - 1)) {
			return S;
		} else if (currRange.getStartPos() > m_prevRange.getEndPos()) {
			return DR;
		}
		return DL;
	}

	private int getOrientationTypeMonotonic(WordsRange currRange, WordsBitmap coverage) {
		if (currRange.getStartPos() > m_prevRange.getEndPos()
				&& (!coverage.getValue(m_prevRange.getEndPos() + 1) || currRange.getStartPos() == m_prevRange
						.getEndPos() + 1)) {
			return M;
		}
		return NM;
	}

	private int getOrientationTypeLeftRight(WordsRange currRange, WordsBitmap coverage) {
		if (currRange.getStartPos() > m_prevRange.getEndPos()) {
			return R;
		}
		return L;
	}

}
