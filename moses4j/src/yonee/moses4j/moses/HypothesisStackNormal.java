package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yonee.utils.ASSERT;
import yonee.utils.CollectionUtils;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       ColletionUtils.get
 */
public class HypothesisStackNormal extends HypothesisStack {

	// public:
	// friend std::ostream& operator<<(std::ostream&, const HypothesisStackNormal&);

	private static final long serialVersionUID = 1L;

	protected float m_bestScore;
	/** < score of the best hypothesis in collection */
	protected float m_worstScore;
	/** < score of the worse hypothesis in collection */
	protected Map<Long, Float> m_diversityWorstScore;
	/** < score of worst hypothesis for particular source word coverage */
	protected float m_beamWidth;
	/** < minimum score due to threashold pruning */
	protected int m_maxHypoStackSize;
	/** < maximum number of hypothesis allowed in this stack */
	protected int m_minHypoStackDiversity;
	/** < minimum number of hypothesis with different source word coverage */
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
			// update best/worst score for stack diversity 1
			if (m_minHypoStackDiversity == 1
					&& hypo.getTotalScore() > getWorstScoreForBitmap(hypo.getWordsBitmap())) {
				setWorstScoreForBitmap(hypo.getWordsBitmap().getID(), hypo.getTotalScore());
			}

			VERBOSE.v(3, ", now size " + size());

			// prune only if stack is twice as big as needed (lazy pruning)
			int toleratedSize = 2 * m_maxHypoStackSize - 1;
			// add in room for stack diversity
			if (m_minHypoStackDiversity != 0)
				toleratedSize += m_minHypoStackDiversity << StaticData.instance()
						.getMaxDistortion();
			if (size() > toleratedSize) {
				pruneToSize(m_maxHypoStackSize);
			} else {
				VERBOSE.v(3, "\n");
			}
		}

		return ret;
	}

	/** destroy all instances of Hypothesis in this collection */
	protected void removeAll() {
		clear();
	}

	protected void setWorstScoreForBitmap(long id, float worstScore) {
		m_diversityWorstScore.put(id, worstScore);
	}

	public float getWorstScoreForBitmap(long id) {
		if (m_diversityWorstScore.get(id) == null)
			return Float.NEGATIVE_INFINITY;
		return m_diversityWorstScore.get(id);
	}

	public float getWorstScoreForBitmap(final WordsBitmap coverage) {
		return getWorstScoreForBitmap(coverage.getID());
	}

	public HypothesisStackNormal(Manager manager) {
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
		// too bad for stack. don't bother adding hypo into collection
		if (!StaticData.instance().getDisableDiscarding()
				&& hypo.getTotalScore() < m_worstScore
				&& !(m_minHypoStackDiversity > 0 && hypo.getTotalScore() >= getWorstScoreForBitmap(hypo
						.getWordsBitmap()))) {
			m_manager.getSentenceStats().addDiscarded();
			VERBOSE.v(3, "discarded, too bad for stack" + "\n");
			hypo = null;
			return false;
		}

		// over threshold, try to add to collection
		boolean addRet = add(hypo);
		if (addRet) { // nothing found. add to collection
			return true;
		}

		// equiv hypo exists, recombine with other hypo
		// iterator &iterExisting = addRet.first;
		Hypothesis hypoExisting = CollectionUtils.get(this, hypo);
		ASSERT.a(hypoExisting != null);

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
				System.exit(0);
			}
			return false;
		} else { // already storing the best hypo. discard current hypo
			VERBOSE
					.v(3, "worse than matching hyp " + hypoExisting.getId() + ", recombining"
							+ "\n");
			if (m_nBestIsEnabled) {
				hypoExisting.addArc(hypo);
			} else {
				hypo = null;
			}
			return false;
		}
	}

	/**
	 * set maximum number of hypotheses in the collection
	 * \param maxHypoStackSize maximum number (typical number: 100)
	 * \param maxHypoStackSize maximum number (defauly: 0)
	 */
	public final void setMaxHypoStackSize(int maxHypoStackSize, int minHypoStackDiversity) {
		m_maxHypoStackSize = maxHypoStackSize;
		m_minHypoStackDiversity = minHypoStackDiversity;
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

	/** return worst allowable score */
	public final float getWorstScore() {
		return m_worstScore;
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
		if (size() <= newSize)
			return; // ok, if not over the limit

		// we need to store a temporary list of hypotheses
		List<Hypothesis> hypos = getSortedListNOTCONST();
		boolean[] included = new boolean[hypos.size()];
		for (int i = 0; i < hypos.size(); i++)
			included[i] = false;

		// clear out original set
		// for( iterator iter = m_hypos.begin(); iter != m_hypos.end(); )
		// {
		// iterator removeHyp = iter++;
		// detach(removeHyp);
		// }
		clear();

		// add best hyps for each coverage according to minStackDiversity
		if (m_minHypoStackDiversity > 0) {
			Map<Long, Integer> diversityCount = new HashMap<Long, Integer>();
			for (int i = 0; i < hypos.size(); i++) {
				Hypothesis hyp = hypos.get(i);
				long coverage = hyp.getWordsBitmap().getID();
				;
				if (diversityCount.get(coverage) == null)
					diversityCount.put(coverage, 0);

				if (diversityCount.get(coverage) < m_minHypoStackDiversity) {
					add(hyp);
					included[i] = true;
					diversityCount.put(coverage, diversityCount.get(coverage) + 1);
					if (diversityCount.get(coverage) == m_minHypoStackDiversity)
						setWorstScoreForBitmap(coverage, hyp.getTotalScore());
				}
			}
		}

		// only add more if stack not full after satisfying minStackDiversity
		if (size() < newSize) {

			// add best remaining hypotheses
			for (int i = 0; i < hypos.size() && size() < newSize
					&& hypos.get(i).getTotalScore() > m_bestScore + m_beamWidth; i++) {
				if (!included[i]) {
					add(hypos.get(i));
					included[i] = true;
					if (size() == newSize)
						m_worstScore = hypos.get(i).getTotalScore();
				}
			}
		}

		// delete hypotheses that have not been included
		for (int i = 0; i < hypos.size(); i++) {
			if (!included[i]) {
				hypos.set(i, null);
				m_manager.getSentenceStats().addPruning();
			}
		}
		included = null;

		// some reporting....
		VERBOSE.v(3, ", pruned to size " + size() + "\n");
		if (VERBOSE.v(3)) {
			TRACE.err("stack now contains: ");
			for (Hypothesis hypo : this) {
				TRACE.err(hypo.getId() + " (" + hypo.getTotalScore() + ") ");
			}
			TRACE.err("\n");
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
				return hypo1.getTotalScore() < hypo2.getTotalScore() ? -1 : 1;
			}

		});
		return ret;

	}

	public List<Hypothesis> getSortedListNOTCONST() {
		List<Hypothesis> ret = new ArrayList<Hypothesis>(size());
		ret.addAll(this);
		Collections.sort(ret, new Comparator<Hypothesis>() {
			public int compare(Hypothesis hypo1, Hypothesis hypo2) {
				return hypo1.getTotalScore() < hypo2.getTotalScore()? -1 :1;
			}

		});

		return ret;
	}

	/**
	 * make all arcs in point to the equiv hypothesis that contains them.
	 * Ie update doubly linked list be hypo & arcs
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
