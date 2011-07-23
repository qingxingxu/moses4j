package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import yonee.utils.ASSERT;
/**
 * 
 * @author YONEE
 * @OK
 */
public class BitmapContainer {

	private WordsBitmap bitmap;
	private HypothesisStackCubePruning stack;
	private List<Hypothesis> hypotheses; // HypothesisSet
	private Set<BackwardsEdge> edges; // BackwardsEdgeSet
	private Queue<HypothesisQueueItem> queue; //
	private int numStackInsertions;

	// We always require a corresponding bitmap to be supplied.

	public BitmapContainer(final WordsBitmap bitmap, HypothesisStackCubePruning stack) {
		hypotheses = new ArrayList<Hypothesis>();
		edges = new HashSet<BackwardsEdge>();
		queue = new PriorityQueue<HypothesisQueueItem>();
	}

	public void enqueue(int hypothesisPos, int translationPos, Hypothesis hypothesis,
			BackwardsEdge edge) {
		queue.add(new HypothesisQueueItem(hypothesisPos, translationPos, hypothesis, edge));
	}

	public HypothesisQueueItem dequeue() {
		return dequeue(false);

	}

	public HypothesisQueueItem dequeue(boolean keepValue) {
		if (!queue.isEmpty()) {
			HypothesisQueueItem item = queue.peek();

			if (!keepValue) {
				queue.poll();
			}

			return item;
		}

		return null;
	}

	public final HypothesisQueueItem top() {
		return queue.peek();
	}

	public int size() {
		return queue.size();
	}

	public final boolean empty() {
		return queue.isEmpty();
	}

	public final WordsBitmap getWordsBitmap() {
		return this.bitmap;
	}

	public final List<Hypothesis> getHypotheses() {
		return this.hypotheses;
	}

	public final int getHypothesesSize() {
		return this.hypotheses.size();
	}

	public final Set<BackwardsEdge> getBackwardsEdges() {
		return this.edges;
	}

	public void initializeEdges() {
		for (Iterator<BackwardsEdge> iter = edges.iterator(); iter.hasNext();) {
			iter.next().initialize();
		}
	}

	public void processBestHypothesis() {
		if (queue.isEmpty()) {
			return;
		}

		// Get the currently best hypothesis from the queue.
		HypothesisQueueItem item = dequeue();

		// If the priority queue is exhausted, we are done and should have
		// exited
		ASSERT.a(item != null);

		// check we are pulling things off of priority queue in right order
		if (!empty()) {
			HypothesisQueueItem check = dequeue(true);
			ASSERT.a(item.getHypothesis().getTotalScore() >= check.getHypothesis().getTotalScore());
		}

		// Logging for the criminally insane
		// IFVERBOSE(3) {
		// const StaticData &staticData = StaticData::Instance();
		// item.getHypothesis().PrintHypothesis();
		// }

		// Add best hypothesis to hypothesis stack.
		boolean newstackentry = stack.addPrune(item.getHypothesis());
		if (newstackentry)
			numStackInsertions++;

		// IFVERBOSE(3) {
		// TRACE_ERR("new stack entry flag is " << newstackentry << std::endl);
		// }

		// Create new hypotheses for the two successors of the hypothesis just
		// added.
		item.getBackwardsEdge().pushSuccessors(item.getHypothesisPos(), item.getTranslationPos());

	}

	public void ensureMinStackHyps(final int minNumHyps) {
		while ((!empty()) && numStackInsertions < minNumHyps) {
			processBestHypothesis();
		}
	}

	public void addHypothesis(Hypothesis hypothesis) {
		boolean itemExists = false;
		for (Iterator<Hypothesis> iter = hypotheses.iterator(); iter.hasNext();) {
			if (iter == hypothesis) {
				itemExists = true;
				break;
			}
		}
		if (!itemExists) {
			hypotheses.add(hypothesis);
		}

	}

	public void addBackwardsEdge(BackwardsEdge edge) {
		edges.add(edge);
	}

	public void sortHypotheses() {
		Collections.sort(hypotheses, new Hypothesis.HypothesisScoreOrderer());
	}

	public static class HypothesisScoreOrdererWithDistortion implements Comparator<Hypothesis> {

		public HypothesisScoreOrdererWithDistortion(WordsRange transOptRange) {
			this.transOptRange = transOptRange;
		}

		public WordsRange transOptRange;

		public int compare(final Hypothesis hypoA, final Hypothesis hypoB) {
			ASSERT.a(transOptRange != null);

			final float weightDistortion = StaticData.instance().getWeightDistortion();
			final DistortionScoreProducer dsp = StaticData.instance().getDistortionScoreProducer();
			final float distortionScoreA = dsp.calculateDistortionScore(hypoA, hypoA
					.getCurrSourceWordsRange(), transOptRange, hypoA.getWordsBitmap()
					.getFirstGapPos());
			final float distortionScoreB = dsp.calculateDistortionScore(hypoB, hypoB
					.getCurrSourceWordsRange(), transOptRange, hypoB.getWordsBitmap()
					.getFirstGapPos());

			final float scoreA = hypoA.getScore() + distortionScoreA * weightDistortion;
			final float scoreB = hypoB.getScore() + distortionScoreB * weightDistortion;

			if (scoreA > scoreB) {
				return 1;
			} else if (scoreA < scoreB) {
				return 0;
			} else if (hypoA == hypoB) {// YONEE
				return 0;
			} else {
				return 1;
			}
		}

	};
}
