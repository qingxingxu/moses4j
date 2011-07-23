package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.utils.ASSERT;
import yonee.utils.Ref;

/**
 * 
 * @author YONEE
 * @OK<<
 */
public class TranslationOption {

	// friend std::ostream& operator<<(std::ostream& out, const TranslationOption&
	// possibleTranslation);

	protected TargetPhrase m_targetPhrase = new TargetPhrase(); /*
																 * < output phrase when using this
																 * translation option
																 */
	protected Phrase m_sourcePhrase; /* < input phrase translated by this */
	protected WordsRange m_sourceWordsRange = new WordsRange(); /*
																 * < word position in the input that
																 * are covered by this
																 * translation option
																 */
	protected float m_futureScore; /*
									 * < estimate of total cost when using this translation option,
									 * includes language model probabilities
									 */
	protected List<TranslationOption> m_linkedTransOpts; /*
														 * list of linked TOs which must be included
														 * with this in any hypothesis
														 */

	// ! in TranslationOption, m_scoreBreakdown is not complete. It cannot,
	// ! for example, know the full n-gram score since the length of the
	// ! TargetPhrase may be shorter than the n-gram order. But, if it is
	// ! possible to estimate, it is included here.
	protected ScoreComponentCollection m_scoreBreakdown = new ScoreComponentCollection();

	protected Map<ScoreProducer, List<Float>> m_cachedScores = new HashMap<ScoreProducer, List<Float>>();// _ScoreCacheMap

	/** constructor. Used by initial translation step */
	public TranslationOption(final WordsRange wordsRange, final TargetPhrase targetPhrase,
			final InputType inputType) {
		m_targetPhrase = targetPhrase;
		m_sourceWordsRange = wordsRange;
		// set score
		m_scoreBreakdown.plusEquals(targetPhrase.getScoreBreakdown());

		if (inputType.getType() == InputTypeEnum.SentenceInput) {
			Phrase phrase = inputType.getSubString(wordsRange);
			m_sourcePhrase = new Phrase(phrase);
		} else { // TODO lex reordering with confusion network
			m_sourcePhrase = new Phrase(targetPhrase.getSourcePhrase());
		}
	}

	/** constructor. Used to create trans opt from unknown word */
	public TranslationOption(final WordsRange wordsRange, final TargetPhrase targetPhrase,
			final InputType inputType, int whatever) {
		m_targetPhrase = targetPhrase;
		m_sourceWordsRange = wordsRange;
		m_futureScore = 0;
		final UnknownWordPenaltyProducer up = StaticData.instance().getUnknownWordPenaltyProducer();
		if (up != null) {
			final ScoreProducer scoreProducer = (ScoreProducer) up; // not sure why none of the c++
			// cast works
			List<Float> score = new ArrayList<Float>(1);
			score.add(0, Util.floorScore(Float.NEGATIVE_INFINITY));
			m_scoreBreakdown.assign(scoreProducer, score);
		}

		if (inputType.getType() == InputTypeEnum.SentenceInput) {
			Phrase phrase = inputType.getSubString(wordsRange);
			m_sourcePhrase = new Phrase(phrase);
		} else { // TODO lex reordering with confusion network
			m_sourcePhrase = new Phrase(targetPhrase.getSourcePhrase());
			// the target phrase from a confusion network/lattice has input scores that we want to
			// keep
			m_scoreBreakdown.plusEquals(targetPhrase.getScoreBreakdown());

		}
	}

	/** copy constructor */
	public TranslationOption(final TranslationOption copy) {
		m_targetPhrase = copy.m_targetPhrase;
		m_sourcePhrase = (copy.m_sourcePhrase == null) ? new Phrase(FactorDirection.Input)
				: new Phrase(copy.m_sourcePhrase);
		m_sourceWordsRange = copy.m_sourceWordsRange;
		m_futureScore = copy.m_futureScore;
		m_scoreBreakdown = copy.m_scoreBreakdown;
		m_cachedScores = copy.m_cachedScores;
	}

	/** copy constructor, but change words range. used by caching */
	public TranslationOption(final TranslationOption copy, final WordsRange sourceWordsRange) {
		m_targetPhrase = (copy.m_targetPhrase);
		m_sourcePhrase = (copy.m_sourcePhrase == null) ? new Phrase(FactorDirection.Input)
				: new Phrase(copy.m_sourcePhrase);
		m_sourceWordsRange = (sourceWordsRange);
		m_futureScore = (copy.m_futureScore);
		m_scoreBreakdown = (copy.m_scoreBreakdown);
		m_cachedScores = (copy.m_cachedScores);
	}

	/** returns true if all feature types in featuresToCheck are compatible between the two phrases */
	public boolean isCompatible(final Phrase phrase, final int[] featuresToCheck) {
		if (featuresToCheck.length == 1) {
			return m_targetPhrase.isCompatible(phrase, featuresToCheck[0]);
		} else if (featuresToCheck != null) {
			return true;
			/* features already there, just update score */
		} else {
			return m_targetPhrase.isCompatible(phrase, featuresToCheck);
		}
	}

	/** used when precomputing (composing) translation options */
	public void mergeNewFeatures(final Phrase phrase, final ScoreComponentCollection score,
			final int[] featuresToAdd) {
		ASSERT.a(phrase.getSize() == m_targetPhrase.getSize());
		if (featuresToAdd.length == 1) {
			m_targetPhrase.mergeFactors(phrase, featuresToAdd[0]);
		} else if (featuresToAdd != null) {
			/* features already there, just update score */
		} else {
			m_targetPhrase.mergeFactors(phrase, featuresToAdd);
		}
		m_scoreBreakdown.plusEquals(score);
	}

	/** returns target phrase */
	public final TargetPhrase getTargetPhrase() {
		return m_targetPhrase;
	}

	/** returns source word range */
	public final WordsRange getSourceWordsRange() {
		return m_sourceWordsRange;
	}

	/** returns source phrase */
	public final Phrase getSourcePhrase() {
		return m_sourcePhrase;
	}

	/** returns linked TOs */
	public final List<TranslationOption> getLinkedTransOpts() {
		return m_linkedTransOpts;
	}

	/** add link to another TO */
	public final void addLinkedTransOpt(TranslationOption to) {
		m_linkedTransOpts.add(to);
	}

	/** whether source span overlaps with those of a hypothesis */
	public boolean overlap(final Hypothesis hypothesis) {
		final WordsBitmap bitmap = hypothesis.getWordsBitmap();
		return bitmap.overlap(getSourceWordsRange());
	}

	/** return start index of source phrase */
	public final int getStartPos() {
		return m_sourceWordsRange.getStartPos();
	}

	/** return end index of source phrase */
	public final int getEndPos() {
		return m_sourceWordsRange.getEndPos();
	}

	/** return length of source phrase */
	public final int getSize() {
		return m_sourceWordsRange.getEndPos() - m_sourceWordsRange.getStartPos() + 1;
	}

	/** return estimate of total cost of this option */
	public final float getFutureScore() {
		return m_futureScore;
	}

	/** return true if the source phrase translates into nothing */
	public final boolean isDeletionOption() {
		return m_targetPhrase.getSize() == 0;
	}

	/** returns detailed component scores */
	public final ScoreComponentCollection getScoreBreakdown() {
		return m_scoreBreakdown;
	}

	/** returns cached scores */
	public final List<Float> getCachedScores(final ScoreProducer scoreProducer) {
		return m_cachedScores.get(scoreProducer);

	}

	/** Calculate future score and n-gram score of this trans option, plus the score breakdowns */
	public void calcScore() {
		// LM scores

		Ref<Float> ngramScore = new Ref<Float>(0.0f), retFullScore = new Ref<Float>(0.0f);

		final LMList allLM = StaticData.instance().getAllLM();

		allLM.calcScore(getTargetPhrase(), retFullScore, ngramScore, m_scoreBreakdown);

		int phraseSize = getTargetPhrase().getSize();
		// future score
		m_futureScore = retFullScore.v - ngramScore.v
				+ m_scoreBreakdown.innerProduct(StaticData.instance().getAllWeights()) - phraseSize
				* StaticData.instance().getWeightWordPenalty();
	}

	public void cacheScores(final ScoreProducer producer, final List<Float> score) {
		m_cachedScores.put(producer, new ArrayList<Float>(score));
	}

	// TO_STRING(){}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getTargetPhrase()).append(" c=").append(getFutureScore()).append(" [").append(
				getSourceWordsRange()).append("]").append(getScoreBreakdown());
		return sb.toString();
	}

}
