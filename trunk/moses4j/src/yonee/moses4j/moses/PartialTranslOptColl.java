package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import yonee.utils.CollectionUtils;

/**
 * 
 * @author YONEE
 * @OK
 */
public class PartialTranslOptColl {

	protected List<TranslationOption> m_list = new  ArrayList<TranslationOption>();
	protected float m_bestScore;
	/** < score of the best translation option */
	protected float m_worstScore;
	/** < score of the worse translation option */
	protected int m_maxSize;
	/** < maximum number of translation options allowed */
	protected int m_totalPruned;

	/** < number of options pruned */

	public PartialTranslOptColl() {
		m_bestScore = Float.NEGATIVE_INFINITY;
		m_worstScore = Float.NEGATIVE_INFINITY;
		m_maxSize = StaticData.instance().getMaxNoPartTransOpt();
		m_totalPruned = 0;
	}

	public void addNoPrune(TranslationOption partialTranslOpt) {
		partialTranslOpt.calcScore();
		if (partialTranslOpt.getFutureScore() >= m_worstScore) {
			m_list.add(partialTranslOpt);
			if (partialTranslOpt.getFutureScore() > m_bestScore)
				m_bestScore = partialTranslOpt.getFutureScore();
		} else {
			m_totalPruned++;
			partialTranslOpt = null;
		}
	}

	public void add(TranslationOption partialTranslOpt) {
		// add
		addNoPrune(partialTranslOpt);

		// done if not too large (lazy pruning, only if twice as large as max)
		if (m_list.size() > 2 * m_maxSize) {
			prune();
		}
	}

	public void prune() {
		// done if not too big
		if (m_list.size() <= m_maxSize) {
			return;
		}

		// TRACE_ERR( "pruning partial translation options from size " << m_list.size() <<
		// std::endl);

		// find nth element
		Collections.sort(m_list,  new Comparator<TranslationOption>() {
			public int compare(TranslationOption a, TranslationOption b) {
				return a.getFutureScore() < b.getFutureScore() ? -1:1;
			}

		});

		m_worstScore = m_list.get(m_maxSize - 1).getFutureScore();

		// delete the rest
		for (int i = m_maxSize; i < m_list.size(); ++i) {
			m_totalPruned++;
		}
		CollectionUtils.resize(m_list, m_maxSize, null);

		// TRACE_ERR( "pruned to size " << m_list.size() << ", total pruned: " << m_totalPruned <<
		// std::endl);
	}

	/** returns list of translation options */
	public final List<TranslationOption> getList() {
		return m_list;
	}

	/** clear out the list */
	public void detachAll() {
		m_list.clear();
	}

	/** return number of pruned partial hypotheses */
	public int getPrunedCount() {
		return m_totalPruned;
	}

}
