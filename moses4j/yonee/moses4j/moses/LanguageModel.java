package yonee.moses4j.moses;

import java.util.ArrayList;

import java.util.List;

import yonee.moses4j.moses.TypeDef.LMType;
import yonee.utils.ASSERT;
import yonee.utils.CollectionUtils;
import yonee.utils.Ref;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public abstract class LanguageModel extends StatefulFeatureFunction {
	protected float m_weight; // ! scoring weight. Shouldn't this now be
	// superceded by ScoreProducer???

	protected String m_filePath; // ! for debugging purposes
	protected int m_nGramOrder; // ! max n-gram length contained in this LM
	protected Word m_sentenceStartArray = new Word(), m_sentenceEndArray = new Word(); // ! Contains

	// factors which
	// represents
	// the beging
	// and end words
	// for this LM.

	// ! Usually <s> and </s>
	protected void shiftOrPush(List<Word> contextFactor, final Word word) {
		if (contextFactor.size() < m_nGramOrder) {
			contextFactor.add(word);
		} else { // shift
			for (int currNGramOrder = 0; currNGramOrder < m_nGramOrder - 1; currNGramOrder++) {
				contextFactor.set(currNGramOrder, contextFactor.get(currNGramOrder + 1));
			}
			contextFactor.set(m_nGramOrder - 1, word);
		}
	}

	/**
	 * constructor to be called by inherited class \param registerScore whether
	 * this LM will be directly used to score sentence. Usually true, except
	 * where LM is a component in a composite LM, eg. LanguageModelJoint
	 */
	protected LanguageModel(boolean registerScore, ScoreIndexManager scoreIndexManager) {
		if (registerScore)
			scoreIndexManager.addScoreProducer(this);
	}

	/*
	 * Returned from LM implementations which points at the state used. For
	 * example, if a trigram score was requested but the LM backed off to using
	 * the trigram, the State pointer will point to the bigram. Used for more
	 * agressive pruning of hypothesis
	 */

	// ! see ScoreProducer.h
	public int getNumScoreComponents() {
		return 1;
	}

	// ! Single or multi-factor
	public abstract LMType getLMType();

	/*
	 * whether this LM can be used on a particular phrase. Should return false
	 * if phrase size = 0 or factor types required don't exists
	 */
	public abstract boolean useable(final Phrase phrase);

	/*
	 * calc total unweighted LM score of this phrase and return score via
	 * arguments. Return scores should always be in natural log, regardless of
	 * representation with LM implementation. Uses GetValue() of inherited
	 * class. Useable() should be called beforehand on the phrase \param
	 * fullScore scores of all unigram, bigram... of contiguous n-gram of the
	 * phrase \param ngramScore score of only n-gram of order m_nGramOrder
	 */// @BUG primity value
	public void calcScore(final Phrase phrase, Ref<Float> beginningBitsOnly, Ref<Float> ngramScore) {
		beginningBitsOnly.v = 0f;
		ngramScore.v = 0f;

		int phraseSize = phrase.getSize();

		List<Word> contextFactor = new ArrayList<Word>(m_nGramOrder);
		// contextFactor.reserve(m_nGramOrder);

		int currPos = 0;
		while (currPos < phraseSize) {
			final Word word = phrase.getWord(currPos);
			ASSERT.a(!word.isNonTerminal());

			shiftOrPush(contextFactor, word);
			ASSERT.a(contextFactor.size() <= m_nGramOrder);

			if (word.equals(getSentenceStartArray())) { // do nothing, don't
				// include prob for <s>
				// unigram
				ASSERT.a(currPos == 0);
			} else {
				float partScore = getValue(contextFactor);

				if (contextFactor.size() == m_nGramOrder)
					ngramScore.v += partScore;
				else
					beginningBitsOnly.v += partScore;
			}

			currPos++;
		}
	}

	public void calcScoreChart(final Phrase phrase, Ref<Float> beginningBitsOnly,
			Ref<Float> ngramScore) {
		beginningBitsOnly.v = 0f;
		ngramScore.v = 0f;

		int phraseSize = phrase.getSize();

		List<Word> contextFactor = new ArrayList<Word>(m_nGramOrder);
		// contextFactor.reserve(m_nGramOrder);
		// contextFactor.

		int currPos = 0;
		while (currPos < phraseSize) {
			final Word word = phrase.getWord(currPos);
			ASSERT.a(!word.isNonTerminal());

			shiftOrPush(contextFactor, word);
			ASSERT.a(contextFactor.size() <= m_nGramOrder);

			if (word == getSentenceStartArray()) { // do nothing, don't include
				// prob for <s> unigram
				ASSERT.a(currPos == 0);
			} else {
				float partScore = getValue(contextFactor);

				if (contextFactor.size() == m_nGramOrder)
					ngramScore.v += partScore;
				else
					beginningBitsOnly.v += partScore;
			}
			currPos++;
		}
	}

	/*
	 * get score of n-gram. n-gram should not be bigger than m_nGramOrder
	 * Specific implementation can return State and len data to be used in
	 * hypothesis pruning \param contextFactor n-gram to be scored \param
	 * finalState state used by LM. Return arg \param len ???
	 */
	public float getValue(final List<Word> contextFactor) {
		return getValue(contextFactor, null);
	}

	public float getValue(final List<Word> contextFactor, Integer finalState) {
		return getValue(contextFactor, null, null);
	}

	public abstract float getValue(final List<Word> contextFactor, Ref<Object> finalState // = 0
			, Ref<Integer> len);//==0

	// ! get State for a particular n-gram
	public Ref<Object> getState(final List<Word> contextFactor) {// =0
		return getState(contextFactor, null);
	}

	public Ref<Object> getState(final List<Word> contextFactor, Ref<Integer> len) {// =0
		Ref<Object> state = new Ref<Object>(null);
		int dummy = 0;
		if (len != null)
			len.v = dummy;
		getValue(contextFactor, state, len);
		return state;
	}

	// ! max n-gram order of LM
	public int getNGramOrder() {
		return m_nGramOrder;
	}

	// ! Contains factors which represents the beging and end words for this LM.
	// Usually <s> and </s>
	public final Word getSentenceStartArray() {
		return m_sentenceStartArray;
	}

	public final Word getSentenceEndArray() {
		return m_sentenceEndArray;
	}

	// ! scoring weight. Shouldn't this now be superceded by ScoreProducer???
	public float getWeight() {
		return m_weight;
	}

	public void setWeight(float weight) {
		m_weight = weight;
	}

	public abstract String getScoreProducerDescription();

	public String getScoreProducerWeightShortName() {
		return "lm";
	}

	// ! overrideable funtions for IRST LM to cleanup. Maybe something to do
	// with on demand/cache loading/unloading
	public void initializeBeforeSentenceProcessing() {
	}

	public void cleanUpAfterSentenceProcessing() {
	}

	class LMState implements FFState {
		public Integer lmstate;

		public LMState(final Integer lms) {
			lmstate = lms;
		}

		public int compare(final FFState o) {
			final LMState other = (LMState) o;
			if (other.lmstate > lmstate)
				return 1;
			else if (other.lmstate < lmstate)
				return -1;
			return 0;
		}
	};

	public FFState emptyHypothesisState(final InputType input) {
		return new LMState(null);
	}

	public FFState evaluate(final Hypothesis hypo,// YONEE
			final FFState ps, ScoreComponentCollection out) {

		// In this function, we only compute the LM scores of n-grams that
		// overlap a
		// phrase boundary. Phrase-internal scores are taken directly from the
		// translation option. In the unigram case, there is no overlap, so we
		// don't
		// need to do anything.
		if (m_nGramOrder <= 1)
			return null;

		long t = 0;
		if (VERBOSE.v(2)) {
			t = System.currentTimeMillis();
		} // track time
		final Integer prevlm = ps != null ? (((LMState) (ps)).lmstate) : null;
		LMState res = new LMState(prevlm);
		if (hypo.getCurrTargetLength() == 0)
			return res;
		final int currEndPos = hypo.getCurrTargetWordsRange().getEndPos();
		final int startPos = hypo.getCurrTargetWordsRange().getStartPos();

		// 1st n-gram
		List<Word> contextFactor = new ArrayList<Word>((m_nGramOrder));
		CollectionUtils.init(contextFactor, m_nGramOrder, null);

		int index = 0;
		for (int currPos = (int) startPos - (int) m_nGramOrder + 1; currPos <= (int) startPos; currPos++) {
			if (currPos >= 0)
				contextFactor.set(index++, hypo.getWord(currPos));
			else
				contextFactor.set(index++, getSentenceStartArray());
		}
		float lmScore = getValue(contextFactor);
		// cout<<"context factor: "<<GetValue(contextFactor)<<endl;

		// main loop
		int endPos = Math.min(startPos + m_nGramOrder - 2, currEndPos);
		for (int currPos = startPos + 1; currPos <= endPos; currPos++) {
			// shift all args down 1 place
			for (int i = 0; i < m_nGramOrder - 1; i++)
				contextFactor.set(i, contextFactor.get(i + 1));

			// add last factor
			contextFactor.set(contextFactor.size() - 1, hypo.getWord(currPos));

			lmScore += getValue(contextFactor);
		}

		// end of sentence
		if (hypo.isSourceCompleted()) {
			final int size = hypo.getSize();
			contextFactor.set(contextFactor.size() - 1, getSentenceEndArray());

			for (int i = 0; i < m_nGramOrder - 1; i++) {
				int currPos = (int) (size - m_nGramOrder + i + 1);
				if (currPos < 0)
					contextFactor.set(i, getSentenceStartArray());
				else
					contextFactor.set(i, hypo.getWord(currPos));
			}
			lmScore += getValue(contextFactor, res.lmstate);
		} else {
			for (int currPos = endPos + 1; currPos <= currEndPos; currPos++) {
				for (int i = 0; i < m_nGramOrder - 1; i++)
					contextFactor.set(i, contextFactor.get(i + 1));
				contextFactor.set(contextFactor.size() - 1, hypo.getWord(currPos));
			}
			res.lmstate = (Integer)getState(contextFactor).v;
		}
		out.plusEquals(this, lmScore);
		if (VERBOSE.v(2)) {
			hypo.getManager().getSentenceStats().addTimeCalcLM(System.currentTimeMillis() - t);
		}
		return res;
	}

}
