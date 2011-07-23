package yonee.moses4j.moses;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       PriorityQueue 比较问题
 */
public class SearchCubePruning extends Search {

	class BitmapContainerOrderer implements Comparator<BitmapContainer> {
		public int compare(BitmapContainer A, BitmapContainer B) {

			if (B.empty()) {
				if (A.empty()) {
					return A.hashCode() - B.hashCode();
				}
				return 0;
			}
			if (A.empty()) {
				return 1;
			}

			// Compare the top hypothesis of each bitmap container using the TotalScore, which
			// includes future cost
			final float scoreA = A.top().getHypothesis().getTotalScore();
			final float scoreB = B.top().getHypothesis().getTotalScore();

			if (scoreA < scoreB) {
				return -1;
			} else if (scoreA > scoreB) {
				return 1;
			} else {
				return A.hashCode() - B.hashCode();
			}
		}
	}

	protected InputType m_source;
	protected HypothesisStack[] m_hypoStackColl;
	/** < stacks to store hypotheses (partial translations) */
	// no of elements = no of words in source + 1
	protected TargetPhrase m_initialTargetPhrase;
	/** < used to seed 1st hypo */
	protected long m_start;
	/** < used to track time spend on translation */
	protected TranslationOptionCollection m_transOptColl;

	/** < pre-computed list of translation options for the phrases in this sentence */

	// ! go thru all bitmaps in 1 stack & create backpointers to bitmaps in the stack
	protected void createForwardTodos(HypothesisStackCubePruning stack) {
		final Map<WordsBitmap, BitmapContainer> bitmapAccessor = stack.getBitmapAccessor();
		int size = m_source.getSize();
		stack.addHypothesesToBitmapContainers();

		for (Entry<WordsBitmap, BitmapContainer> e : bitmapAccessor.entrySet()) {
			final WordsBitmap bitmap = e.getKey();
			BitmapContainer bitmapContainer = e.getValue();

			if (bitmapContainer.getHypothesesSize() == 0) { // no hypothese to expand. don't bother
				// doing it
				continue;
			}

			// Sort the hypotheses inside the Bitmap Container as they are being used by now.
			bitmapContainer.sortHypotheses();

			// check bitamp and range doesn't overlap
			int startPos, endPos;
			for (startPos = 0; startPos < size; startPos++) {
				if (bitmap.getValue(startPos))
					continue;

				// not yet covered
				WordsRange applyRange = new WordsRange(startPos, startPos);
				if (checkDistortion(bitmap, applyRange)) { // apply range
					createForwardTodos(bitmap, applyRange, bitmapContainer);
				}

				int maxSize = size - startPos;
				int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
				maxSize = Math.min(maxSize, maxSizePhrase);

				for (endPos = startPos + 1; endPos < startPos + maxSize; endPos++) {
					if (bitmap.getValue(endPos))
						break;

					WordsRange applyRange0 = new WordsRange(startPos, endPos);
					if (checkDistortion(bitmap, applyRange0)) { // apply range
						createForwardTodos(bitmap, applyRange0, bitmapContainer);
					}
				}
			}
		}
	}

	// ! create a back pointer to this bitmap, with edge that has this words range translation
	protected void createForwardTodos(final WordsBitmap bitmap, final WordsRange range,
			BitmapContainer bitmapContainer) {
		WordsBitmap newBitmap = bitmap;
		newBitmap.setValue(range.getStartPos(), range.getEndPos(), true);

		int numCovered = newBitmap.getNumWordsCovered();
		final TranslationOptionList transOptList = m_transOptColl.getTranslationOptionList(range);
		final SquareMatrix futureScore = m_transOptColl.getFutureScore();

		if (transOptList.size() > 0) {
			HypothesisStackCubePruning newStack = (HypothesisStackCubePruning) (m_hypoStackColl[numCovered]);
			newStack.setBitmapAccessor(newBitmap, newStack, range, bitmapContainer, futureScore,
					transOptList);
		}
	}

	protected boolean checkDistortion(final WordsBitmap hypoBitmap, final WordsRange range) {
		// since we check for reordering limits, its good to have that limit handy
		int maxDistortion = StaticData.instance().getMaxDistortion();

		// if there are reordering limits, make sure it is not violated
		// the coverage bitmap is handy here (and the position of the first gap)
		final int hypoFirstGapPos = hypoBitmap.getFirstGapPos(), startPos = range.getStartPos(), endPos = range
				.getEndPos();

		// if reordering constraints are used (--monotone-at-punctuation or xml), check if passes
		// all
		if (!m_source.i().getReorderingConstraint().check(hypoBitmap, startPos, endPos)) {
			return false;
		}

		// no limit of reordering: no problem
		if (maxDistortion < 0) {
			return true;
		}

		boolean leftMostEdge = (hypoFirstGapPos == startPos);
		// any length extension is okay if starting at left-most edge
		if (leftMostEdge) {
			return true;
		}
		// starting somewhere other than left-most edge, use caution
		// the basic idea is this: we would like to translate a phrase starting
		// from a position further right than the left-most open gap. The
		// distortion penalty for the following phrase will be computed relative
		// to the ending position of the current extension, so we ask now what
		// its maximum value will be (which will always be the value of the
		// hypothesis starting at the left-most edge). If this vlaue is than
		// the distortion limit, we don't allow this extension to be made.
		WordsRange bestNextExtension = new WordsRange(hypoFirstGapPos, hypoFirstGapPos);
		int required_distortion = m_source.i().computeDistortionDistance(range, bestNextExtension);

		if (required_distortion > maxDistortion) {
			return false;
		}
		return true;
	}

	protected void printBitmapContainerGraph() {
		HypothesisStackCubePruning lastStack = (HypothesisStackCubePruning) (m_hypoStackColl[m_hypoStackColl.length - 1]);
		final Map<WordsBitmap, BitmapContainer> bitmapAccessor = lastStack.getBitmapAccessor();

		for (Entry<WordsBitmap, BitmapContainer> iterAccessor : bitmapAccessor.entrySet()) {
			System.err.println(iterAccessor.getKey());
		}
	}

	public SearchCubePruning(Manager manager, final InputType source,
			final TranslationOptionCollection transOptColl) {
		super(manager);
		m_source = source;
		m_hypoStackColl = new HypothesisStack[source.getSize() + 1];
		m_initialTargetPhrase = new TargetPhrase(FactorDirection.Output,
				source.i().m_initialTargetPhrase);
		m_start = (System.currentTimeMillis());
		m_transOptColl = (transOptColl);
		final StaticData staticData = StaticData.instance();

		/*
		 * constraint search not implemented in cube pruning
		 * long sentenceID = source.getTranslationId();
		 * m_constraint = staticData.getConstrainingPhrase(sentenceID);
		 */

		for (int ind = 0; ind < m_hypoStackColl.length; ++ind) {
			HypothesisStackCubePruning sourceHypoColl = new HypothesisStackCubePruning(m_manager);
			sourceHypoColl.setMaxHypoStackSize(staticData.getMaxHypoStackSize());
			sourceHypoColl.setBeamWidth(staticData.getBeamWidth());

			m_hypoStackColl[ind] = sourceHypoColl;
		}
	}

	public void processSentence() {
		final StaticData staticData = StaticData.instance();

		// initial seed hypothesis: nothing translated, no words produced
		Hypothesis hypo = Hypothesis.create(m_manager, m_source, m_initialTargetPhrase);

		HypothesisStackCubePruning firstStack = (HypothesisStackCubePruning) (m_hypoStackColl[0]);
		firstStack.addInitial(hypo);
		// Call this here because the loop below starts at the second stack.
		firstStack.cleanupArcList();
		createForwardTodos(firstStack);

		final int PopLimit = StaticData.instance().getCubePruningPopLimit();
		VERBOSE.v(3, "Cube Pruning pop limit is " + PopLimit + "\n");

		final int Diversity = StaticData.instance().getCubePruningDiversity();
		VERBOSE.v(3, "Cube Pruning diversity is " + Diversity + "\n");

		// go through each stack
		int stackNo = 1;
		for (HypothesisStack iterStack : m_hypoStackColl) {
			// check if decoding ran out of time
			double _elapsed_time = Util.getUserTime();
			if (_elapsed_time > staticData.getTimeoutThreshold()) {
				VERBOSE.v(1, "Decoding is out of time (" + _elapsed_time + ","
						+ staticData.getTimeoutThreshold() + ")" + "\n");
				return;
			}
			HypothesisStackCubePruning sourceHypoColl = (HypothesisStackCubePruning) iterStack;

			// Map<WordsBitmap, BitmapContainer> ::const_iterator bmIter;
			final Map<WordsBitmap, BitmapContainer> accessor = sourceHypoColl.getBitmapAccessor();

			// priority queue which has a single entry for each bitmap container, sorted by score of
			// top hyp
			PriorityQueue<BitmapContainer> BCQueue = new PriorityQueue<BitmapContainer>(accessor
					.size(), new BitmapContainerOrderer());

			for (Entry<WordsBitmap, BitmapContainer> bmIter : accessor.entrySet()) {
				bmIter.getValue().initializeEdges();
				BCQueue.add(bmIter.getValue());

			}

			// main search loop, pop k best hyps
			for (int numpops = 1; numpops <= PopLimit && !BCQueue.isEmpty(); numpops++) {
				BitmapContainer bc = BCQueue.peek();
				BCQueue.poll();
				bc.processBestHypothesis();
				if (!bc.empty())
					BCQueue.add(bc);
			}

			// ensure diversity, a minimum number of inserted hyps for each bitmap container;
			// NOTE: diversity doesn't ensure they aren't pruned at some later point
			if (Diversity > 0) {
				for (Entry<WordsBitmap, BitmapContainer> bmIter : accessor.entrySet()) {
					bmIter.getValue().ensureMinStackHyps(Diversity);
				}
			}

			// the stack is pruned before processing (lazy pruning):
			VERBOSE.v(3, "processing hypothesis from next stack");
			// VERBOSE.v("processing next stack at ");
			sourceHypoColl.pruneToSize(staticData.getMaxHypoStackSize());
			VERBOSE.v(3, "\n");
			sourceHypoColl.cleanupArcList();

			createForwardTodos(sourceHypoColl);

			stackNo++;
		}

		printBitmapContainerGraph();

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

	public void OutputHypoStack(int stack) {
		if (stack >= 0) {
			TRACE.err("Stack " + stack + ": " + "\n" + m_hypoStackColl[stack] + "\n");
		} else { // all stacks
			int i = 0;
			for (HypothesisStack iterStack : m_hypoStackColl) {
				HypothesisStackCubePruning hypoColl = (HypothesisStackCubePruning) (iterStack);
				TRACE.err("Stack " + i++ + ": " + "\n" + hypoColl + "\n");
			}
		}
	}

	public HypothesisStack[] getHypothesisStacks() {
		return m_hypoStackColl;
	}

	public Hypothesis getBestHypothesis() {
		// final HypothesisStackCubePruning hypoColl = m_hypoStackColl.back();
		final HypothesisStack hypoColl = m_hypoStackColl[m_hypoStackColl.length - 1];
		return hypoColl.getBestHypothesis();
	}

}
