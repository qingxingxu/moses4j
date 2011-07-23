package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.ASSERT;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class TrellisPath {

	// friend std::ostream& operator<<(std::ostream&, const TrellisPath&);

	protected List<Hypothesis> m_path = new ArrayList<Hypothesis>(); // < list of hypotheses/arcs
	protected int m_prevEdgeChanged;
	/**
	 * < the last node that was wiggled to create this path
	 * , or NOT_FOUND if this path is the best trans so consist of only hypos
	 */

	protected ScoreComponentCollection m_scoreBreakdown;
	protected float m_totalScore;

	// ! create path OF pure hypo
	protected TrellisPath(Hypothesis hypo) {
		m_prevEdgeChanged = TypeDef.NOT_FOUND;
		m_scoreBreakdown = hypo.getScoreBreakdown();
		m_totalScore = hypo.getTotalScore();

		// enumerate path using prevHypo
		while (hypo != null) {
			m_path.add(hypo);
			hypo = hypo.getPrevHypo();
		}
	}

	/**
	 * create path from another path, deviate at edgeIndex by using arc instead,
	 * which may change other hypo back from there
	 */
	protected TrellisPath(final TrellisPath copy, int edgeIndex, final Hypothesis arc) {
		m_prevEdgeChanged = edgeIndex;

		// m_path.reserve(copy.m_path.size());
		for (int currEdge = 0; currEdge < edgeIndex; currEdge++) { // copy path from parent
			m_path.add(copy.m_path.get(currEdge));
		}

		// 1 deviation
		m_path.add(arc);

		// rest of path comes from following best path backwards
		Hypothesis prevHypo = arc.getPrevHypo();
		while (prevHypo != null) {
			m_path.add(prevHypo);
			prevHypo = prevHypo.getPrevHypo();
		}

		// Calc score
		m_totalScore = m_path.get(0).getWinningHypo().getTotalScore();
		m_scoreBreakdown = m_path.get(0).getWinningHypo().getScoreBreakdown();

		int sizePath = m_path.size();
		for (int pos = 0; pos < sizePath; pos++) {
			final Hypothesis hypo = m_path.get(pos);
			final Hypothesis winningHypo = hypo.getWinningHypo();
			if (hypo != winningHypo) {
				m_totalScore = m_totalScore - winningHypo.getTotalScore() + hypo.getTotalScore();
				m_scoreBreakdown.minusEquals(winningHypo.getScoreBreakdown());
				m_scoreBreakdown.plusEquals(hypo.getScoreBreakdown());
			}
		}
	}

	// ! get score for this path throught trellis
	public final float getTotalScore() {
		return m_totalScore;
	}

	/**
	 * list of each hypo/arcs in path. For anything other than the best hypo, it is not possible
	 * just to follow the
	 * m_prevHypo variable in the hypothesis object
	 */
	public final List<Hypothesis> getEdges() {
		return m_path;
	}

	// ! create a set of next best paths by wiggling 1 of the node at a time.
	protected void createDeviantPaths(TrellisPathCollection pathColl) {
		final int sizePath = m_path.size();

		if (m_prevEdgeChanged == TypeDef.NOT_FOUND) { // initial enumration from a pure hypo
			for (int currEdge = 0; currEdge < sizePath; currEdge++) {
				final Hypothesis hypo = (Hypothesis) (m_path.get(currEdge));
				final List<Hypothesis> pAL = hypo.getArcList();
				if (pAL == null)
					continue;
				final List<Hypothesis> arcList = pAL;

				// every possible Arc to replace this edge
				for (Hypothesis arc : arcList) {
					pathColl.add(new TrellisPath(this, currEdge, arc));
				}
			}
		} else { // wiggle 1 of the edges only
			for (int currEdge = m_prevEdgeChanged + 1; currEdge < sizePath; currEdge++) {
				final List<Hypothesis> pAL = m_path.get(currEdge).getArcList();
				if (pAL == null)
					continue;
				final List<Hypothesis> arcList = pAL;

				for (Hypothesis arcReplace : arcList) {
					pathColl.add(new TrellisPath(this, currEdge, arcReplace));
				} // for (iterArc...
			} // for (currEdge = 0 ...
		}
	}

	// ! create a list of next best paths by wiggling 1 of the node at a time.
	protected void createDeviantPaths(TrellisPathList pathColl) {
		final int sizePath = m_path.size();

		if (m_prevEdgeChanged == TypeDef.NOT_FOUND) { // initial enumration from a pure hypo
			for (int currEdge = 0; currEdge < sizePath; currEdge++) {
				final Hypothesis hypo = (Hypothesis) (m_path.get(currEdge));
				final List<Hypothesis> pAL = hypo.getArcList();
				if (pAL == null)
					continue;
				final List<Hypothesis> arcList = pAL;
				for (Hypothesis arc : arcList) {
					pathColl.add(new TrellisPath(this, currEdge, arc));
				}

			}
		} else { // wiggle 1 of the edges only
			for (int currEdge = m_prevEdgeChanged + 1; currEdge < sizePath; currEdge++) {
				final List<Hypothesis> pAL = m_path.get(currEdge).getArcList();
				if (pAL == null)
					continue;
				final List<Hypothesis> arcList = pAL;

				for (Hypothesis arcReplace : arcList) {
					pathColl.add(new TrellisPath(this, currEdge, arcReplace));
				} // for (iterArc...
			} // for (currEdge = 0 ...
		}

	}

	public final ScoreComponentCollection getScoreBreakdown() {
		return m_scoreBreakdown;
	}

	// ! get target words range of the hypo within n-best trellis. not necessarily the same as
	// hypo.GetCurrTargetWordsRange()
	public WordsRange getTargetWordsRange(final Hypothesis hypo) {
		int startPos = 0;

		for (int indEdge = (int) m_path.size() - 1; indEdge >= 0; --indEdge) {
			final Hypothesis currHypo = m_path.get(indEdge);
			int endPos = startPos + currHypo.getCurrTargetLength() - 1;

			if (currHypo == hypo) {
				return new WordsRange(startPos, endPos);
			}
			startPos = endPos + 1;
		}

		// have to give a hypo in the trellis path, but u didn't.
		ASSERT.a(false);
		return new WordsRange(TypeDef.NOT_FOUND, TypeDef.NOT_FOUND);
	}

	protected Phrase getTargetPhrase() {
		Phrase targetPhrase = new Phrase(FactorDirection.Output);

		int numHypo = (int) m_path.size();
		for (int node = numHypo - 2; node >= 0; --node) { // don't do the empty hypo - waste of time
			// and decode step id is invalid
			final Hypothesis hypo = m_path.get(node);
			final Phrase currTargetPhrase = hypo.getCurrTargetPhrase();

			targetPhrase.append(currTargetPhrase);
		}

		return targetPhrase;
	}

	protected Phrase getSurfacePhrase() {
		final int[] outputFactor = StaticData.instance().getOutputFactorOrder();
		Phrase targetPhrase = getTargetPhrase(), ret = new Phrase(FactorDirection.Output);

		for (int pos = 0; pos < targetPhrase.getSize(); ++pos) {
			Word newWord = ret.addWord();
			for (int i = 0; i < outputFactor.length; i++) {
				int factorType = outputFactor[i];
				final Factor factor = targetPhrase.getFactor(pos, factorType);
				ASSERT.a(factor != null);
				newWord.set(factorType, factor);
			}
		}

		return ret;
	}

	// TO_STRING(){}

}
