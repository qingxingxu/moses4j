package yonee.moses4j.moses;

import java.util.Comparator;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class HypothesisQueueItem {

	private int hypothesisPos, translationPos;

	private Hypothesis hypothesis;
	private BackwardsEdge edge;

	public HypothesisQueueItem(final int hypothesisPos,
			final int translationPos, Hypothesis hypothesis, BackwardsEdge edge) {
		this.hypothesisPos = hypothesisPos;
		this.translationPos = translationPos;
		this.hypothesis = hypothesis;
		this.edge = edge;
	}

	public int getHypothesisPos() {
		return hypothesisPos;
	}

	public int getTranslationPos() {
		return translationPos;
	}

	public Hypothesis getHypothesis() {
		return hypothesis;
	}

	public BackwardsEdge getBackwardsEdge() {
		return edge;
	}

	// Allows to compare two HypothesisQueueItem objects by the corresponding
	// scores.
	public static  class QueueItemOrderer implements Comparator<HypothesisQueueItem> {
		public int compare(HypothesisQueueItem itemA, HypothesisQueueItem itemB) {
			return (int) (itemA.getHypothesis().getTotalScore() - itemB
					.getHypothesis().getTotalScore());
		}
	}
}
