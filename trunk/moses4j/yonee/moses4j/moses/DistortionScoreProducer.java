package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class DistortionScoreProducer extends StatefulFeatureFunction {

	public DistortionScoreProducer(ScoreIndexManager scoreIndexManager) {
		scoreIndexManager.addScoreProducer(this);
	}

	public float calculateDistortionScore(final Hypothesis hypo, final WordsRange prev,
			final WordsRange curr, final int firstGap) {
		//final int USE_OLD = 1;
		//if (USE_OLD != 0) {
			return -(float) hypo.getInput().i().computeDistortionDistance(prev, curr);
		//}

		// Pay distortion score as soon as possible, from Moore and Quirk MT
		// Summit 2007

//		int prefixEndPos = firstGap - 1;
//		if ((int) curr.getStartPos() == prefixEndPos + 1) {
//			return 0;
//		}
//
//		if ((int) curr.getEndPos() < (int) prev.getEndPos()) {
//			return (float) -2 * curr.getNumWordsCovered();
//		}
//
//		if ((int) prev.getEndPos() <= prefixEndPos) {
//			int z = curr.getStartPos() - prefixEndPos;
//			return (float) -2 * (z + curr.getNumWordsCovered());
//		}
//
//		return (float) -2 * (curr.getNumWordsBetween(prev) + curr.getNumWordsCovered());
	}

	public int getNumScoreComponents() {
		return 1;
	}

	public String getScoreProducerDescription() {
		return "Distortion";
	}

	public String getScoreProducerWeightShortName() {
		return "d";
	}

	public int getNumInputScores() {
		return 0;
	}

	public FFState emptyHypothesisState(final InputType input) {
		// fake previous translated phrase start and end
		int start = TypeDef.NOT_FOUND;
		int end = TypeDef.NOT_FOUND;
		if (input.i().m_frontSpanCoveredLength > 0) {
			// can happen with --continue-partial-translation
			start = 0;
			end = input.i().m_frontSpanCoveredLength - 1;
		}
		return new DistortionStateTraditional(new WordsRange(start, end), TypeDef.NOT_FOUND);
	}

	public FFState evaluate(final Hypothesis hypo, final FFState prev_state,
			ScoreComponentCollection out) {
		final DistortionStateTraditional prev = (DistortionStateTraditional) prev_state;
		final float distortionScore = calculateDistortionScore(hypo, prev.range, hypo
				.getCurrSourceWordsRange(), prev.firstGap);
		out.plusEquals(this, distortionScore);
		DistortionStateTraditional res = new DistortionStateTraditional(hypo
				.getCurrSourceWordsRange(), hypo.getPrevHypo().getWordsBitmap().getFirstGapPos());
		return res;
	}

	class DistortionStateTraditional implements FFState {
		public WordsRange range;
		public int firstGap;

		public DistortionStateTraditional(WordsRange wr, int fg) {
			range = wr;
			firstGap = fg;
		}

		public int compare(final FFState other) {
			DistortionStateTraditional o = (DistortionStateTraditional) other;
			if (range.getEndPos() < o.range.getEndPos())
				return -1;
			if (range.getEndPos() > o.range.getEndPos())
				return 1;
			return 0;
		}
	};

}
