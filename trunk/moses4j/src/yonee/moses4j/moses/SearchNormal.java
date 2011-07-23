package yonee.moses4j.moses;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK
 */
public class SearchNormal extends Search {

	protected InputType m_source;
	protected HypothesisStack m_hypoStackColl[];
	/** < stacks to store hypotheses (partial translations) */
	// no of elements = no of words in source + 1
	protected TargetPhrase m_initialTargetPhrase;
	/** < used to seed 1st hypo */
	protected long m_start;
	/** < starting time, used for logging */
	protected int interrupted_flag;
	/** < flag indicating that decoder ran out of time (see switch -time-out) */
	protected HypothesisStackNormal actual_hypoStack;
	/** actual (full expanded) stack of hypotheses */
	protected TranslationOptionCollection m_transOptColl;

	/** < pre-computed list of translation options for the phrases in this sentence */

	// functions for creating hypotheses
	protected void processOneHypothesis(final Hypothesis hypothesis) {
		// since we check for reordering limits, its good to have that limit handy
		int maxDistortion = StaticData.instance().getMaxDistortion();
		boolean isWordLattice = StaticData.instance().getInputType() == InputTypeEnum.WordLatticeInput;

		// no limit of reordering: only check for overlap
		if (maxDistortion < 0) {
			final WordsBitmap hypoBitmap = new WordsBitmap(hypothesis.getWordsBitmap());
			final int hypoFirstGapPos = hypoBitmap.getFirstGapPos(), sourceSize = m_source
					.getSize();

			for (int startPos = hypoFirstGapPos; startPos < sourceSize; ++startPos) {
				int maxSize = sourceSize - startPos;
				int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
				maxSize = (maxSize < maxSizePhrase) ? maxSize : maxSizePhrase;

				for (int endPos = startPos; endPos < startPos + maxSize; ++endPos) {
					// basic checks
					// there have to be translation options
					if (m_transOptColl.getTranslationOptionList(new WordsRange(startPos, endPos))
							.size() == 0
							||
							// no overlap with existing words
							hypoBitmap.overlap(new WordsRange(startPos, endPos)) ||
							// specified reordering constraints (set with -monotone-at-punctuation
							// or xml)
							!m_source.i().getReorderingConstraint().check(hypoBitmap, startPos,
									endPos)) {
						continue;
					}

					// TODO: does this method include incompatible WordLattice hypotheses?
					expandAllHypotheses(hypothesis, startPos, endPos);
				}
			}

			return; // done with special case (no reordering limit)
		}

		// if there are reordering limits, make sure it is not violated
		// the coverage bitmap is handy here (and the position of the first gap)
		final WordsBitmap hypoBitmap = new WordsBitmap(hypothesis.getWordsBitmap());
		final int hypoFirstGapPos = hypoBitmap.getFirstGapPos(), sourceSize = m_source.getSize();

		// MAIN LOOP. go through each possible range
		for (int startPos = hypoFirstGapPos; startPos < sourceSize; ++startPos) {
			// don't bother expanding phrases if the first position is already taken
			if (hypoBitmap.getValue(startPos))
				continue;

			WordsRange prevRange = hypothesis.getCurrSourceWordsRange();

			int maxSize = sourceSize - startPos;
			int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
			maxSize = (maxSize < maxSizePhrase) ? maxSize : maxSizePhrase;
			int closestLeft = hypoBitmap.getEdgeToTheLeftOf(startPos);
			if (isWordLattice) {
				// first question: is there a path from the closest translated word to the left
				// of the hypothesized extension to the start of the hypothesized extension?
				// long version: is there anything to our left? is it farther left than where we're
				// starting anyway? can we get to it?
				// closestLeft is exclusive: a value of 3 means 2 is covered, our arc is currently
				// ENDING at 3 and can start at 3 implicitly
				if (closestLeft != 0 && closestLeft != startPos
						&& !m_source.i().canIGetFromAToB(closestLeft, startPos)) {
					continue;
				}
				if (prevRange.getStartPos() != TypeDef.NOT_FOUND
						&& prevRange.getStartPos() > startPos
						&& !m_source.i().canIGetFromAToB(startPos, prevRange.getStartPos())) {
					continue;
				}
			}

			WordsRange currentStartRange = new WordsRange(startPos, startPos);
			if (m_source.i().computeDistortionDistance(prevRange, currentStartRange) > maxDistortion)
				continue;

			for (int endPos = startPos; endPos < startPos + maxSize; ++endPos) {
				// basic checks
				WordsRange extRange = new WordsRange(startPos, endPos);
				// there have to be translation options
				if (m_transOptColl.getTranslationOptionList(extRange).size() == 0
						||
						// no overlap with existing words
						hypoBitmap.overlap(extRange)
						||
						// specified reordering constraints (set with -monotone-at-punctuation or
						// xml)
						!m_source.i().getReorderingConstraint().check(hypoBitmap, startPos, endPos)
						|| //
						// connection in input word lattice
						(isWordLattice && !m_source.i().isCoveragePossible(extRange))) {
					continue;
				}

				// ask second question here:
				// we already know we can get to our starting point from the closest thing to the
				// left. We now ask the follow up:
				// can we get from our end to the closest thing on the right?
				// long version: is anything to our right? is it farther right than our (inclusive)
				// end? can our end reach it?
				boolean leftMostEdge = (hypoFirstGapPos == startPos);

				// closest right definition:
				int closestRight = hypoBitmap.getEdgeToTheRightOf(endPos);
				if (isWordLattice) {
					// if (!leftMostEdge && closestRight != endPos && closestRight != sourceSize &&
					// !m_source.CanIGetFromAToB(endPos, closestRight + 1)) {
					if (closestRight != endPos && ((closestRight + 1) < sourceSize)
							&& !m_source.i().canIGetFromAToB(endPos + 1, closestRight + 1)) {
						continue;
					}
				}

				// any length extension is okay if starting at left-most edge
				if (leftMostEdge) {
					expandAllHypotheses(hypothesis, startPos, endPos);
				}
				// starting somewhere other than left-most edge, use caution
				else {
					// the basic idea is this: we would like to translate a phrase starting
					// from a position further right than the left-most open gap. The
					// distortion penalty for the following phrase will be computed relative
					// to the ending position of the current extension, so we ask now what
					// its maximum value will be (which will always be the value of the
					// hypothesis starting at the left-most edge). If this value is less than
					// the distortion limit, we don't allow this extension to be made.
					WordsRange bestNextExtension = new WordsRange(hypoFirstGapPos, hypoFirstGapPos);
					int required_distortion = m_source.i().computeDistortionDistance(extRange,
							bestNextExtension);

					if (required_distortion > maxDistortion) {
						continue;
					}

					// everything is fine, we're good to go
					expandAllHypotheses(hypothesis, startPos, endPos);

				}
			}
		}
	}

	protected void expandAllHypotheses(final Hypothesis hypothesis, int startPos, int endPos) {
		float expectedScore = 0.0f;
		if (StaticData.instance().useEarlyDiscarding()) {
			// expected score is based on score of current hypothesis
			expectedScore = hypothesis.getScore();

			// add new future score estimate
			expectedScore += m_transOptColl.getFutureScore().calcFutureScore(
					hypothesis.getWordsBitmap(), startPos, endPos);
		}

		// loop through all translation options
		TranslationOptionList transOptList = m_transOptColl
				.getTranslationOptionList(new WordsRange(startPos, endPos));

		for (TranslationOption iter : transOptList) {
			expandHypothesis(hypothesis, iter, expectedScore);
		}
	}

	protected void expandHypothesis(final Hypothesis hypothesis, final TranslationOption transOpt,
			float expectedScore) {
		final StaticData staticData = StaticData.instance();
		SentenceStats stats = m_manager.getSentenceStats();
		long t = 0; // used to track time for steps

		Hypothesis newHypo;
		if (!staticData.useEarlyDiscarding()) {
			// simple build, no questions asked
			if (VERBOSE.v(2)) {
				t = System.currentTimeMillis();
			}
			newHypo = hypothesis.createNext(transOpt, m_constraint);
			if (VERBOSE.v(2)) {
				stats.addTimeBuildHyp(System.currentTimeMillis() - t);
			}
			if (newHypo == null)
				return;
			newHypo.calcScore(m_transOptColl.getFutureScore());
		} else
		// early discarding: check if hypothesis is too bad to build
		{
			// worst possible score may have changed . recompute
			int wordsTranslated = hypothesis.getWordsBitmap().getNumWordsCovered()
					+ transOpt.getSize();
			float allowedScore = m_hypoStackColl[wordsTranslated].getWorstScore();
			if (staticData.getMinHypoStackDiversity() != 0) {
				long id = hypothesis.getWordsBitmap().getIDPlus(transOpt.getStartPos(),
						transOpt.getEndPos());
				float allowedScoreForBitmap = m_hypoStackColl[wordsTranslated]
						.getWorstScoreForBitmap(id);
				allowedScore = Math.min(allowedScore, allowedScoreForBitmap);
			}
			allowedScore += staticData.getEarlyDiscardingThreshold();

			// add expected score of translation option
			expectedScore += transOpt.getFutureScore();
			// TRACE_ERR("EXPECTED diff: " << (newHypo.GetTotalScore()-expectedScore) << " (pre " <<
			// (newHypo.GetTotalScore()-expectedScorePre) << ") " << hypothesis.getTargetPhrase() <<
			// " ... " << transOpt.getTargetPhrase() << " [" << expectedScorePre << "," <<
			// expectedScore << "," << newHypo.GetTotalScore() << "]" << endl);

			// check if transOpt score push it already below limit
			if (expectedScore < allowedScore) {
				if (VERBOSE.v(2)) {
					stats.addNotBuilt();
				}
				return;
			}

			// build the hypothesis without scoring
			if (VERBOSE.v(2)) {
				t = System.currentTimeMillis();
			}
			newHypo = hypothesis.createNext(transOpt, m_constraint);
			if (newHypo == null)
				return;
			if (VERBOSE.v(2)) {
				stats.addTimeBuildHyp(System.currentTimeMillis() - t);
			}

			// compute expected score (all but correct LM)
			expectedScore = newHypo.calcExpectedScore(m_transOptColl.getFutureScore());
			// ... and check if that is below the limit
			if (expectedScore < allowedScore) {
				if (VERBOSE.v(2)) {
					stats.addEarlyDiscarded();
				}
				newHypo = null;
				return;
			}

			// ok, all is good, compute remaining scores
			newHypo.calcRemainingScore();

		}

		// logging for the curious
		if (VERBOSE.v(3)) {
			newHypo.printHypothesis();
		}

		// add to hypothesis stack
		int wordsTranslated = newHypo.getWordsBitmap().getNumWordsCovered();
		if (VERBOSE.v(2)) {
			t = System.currentTimeMillis();
		}
		m_hypoStackColl[wordsTranslated].addPrune(newHypo);
		if (VERBOSE.v(2)) {
			stats.addTimeStack(System.currentTimeMillis() - t);
		}
	}

	public SearchNormal(Manager manager, final InputType source,
			final TranslationOptionCollection transOptColl) {
		super(manager);
		m_source = source;
		m_hypoStackColl = new HypothesisStack[source.getSize() + 1];
		m_initialTargetPhrase = new TargetPhrase(FactorDirection.Output,
				source.i().m_initialTargetPhrase);
		m_start = System.currentTimeMillis();
		interrupted_flag = 0;
		m_transOptColl = transOptColl;
		VERBOSE.v(1, "Translating: " + m_source + "\n");
		final StaticData staticData = StaticData.instance();

		if (m_initialTargetPhrase.getSize() > 0) {
			VERBOSE.v(1, "Search extends partial output: " + m_initialTargetPhrase + "\n");
		}

		// only if constraint decoding (having to match a specified output)
		long sentenceID = source.i().getTranslationId();
		m_constraint = staticData.getConstrainingPhrase(sentenceID);
		if (m_constraint != null) {
			VERBOSE.v(1, "Search constraint to output: " + m_constraint + "\n");
		}

		// initialize the stacks: create data structure and set limits
		for (int ind = 0; ind < m_hypoStackColl.length; ++ind) {
			HypothesisStackNormal sourceHypoColl = new HypothesisStackNormal(m_manager);
			sourceHypoColl.setMaxHypoStackSize(staticData.getMaxHypoStackSize(), staticData
					.getMinHypoStackDiversity());
			sourceHypoColl.setBeamWidth(staticData.getBeamWidth());

			m_hypoStackColl[ind] = sourceHypoColl;
		}
	}

	public void processSentence() {
		final StaticData staticData = StaticData.instance();
		SentenceStats stats = m_manager.getSentenceStats();
		long t = 0; // used to track time for steps

		// initial seed hypothesis: nothing translated, no words produced
		Hypothesis hypo = Hypothesis.create(m_manager, m_source, m_initialTargetPhrase);
		m_hypoStackColl[0].addPrune(hypo);

		// go through each stack
		for (HypothesisStack iterStack : m_hypoStackColl) {
			// check if decoding ran out of time
			double _elapsed_time = Util.getUserTime();
			if (_elapsed_time > staticData.getTimeoutThreshold()) {
				VERBOSE.v(1, "Decoding is out of time (" + _elapsed_time + ","
						+ staticData.getTimeoutThreshold() + ")" + "\n");
				interrupted_flag = 1;
				return;
			}
			HypothesisStackNormal sourceHypoColl = (HypothesisStackNormal) iterStack;

			// the stack is pruned before processing (lazy pruning):
			VERBOSE.v(3, "processing hypothesis from next stack");
			if (VERBOSE.v(2)) {
				t = System.currentTimeMillis();
			}
			sourceHypoColl.pruneToSize(staticData.getMaxHypoStackSize());
			VERBOSE.v(3, "\n");
			sourceHypoColl.cleanupArcList();
			if (VERBOSE.v(2)) {
				stats.addTimeStack(System.currentTimeMillis() - t);
			}

			// go through each hypothesis on the stack and try to expand it
			for (Hypothesis hypothesis : sourceHypoColl) {
				processOneHypothesis(hypothesis); // expand the hypothesis
			}
			// some logging
			if (VERBOSE.v(2)) {
				outputHypoStackSize();
			}

			// this stack is fully expanded;
			actual_hypoStack = sourceHypoColl;
		}

		// some more logging
		if (VERBOSE.v(2)) {
			m_manager.getSentenceStats().setTimeTotal(System.currentTimeMillis() - m_start);
		}
		VERBOSE.v(2, m_manager.getSentenceStats() + "");
	}

	public void outputHypoStackSize() {
		TRACE.err("Stack sizes: " + m_hypoStackColl.length);
		for (HypothesisStack iterStack : m_hypoStackColl) {
			TRACE.err(", " + iterStack.size());
		}
		TRACE.err("\n");
	}

	public void outputHypoStack(int stack) {
	}

	public HypothesisStack[] getHypothesisStacks() {
		return m_hypoStackColl;
	}

	public Hypothesis getBestHypothesis() {
		if (interrupted_flag == 0) {
			final HypothesisStackNormal hypoColl = (HypothesisStackNormal) (m_hypoStackColl[m_hypoStackColl.length - 1]);
			return hypoColl.getBestHypothesis();
		} else {
			final HypothesisStackNormal hypoColl = actual_hypoStack;
			return hypoColl.getBestHypothesis();
		}
	}
}
