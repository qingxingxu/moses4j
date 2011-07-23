package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import yonee.utils.CollectionUtils;

/**
 * 
 * @author YONEE
 * @REST<<
 * 
 * @RISK
 *       std::nth_element
 *       resize
 * 
 */
public class ChartRuleCollection extends ArrayList<ChartRule> {

	private static final long serialVersionUID = 1L;
	protected float m_scoreThreshold;

	ChartRuleCollection() {
		super(200);
		m_scoreThreshold = Float.POSITIVE_INFINITY;
	}

	void add(TargetPhraseCollection targetPhraseCollection, final WordConsumed wordConsumed,
			boolean adhereTableLimit, int ruleLimit) {

		int iterEnd = (!adhereTableLimit || ruleLimit == 0 || targetPhraseCollection.size() < ruleLimit) ? targetPhraseCollection
				.size()
				: ruleLimit;

		for (int i = 0; i != iterEnd; i++) {
			final TargetPhrase targetPhrase = targetPhraseCollection.get(i);
			float score = targetPhrase.getFutureScore();

			if (size() < ruleLimit) { // not yet filled out quota. add
				// everything
				add(new ChartRule(targetPhrase, wordConsumed));
				m_scoreThreshold = (score < m_scoreThreshold) ? score : m_scoreThreshold;
			} else if (score > m_scoreThreshold) { // full but not bursting. add
				// if better than worst
				// score
				add(new ChartRule(targetPhrase, wordConsumed));
			}

			// prune if bursting
			if (size() > ruleLimit * 2) {
				Collections.sort(this,  new Comparator<ChartRule>() {
					public int compare(ChartRule o1, ChartRule o2) {
						return o1.getTargetPhrase().getFutureScore() < o2.getTargetPhrase()
								.getFutureScore() ? -1 : 1;
					}

				});
				// delete the bottom half
				for (int ind = ruleLimit; ind < size(); ++ind) {
					// make the best score of bottom half the score threshold
					final TargetPhrase targetPhrase0 = get(ind).getTargetPhrase();
					float score0 = targetPhrase0.getFutureScore();
					m_scoreThreshold = (score0 > m_scoreThreshold) ? score0 : m_scoreThreshold;
				}
				CollectionUtils.resize(this, ruleLimit, null);
			}

		}
	}

	public void createChartRules(int ruleLimit) {
		if (size() > ruleLimit) {
			Collections.sort(this, new Comparator<ChartRule>() {
				public int compare(ChartRule o1, ChartRule o2) {
					return o1.getTargetPhrase().getFutureScore() < o2.getTargetPhrase()
							.getFutureScore() ? -1 :1;
				}
			});
			CollectionUtils.resize(this, ruleLimit, null);
		}
		// finalise creation of chart rules
		for (int ind = 0; ind < size(); ++ind) {
			ChartRule rule = get(ind);
			rule.createNonTermIndex();
		}
	}
}
