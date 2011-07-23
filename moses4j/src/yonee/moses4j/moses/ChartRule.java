package yonee.moses4j.moses;


import java.util.Arrays;
import yonee.utils.ASSERT;
import yonee.utils.Pair;

/**
 * 
 * @author YONEE
 * @REST<<
 */
public class ChartRule {

	protected TargetPhrase m_targetPhrase;
	protected WordConsumed m_lastWordConsumed;
	/*
	 * map each source word in the phrase table to:
	 * 1. a word in the input sentence, if the pt word is a terminal
	 * 2. a 1+ phrase in the input sentence, if the pt word is a non-terminal
	 */
	protected int[] m_wordsConsumedTargetOrder;// = new ArrayList<Integer>();

	/*
	 * size is the size of the target phrase.
	 * Usually filled with NOT_KNOWN, unless the pos is a non-term, in which case its filled
	 * with its index
	 */

	public ChartRule(final TargetPhrase targetPhrase, final WordConsumed lastWordConsumed) {
		m_targetPhrase = targetPhrase;
		m_lastWordConsumed = lastWordConsumed;
	}

	public final TargetPhrase getTargetPhrase() {
		return m_targetPhrase;
	}

	public final WordConsumed getLastWordConsumed() {
		return m_lastWordConsumed;
	}

	public final int[] getWordsConsumedTargetOrder() {
		return m_wordsConsumedTargetOrder;
	}

	public void createNonTermIndex() {
		//CollectionUtils.resize(m_wordsConsumedTargetOrder, m_targetPhrase.getSize(),
			//	TypeDef.NOT_FOUND);
		m_wordsConsumedTargetOrder = new int[m_targetPhrase.getSize()];
		Arrays.fill(m_wordsConsumedTargetOrder, TypeDef.NOT_FOUND);
		final AlignmentInfo alignInfo = m_targetPhrase.getAlignmentInfo();

		int nonTermInd = 0;
		int prevSourcePos = 0;

		for (Pair<Integer, Integer> iter : alignInfo.c()) {
			int sourcePos = iter.first;
			if (nonTermInd > 0) {
				ASSERT.a(sourcePos > prevSourcePos);
			}
			prevSourcePos = sourcePos;

			int targetPos = iter.second;
			m_wordsConsumedTargetOrder[targetPos] = nonTermInd;
			nonTermInd++;
		}
	}

}
