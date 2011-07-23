package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import yonee.utils.ASSERT;
import yonee.utils.CollectionUtils;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       set.remove 性能
 *       set.get == equals实现
 */
public class HypothesisStackCubePruning extends HypothesisStack {
	// friend std::ostream& operator<<(std::ostream&, const HypothesisStackCubePruning&);

	private static final long serialVersionUID = 1L;

	protected Map<WordsBitmap, BitmapContainer> m_bitmapAccessor;// BMType

	protected float m_bestScore;
	/** < score of the best hypothesis in collection */
	protected float m_worstScore;
	/** < score of the worse hypthesis in collection */
	protected float m_beamWidth;
	/** < minimum score due to threashold pruning */
	protected int m_maxHypoStackSize;
	/** < maximum number of hypothesis allowed in this stack */
	protected boolean m_nBestIsEnabled;

	/** < flag to determine whether to keep track of old arcs */

	/**
	 * add hypothesis to stack. Prune if necessary.
	 * Returns false if equiv hypo exists in collection, otherwise returns true
	 */
	public boolean add(Hypothesis hypo) {
		boolean ret = super.add(hypo);
		if (ret) { // equiv hypo doesn't exists
			VERBOSE.v(3, "added hyp to stack");

			// Update best score, if this hypothesis is new best
			if (hypo.getTotalScore() > m_bestScore) {
				VERBOSE.v(3, ", best on stack");
				m_bestScore = hypo.getTotalScore();
				// this may also affect the worst score
				if (m_bestScore + m_beamWidth > m_worstScore)
					m_worstScore = m_bestScore + m_beamWidth;
			}

			// Prune only if stack is twice as big as needed (lazy pruning)
			VERBOSE.v(3, ", now size " + size());
			if (size() > 2 * m_maxHypoStackSize - 1) {
				pruneToSize(m_maxHypoStackSize);
			} else {
				VERBOSE.v(3, "\n");
			}
		}

		return ret;
	}

	/** destroy all instances of Hypothesis in this collection */
	protected void removeAll() {
		m_bitmapAccessor.clear();
	}

	public HypothesisStackCubePruning(Manager manager) {
		super(manager);
		m_nBestIsEnabled = StaticData.instance().isNBestEnabled();
		m_bestScore = Float.NEGATIVE_INFINITY;
		m_worstScore = Float.NEGATIVE_INFINITY;
	}

	/**
	 * adds the hypo, but only if within thresholds (beamThr, stackSize).
	 * This function will recombine hypotheses silently! There is no record
	 * (could affect n-best list generation...TODO)
	 * Call stack for adding hypothesis is
	 * AddPrune()
	 * Add()
	 * AddNoPrune()
	 */
	public boolean addPrune(Hypothesis hypo) {
		if (hypo.getTotalScore() < m_worstScore) { // too bad for stack. don't bother adding hypo
			// into collection
			m_manager.getSentenceStats().addDiscarded();
			VERBOSE.v(3, "discarded, too bad for stack\n");

			return false;
		}

		// over threshold, try to add to collection
		boolean addRet = add(hypo);
		if (addRet) { // nothing found. add to collection
			return true;
		}

		// equiv hypo exists, recombine with other hypo
		// Iterator iterExisting = this.get
		Hypothesis hypoExisting = CollectionUtils.get(this, hypo);

		// assert(iterExisting != m_hypos.end());

		m_manager.getSentenceStats().addRecombination(hypo, hypoExisting);

		// found existing hypo with same target ending.
		// keep the best 1
		if (hypo.getTotalScore() > hypoExisting.getTotalScore()) { // incoming hypo is better than
			// the one we have
			VERBOSE.v(3, "better than matching hyp " + hypoExisting.getId() + ", recombining, ");
			if (m_nBestIsEnabled) {
				hypo.addArc(hypoExisting);
				detach(hypoExisting);
			} else {
				remove(hypoExisting);
			}

			boolean added = add(hypo);
			if (!added) {
				hypoExisting = CollectionUtils.get(this, hypo);
				TRACE.err("Offending hypo = " + hypoExisting + "\n");
				ASSERT.a(false);
			}
			return false;
		} else { // already storing the best hypo. discard current hypo
			VERBOSE.v(3, "worse than matching hyp " + hypoExisting.getId() + ", recombining\n");
			if (m_nBestIsEnabled) {
				hypoExisting.addArc(hypo);
			} else {
				hypo = null;
			}
			return false;
		}
	}

	public void addInitial(Hypothesis hypo) {
		boolean addRet = add(hypo);
		ASSERT.a(addRet);

		final WordsBitmap bitmap = hypo.getWordsBitmap();
		m_bitmapAccessor.put(bitmap, new BitmapContainer(bitmap, this));
	}

	/**
	 * set maximum number of hypotheses in the collection
	 * \param maxHypoStackSize maximum number (typical number: 100)
	 */
	public final void setMaxHypoStackSize(int maxHypoStackSize) {
		m_maxHypoStackSize = maxHypoStackSize;
	}

	public final int getMaxHypoStackSize() {
		return m_maxHypoStackSize;
	}

	/**
	 * set beam threshold, hypotheses in the stack must not be worse than
	 * this factor times the best score to be allowed in the stack
	 * \param beamThreshold minimum factor (typical number: 0.03)
	 */
	public final void setBeamWidth(float beamWidth) {
		m_beamWidth = beamWidth;
	}

	/** return score of the best hypothesis in the stack */
	public final float getBestScore() {
		return m_bestScore;
	}

	/** return worst score allowed for the stack */
	public final float getWorstScore() {
		return m_worstScore;
	}

	public void addHypothesesToBitmapContainers() {
		for (Hypothesis h : this) {
			final WordsBitmap bitmap = h.getWordsBitmap();
			BitmapContainer container = m_bitmapAccessor.get(bitmap);
			container.addHypothesis(h);
		}
	}

	public final Map<WordsBitmap, BitmapContainer> getBitmapAccessor() {
		return m_bitmapAccessor;
	}

	public void setBitmapAccessor(final WordsBitmap newBitmap, HypothesisStackCubePruning stack,
			final WordsRange range, BitmapContainer bitmapContainer,
			final SquareMatrix futureScore, final TranslationOptionList transOptList) {
		BitmapContainer bmContainer = m_bitmapAccessor.get(newBitmap);

		if (bmContainer == null) {
			bmContainer = new BitmapContainer(newBitmap, stack);
			m_bitmapAccessor.put(newBitmap, bmContainer);
		}
		BackwardsEdge edge = new BackwardsEdge(bitmapContainer, bmContainer, transOptList,
				futureScore, m_manager.getSource());
		bmContainer.addBackwardsEdge(edge);
	}

	/**
	 * pruning, if too large.
	 * Pruning algorithm: find a threshold and delete all hypothesis below it.
	 * The threshold is chosen so that exactly newSize top items remain on the
	 * stack in fact, in situations where some of the hypothesis fell below
	 * m_beamWidth, the stack will contain less items.
	 * \param newSize maximum size
	 */
	public void pruneToSize(int newSize) {
		if (size() > newSize) { // ok, if not over the limit
			PriorityQueue<Float> bestScores = new PriorityQueue<Float>();

			// push all scores to a heap
			// (but never push scores below m_bestScore+m_beamWidth)
			float score = 0;
			for (Hypothesis hypo : this) {
				score = hypo.getTotalScore();
				if (score > m_bestScore + m_beamWidth) {
					bestScores.add(score);
				}
			}

			// pop the top newSize scores (and ignore them, these are the scores of hyps that will
			// remain)
			// ensure to never pop beyond heap size
			int minNewSizeHeapSize = newSize > bestScores.size() ? bestScores.size() : newSize;
			for (int i = 1; i < minNewSizeHeapSize; i++)
				bestScores.poll();

			// and remember the threshold
			float scoreThreshold = bestScores.peek();

			// delete all hypos under score threshold

			for (Iterator<Hypothesis> iter = this.iterator(); iter.hasNext();) {
				Hypothesis hypo = iter.next();
				float score0 = hypo.getTotalScore();
				if (score0 < scoreThreshold) {
					Hypothesis iterRemove = iter.next();
					remove(iterRemove);
					m_manager.getSentenceStats().addPruning();
				} else {
					iter.next();
				}
			}
			VERBOSE.v(3, ", pruned to size " + size() + "\n");

			if (VERBOSE.v(3)) {
				TRACE.err("stack now contains: ");
				for (Hypothesis h : this) {
					TRACE.err(h.getId() + " (" + h.getTotalScore() + ") ");
				}
				TRACE.err("\n");
			}
			// set the worstScore, so that newly generated hypotheses will not be added if worse
			// than the worst in the stack
			m_worstScore = scoreThreshold;
		}
	}

	// ! return the hypothesis with best score. Used to get the translated at end of decoding
	public final Hypothesis getBestHypothesis() {
		if (!isEmpty()) {
			Hypothesis bestHypo = null;
			for (Hypothesis hypo : this) {
				if (bestHypo == null || hypo.getTotalScore() > bestHypo.getTotalScore())
					bestHypo = hypo;
			}
			return bestHypo;
		}
		return null;
	}

	// ! return all hypothesis, sorted by descending score. Used in creation of N best list
	public List<Hypothesis> getSortedList() {
		List<Hypothesis> ret = new ArrayList<Hypothesis>(size());
		ret.addAll(this);
		Collections.sort(ret, new Comparator<Hypothesis>() {
			public int compare(Hypothesis hypo1, Hypothesis hypo2) {
				return hypo1.getTotalScore() < hypo2.getTotalScore() ? -1 :1;
			}

		});
		return ret;
	}

	/**
	 * make all arcs in point to the equiv hypothesis that contains them.
	 * Ie update doubly linked list be hypo arcs
	 */
	public void cleanupArcList() {
		// only necessary if n-best calculations are enabled
		if (!m_nBestIsEnabled)
			return;
		for (Hypothesis hypo : this) {
			hypo.cleanupArcList();
		}
	}

	// TO_STRING(){}
}
