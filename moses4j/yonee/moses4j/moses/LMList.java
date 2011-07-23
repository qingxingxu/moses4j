package yonee.moses4j.moses;

import java.util.LinkedList;

import yonee.utils.ASSERT;
import yonee.utils.Ref;

/**
 * 
 * @author YONEE
 * @OK
 */
public class LMList extends LinkedList<LanguageModel> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected int m_maxNGramOrder;
	protected int m_minInd, m_maxInd;

	public LMList() {

		m_maxNGramOrder = 0;
		m_minInd = Integer.MAX_VALUE;
		m_maxInd = 0;
	}

	public void calcScore(final Phrase phrase, Ref<Float> retFullScore, Ref<Float> retNGramScore,
			ScoreComponentCollection breakdown) {
		// /const_iterator lmIter;
		for (LanguageModel lm : this) {
			// const LanguageModel &lm = **lmIter;
			final float weightLM = lm.getWeight();

			Ref<Float> fullScore = new Ref<Float> ( 0f);
			Ref<Float> nGramScore =  new Ref<Float> ( 0f);

			// do not process, if factors not defined yet (happens in partial translation options)
			if (!lm.useable(phrase))
				continue;

			lm.calcScore(phrase,fullScore, nGramScore);

			breakdown.assign(lm, nGramScore.v); // I'm not sure why += doesn't work here- it should be
			// 0.0 right?
			retFullScore.v += fullScore.v * weightLM;
			retNGramScore.v += nGramScore.v * weightLM;
		}
	}

	public void calcAllLMScores(final Phrase phrase, ScoreComponentCollection nGramOnly,
			ScoreComponentCollection beginningBitsOnly) {
		ASSERT.a(phrase.getNumTerminals() == phrase.getSize());

		for (LanguageModel lm : this) {

			// do not process, if factors not defined yet (happens in partial translation options)
			if (!lm.useable(phrase))
				continue;

			Ref<Float> beginningScore = new Ref<Float>(0.0f),nGramScore = new Ref<Float>(0.0f);
	
			lm.calcScoreChart(phrase, beginningScore, nGramScore);
			beginningScore.v = Util.untransformLMScore(beginningScore.v);
			
			nGramScore.v = Util.untransformLMScore(nGramScore.v);

			nGramOnly.plusEquals(lm, nGramScore.v);

			if (beginningBitsOnly != null)
				beginningBitsOnly.plusEquals(lm, beginningScore.v);

		}
	}

	public boolean add(LanguageModel lm) {
		boolean flag = super.add(lm);
		if (flag) {
			m_maxNGramOrder = (lm.getNGramOrder() > m_maxNGramOrder) ? lm.getNGramOrder()
					: m_maxNGramOrder;

			final ScoreIndexManager scoreMgr = StaticData.instance().getScoreIndexManager();
			int startInd = scoreMgr.getBeginIndex(lm.getScoreBookkeepingID()), endInd = scoreMgr
					.getEndIndex(lm.getScoreBookkeepingID()) - 1;

			m_minInd = Math.min(m_minInd, startInd);
			m_maxInd = Math.max(m_maxInd, endInd);
		}
		return flag;
	}

	public int getMaxNGramOrder() {
		return m_maxNGramOrder;
	}

	public int getMinIndex() {
		return m_minInd;
	}

	public int getMaxIndex() {
		return m_maxInd;
	}

}
