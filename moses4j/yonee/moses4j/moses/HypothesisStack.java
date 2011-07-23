package yonee.moses4j.moses;

import java.util.HashSet;
import java.util.List;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       remove
 *       detach
 */
public abstract class HypothesisStack extends HashSet<Hypothesis> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Manager m_manager;

	public HypothesisStack(Manager manager) {
		m_manager = manager;
	}

	public float getWorstScore() {
		return Float.NEGATIVE_INFINITY;
	}

	public float getWorstScoreForBitmap(long w) {
		return Float.NEGATIVE_INFINITY;
	}

	public float getWorstScoreForBitmap(WordsBitmap w) {
		return Float.NEGATIVE_INFINITY;
	}

	public abstract boolean addPrune(Hypothesis hypothesis);

	public abstract Hypothesis getBestHypothesis();

	public abstract List<Hypothesis> getSortedList();

	// ! remove hypothesis pointed to by iterator but don't delete the object
	public void detach(final Hypothesis iter) {
		remove(iter);
	}

	public void remove(final Hypothesis iter) {
		remove(iter);
	}
}
