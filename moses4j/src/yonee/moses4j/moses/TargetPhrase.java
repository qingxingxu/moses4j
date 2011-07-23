package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.utils.ASSERT;
import yonee.utils.Pair;
import yonee.utils.Ref;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 * 
 *       innerProduct
 */
public class TargetPhrase extends Phrase {

	// friend std::ostream& operator<<(std::ostream&, const TargetPhrase&);

	protected float m_transScore, m_ngramScore, m_fullScore;
	// float m_ngramScore, m_fullScore;
	protected ScoreComponentCollection m_scoreBreakdown = new ScoreComponentCollection();
	protected AlignmentInfo m_alignmentInfo = new AlignmentInfo();

	// in case of confusion net, ptr to source phrase
	protected Phrase m_sourcePhrase;
	protected Word m_lhsTarget = new Word();
	protected CountInfo m_countInfo;

	protected static boolean wordalignflag;
	protected static boolean printalign;

	public TargetPhrase() {
		this(FactorDirection.Output);

	}

	public TargetPhrase(FactorDirection direction) {
		super(direction);
		m_transScore = (0.0f);
		m_ngramScore = (0.0f);
		m_fullScore = (0.0f);
		m_sourcePhrase = null;
		wordalignflag = StaticData.instance().useAlignmentInfo();
		printalign = StaticData.instance().printAlignmentInfo();
	
	}
	/**
	 * 拷贝函数，C++默认拷贝构造是全克隆
	 * @param copy
	 */
	public TargetPhrase(final TargetPhrase copy) {
		super(copy);
		m_transScore = copy.m_transScore;
		m_ngramScore = copy.m_ngramScore;
		m_fullScore = copy.m_fullScore;
		m_scoreBreakdown = copy.m_scoreBreakdown;
		m_alignmentInfo = copy.m_alignmentInfo;
		m_sourcePhrase = copy.m_sourcePhrase;
		m_lhsTarget = copy.m_lhsTarget;
		m_countInfo  = copy.m_countInfo;
	}

	public TargetPhrase(FactorDirection direction, String out_string) {
		super(direction);
		m_transScore = (0.0f);
		m_ngramScore = (0.0f);
		m_fullScore = (0.0f);
		m_sourcePhrase = null;

		// ACAT
		final StaticData staticData = StaticData.instance();
		createFromString(staticData.getInputFactorOrder(), out_string, staticData
				.getFactorDelimiter()); // @BUG
		wordalignflag = StaticData.instance().useAlignmentInfo();
		printalign = StaticData.instance().printAlignmentInfo();

	}

	/**
	 * used by the unknown word handler.
	 * Set alignment to 0
	 */
	public void setAlignment() {

	}

	// ! used by the unknown word handler- these targets
	// ! don't have a translation score, so wp is the only thing used
	public void setScore() {
		m_transScore = m_ngramScore = 0;
		m_fullScore = -StaticData.instance().getWeightWordPenalty();
	}

	// !Set score for Sentence XML target options
	public void setScore(float score) {
		// we use an existing score producer to figure out information for score setting (number of
		// scores and weights)
		// TODO: is this a good idea?
		ScoreProducer prod = StaticData.instance().getPhraseDictionaries().get(0);

		// get the weight list
		int id = prod.getScoreBookkeepingID();
		final List<Float> allWeights = StaticData.instance().getAllWeights();

		int beginIndex = StaticData.instance().getScoreIndexManager().getBeginIndex(id);
		int endIndex = StaticData.instance().getScoreIndexManager().getEndIndex(id);

		List<Float> weights = allWeights.subList(beginIndex, endIndex);

		// weights.addAll(allWeights.subList(beginIndex, endIndex));

		// find out how many items are in the score vector for this producer
		int numScores = prod.getNumScoreComponents();

		// divide up the score among all of the score vectors

		float[] scoreVector = new float[numScores];
		Arrays.fill(scoreVector, score / numScores);

		// numScores,score/numScores

		// Now we have what we need to call the full SetScore method
		setScore(prod, scoreVector, weights, StaticData.instance().getWeightWordPenalty(),
				StaticData.instance().getAllLM());
	}

	// ! set score for unknown words with input weights
	public void setScore(final float[] scoreVector) {
		// we use an existing score producer to figure out information for score setting (number of
		// scores and weights)
		ScoreProducer prod = StaticData.instance().getPhraseDictionaries().get(0);

		// get the weight list
		int id = prod.getScoreBookkeepingID();
		final List<Float> allWeights = StaticData.instance().getAllWeights();

		int beginIndex = StaticData.instance().getScoreIndexManager().getBeginIndex(id);
		int endIndex = StaticData.instance().getScoreIndexManager().getEndIndex(id);
		List<Float> weights = new ArrayList<Float>();

		weights.addAll(allWeights.subList(beginIndex, endIndex));

		// std::copy(allWeights.begin() +beginIndex, allWeights.begin() +
		// endIndex,std::back_inserter(weights));

		// expand the input weight vector
		ASSERT.a(scoreVector.length <= prod.getNumScoreComponents());
		float[] sizedScoreVector = new float[prod.getNumScoreComponents()];

		setScore(prod, sizedScoreVector, weights, StaticData.instance().getWeightWordPenalty(),
				StaticData.instance().getAllLM());
	}

	/***
	 * Called immediately after creation to initialize scores.
	 * 
	 * @param translationScoreProducer
	 *            The PhraseDictionaryMemory that this TargetPhrase is contained by.
	 *            Used to identify where the scores for this phrase belong in the list of all
	 *            scores.
	 * @param scoreVector
	 *            the vector of scores (log probs) associated with this translation
	 * @param weighT
	 *            the weights for the individual scores (t-weights in the .ini file)
	 * @param languageModels
	 *            all the LanguageModels that should be used to compute the LM scores
	 * @param weightWP
	 *            the weight of the word penalty
	 * 
	 * @TODO should this be part of the constructor? If not, add explanation why not.
	 */
	public void setScore(final ScoreProducer translationScoreProducer, final float[] scoreVector,
			final List<Float> weightT, float weightWP, final LMList languageModels) {
		assert (weightT.size() == scoreVector.length);
		// calc average score if non-best

		m_transScore = Util.innerProduct(scoreVector, 0, scoreVector.length, weightT, 0, 0.0f);
		m_scoreBreakdown.plusEquals(translationScoreProducer, scoreVector);

		// Replicated from TranslationOptions.cpp
		float totalFutureScore = 0;
		float totalNgramScore = 0;
		float totalFullScore = 0;

		for (LanguageModel lm : languageModels) {

			if (lm.useable(this)) { // contains factors used by this LM
				float weightLM = lm.getWeight();
				Ref<Float> fullScore = new Ref<Float>(0.0f), nGramScore =  new Ref<Float>(0.0f);

				lm.calcScore(this, fullScore, nGramScore);
				m_scoreBreakdown.assign(lm, nGramScore.v);

				// total LM score so far
				totalNgramScore += nGramScore.v * weightLM;
				totalFullScore += fullScore.v * weightLM;

			}
		}
		m_ngramScore = totalNgramScore;

		m_fullScore = m_transScore + totalFutureScore + totalFullScore - (getSize() * weightWP); // word
		// penalty
	}

	public void setScoreChart(final ScoreProducer translationScoreProducer,
			final float[] scoreVector, final List<Float> weightT, final LMList languageModels) {
		final StaticData staticData = StaticData.instance();

		ASSERT.a(weightT.size() == scoreVector.length);

		// calc average score if non-best
		m_transScore = Util.innerProduct(scoreVector, 0, scoreVector.length, weightT, 0, 0.0f);
		m_scoreBreakdown.plusEquals(translationScoreProducer, scoreVector);

		// Replicated from TranslationOptions.cpp
		float totalNgramScore = 0;
		float totalFullScore = 0;

		for (LanguageModel lm : languageModels) {

			if (lm.useable(this)) { // contains factors used by this LM
				float weightLM = lm.getWeight();
				Ref<Float> fullScore = new Ref<Float>(0.0f), nGramScore =  new Ref<Float>(0.0f);

				lm.calcScore(this, fullScore, nGramScore);
				fullScore.v = Util.untransformLMScore(fullScore.v);
				nGramScore.v = Util.untransformLMScore(nGramScore.v);

				m_scoreBreakdown.assign(lm, nGramScore.v);

				// total LM score so far
				totalNgramScore += nGramScore.v * weightLM;
				totalFullScore += fullScore.v * weightLM;
			}
		}

		// word penalty
		int wordCount = getNumTerminals();
		m_scoreBreakdown.assign(staticData.getWordPenaltyProducer(),
				-(float) wordCount * 0.434294482f); // TODO log -> ln ??

		m_fullScore = m_scoreBreakdown.getWeightedScore() - totalNgramScore + totalFullScore;
	}

	// used by for unknown word proc in chart decoding
	public void setScore(final ScoreProducer producer, final List<Float> scoreVector) {
		// used when creating translations of unknown words (chart decoding)
		m_scoreBreakdown.assign(producer, scoreVector);
		m_transScore = m_ngramScore = 0;
		m_fullScore = m_scoreBreakdown.getWeightedScore();
	}

	// used when creating translations of unknown words:
	public void resetScore() {
		m_fullScore = m_ngramScore = 0;
		m_scoreBreakdown.zeroAll();
	}

	public void setWeights(final ScoreProducer sp, final List<Float> weightT) {
		// calling this function in case of confusion net input is undefined
		ASSERT.a(StaticData.instance().getInputType() == InputTypeEnum.SentenceInput);

		/*
		 * one way to fix this, you have to make sure the weightT contains (in
		 * addition to the usual phrase translation scaling factors) the input
		 * weight factor as last element
		 */

		m_transScore = m_scoreBreakdown.partialInnerProduct(sp, weightT);
	}

	public TargetPhrase mergeNext(final TargetPhrase inputPhrase) {
		if (!isCompatible(inputPhrase)) {
			return null;
		}

		// ok, merge
		TargetPhrase clone = new TargetPhrase(this);
		clone.m_sourcePhrase = m_sourcePhrase;
		int currWord = 0;
		int len = getSize();
		for (int currPos = 0; currPos < len; currPos++) {
			final Word inputWord = inputPhrase.getWord(currPos);
			Word cloneWord = clone.getWord(currPos);
			cloneWord.merge(inputWord);

			currWord++;
		}

		return clone;
	}

	// used for translation step

	// #ifdef HAVE_PROTOBUF
	// void WriteToRulePB(hgmert::Rule pb) const;
	// #endif

	final float getTranslationScore() {
		return m_transScore;
	}

	/***
	 * return the estimated score resulting from our being added to a sentence
	 * (it's an estimate because we don't have full n-gram info for the language model
	 * without using the (unknown) full sentence)
	 */
	public final float getFutureScore() {
		return m_fullScore;
	}

	public final ScoreComponentCollection getScoreBreakdown() {
		return m_scoreBreakdown;
	}

	// ! TODO - why is this needed and is it set correctly by every phrase dictionary class ? should
	// be set in constructor
	public void setSourcePhrase(final Phrase p) {
		m_sourcePhrase = p;
	}

	public Phrase getSourcePhrase() {
		return m_sourcePhrase;
	}

	public void setTargetLHS(final Word lhs) {
		m_lhsTarget = lhs;
	}

	public final Word getTargetLHS() {
		return m_lhsTarget;
	}

	public void setAlignmentInfo(final String alignString) {
		List<Pair<Integer, Integer>> alignmentInfo = new ArrayList<Pair<Integer, Integer>>();
		String[] alignVec = Util.tokenize(alignString);
		for (String align1 : alignVec) {
			String[] alignPos = Util.tokenize(align1, "-");
			ASSERT.a(alignPos.length == 2);
			int sourcePos = Integer.valueOf(alignPos[0]);
			int targetPos = Integer.valueOf(alignPos[1]);

			alignmentInfo.add(new Pair<Integer, Integer>(sourcePos, targetPos));
		}

		setAlignmentInfo(alignmentInfo);
	}

	public void setAlignmentInfo(final List<Pair<Integer, Integer>> alignmentInfo) {
		m_alignmentInfo.addAlignment(alignmentInfo);
	}

	public AlignmentInfo getAlignmentInfo() {
		return m_alignmentInfo;
	}

	public void useWordAlignment(boolean a) {
		wordalignflag = a;
	};

	public boolean useWordAlignment() {
		return wordalignflag;
	};

	public void printAlignmentInfo(boolean a) {
		printalign = a;
	}

	public boolean printAlignmentInfo() {
		return printalign;
	}

	public void createCountInfo(final String countStr) {
		String[] count = Util.tokenize(countStr);
		ASSERT.a(count.length == 2);
		m_countInfo = new CountInfo(Float.valueOf(count[1]), Float.valueOf(count[0]));
	}

	// TO_STRING(){}
	public String toString() {
		StringBuilder sb = new StringBuilder(0);
		sb.append(super.toString()).append(":").append(this.getAlignmentInfo());
		sb.append(": pC=").append(this.m_transScore).append(", c=").append(this.m_fullScore);
		return sb.toString();
	}

}
