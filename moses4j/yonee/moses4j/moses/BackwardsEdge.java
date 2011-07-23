package yonee.moses4j.moses;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import yonee.utils.ASSERT;
import yonee.utils.VERBOSE;
/**
 * 
 * @author YONEE
 * @OK not verify
 * 		
 */
public class BackwardsEdge {

	private boolean initialized;

	private BitmapContainer prevBitmapContainer;
	private BitmapContainer parent;

	private TranslationOptionList translations;
	private SquareMatrix futurescore;

	private List<Hypothesis> hypotheses;
	private Set<Integer> seenPosition;

	private Hypothesis createHypothesis(final Hypothesis hypothesis,
			final TranslationOption transOpt) {
		// create hypothesis and calculate all its scores
		Hypothesis newHypo = hypothesis.createNext(transOpt, null);

		for (TranslationOption to : transOpt.getLinkedTransOpts()) {
			final WordsBitmap hypoBitmap = newHypo.getWordsBitmap();
			if (hypoBitmap.overlap(to.getSourceWordsRange())) {
				// don't want to add a hypothesis that has some but not all of a
				// linked TO set, so return
				// delete newHypo;
				return null;
			} else {
				newHypo.calcScore(futurescore);
				newHypo = newHypo.createNext(to, null);
			}
		}
		newHypo.calcScore(futurescore);
		return newHypo;
	}

	private boolean seenPosition(final int x, final int y) {
		return seenPosition.contains(x << 16 + y);
	}

	private void setSeenPosition(final int x, final int y) {
		ASSERT.a(x < (1 << 17));
		ASSERT.a(y < (1 << 17));
		seenPosition.add((x << 16) + y);
	}

	protected void initialize() {
		if (hypotheses.size() == 0 || translations.size() == 0) {
			initialized = true;
			return;
		}

		Hypothesis expanded = createHypothesis(hypotheses.get(0), translations
				.get(0));
		parent.enqueue(0, 0, expanded, this);
		setSeenPosition(0, 0);
		initialized = true;
	}

	public BackwardsEdge(final BitmapContainer prevBitmapContainer,
			BitmapContainer parent, final TranslationOptionList translations,
			final SquareMatrix futureScore, final InputType source) {
		this.initialized = false;
		this.prevBitmapContainer = prevBitmapContainer;
		this.parent = parent;
		this.translations = translations;
		this.futurescore = futureScore;
		this.seenPosition = new HashSet<Integer>();

		// If either dimension is empty, we haven't got anything to do.
		if (prevBitmapContainer.getHypotheses().size() == 0
				|| translations.size() == 0) {
			VERBOSE.v(3, "Empty cube on BackwardsEdge\n");
			return;
		}

		// Fetch the things we need for distortion cost computation.
		int maxDistortion = StaticData.instance().getMaxDistortion();

		if (maxDistortion == -1) {
			hypotheses.addAll(prevBitmapContainer.getHypotheses());
			return;
		}

		final WordsRange transOptRange = translations.get(0)
				.getSourceWordsRange();
		for (Hypothesis hypo : prevBitmapContainer.getHypotheses()) {
			// Special case: If this is the first hypothesis used to seed the
			// search,
			// it doesn't have a valid range, and we create the hypothesis, if
			// the
			// initial position is not further into the sentence than the
			// distortion limit.
			if (hypo.getWordsBitmap().getNumWordsCovered() == 0) {
				if ((int) transOptRange.getStartPos() <= maxDistortion)
					hypotheses.add(hypo);
			} else {
				int distortionDistance = source.i().computeDistortionDistance(hypo
						.getCurrSourceWordsRange(), transOptRange);

				if (distortionDistance <= maxDistortion)
					hypotheses.add(hypo);
			}
		}

		if (translations.size() > 1) {
			ASSERT.a(translations.get(0).getFutureScore() >= translations
					.get(1).getFutureScore());
		}

		if (hypotheses.size() > 1) {
			ASSERT.a(hypotheses.get(0).getTotalScore() >= hypotheses.get(1)
					.getTotalScore());
		}

		Collections.sort(hypotheses,
				new BitmapContainer.HypothesisScoreOrdererWithDistortion(
						transOptRange));

	}

	public boolean getInitialized() {
		return initialized;
	}

	final public BitmapContainer getBitmapContainer() {
		return prevBitmapContainer;
	}

	// NO IMPLEMENTS
	// public int getDistortionPenalty()

	public void pushSuccessors(final int x, final int y) {
		Hypothesis newHypo;
		if (y + 1 < translations.size() && !seenPosition(x, y + 1)) {
			setSeenPosition(x, y + 1);
			newHypo = createHypothesis(hypotheses.get(x), translations
					.get(y + 1));
			if (newHypo != null) {
				parent.enqueue(x, y + 1, newHypo, this);
			}
		}

		if (x + 1 < hypotheses.size() && !seenPosition(x + 1, y)) {
			setSeenPosition(x + 1, y);
			newHypo = createHypothesis(hypotheses.get(x + 1), translations
					.get(y));
			if (newHypo != null) {
				parent.enqueue(x + 1, y, newHypo, this);
			}
		}
	}
}
