package yonee.moses4j.moses;
/**
 * 
 * @author YONEE
 * @OK
 */
public class WordPenaltyProducer extends StatelessFeatureFunction {

	public WordPenaltyProducer(ScoreIndexManager scoreIndexManager) {
		scoreIndexManager.addScoreProducer((ScoreProducer) this);
	}

	public int getNumScoreComponents() {
		return 1;
	}

	public String getScoreProducerDescription() {
		return "WordPenalty";
	}

	public String getScoreProducerWeightShortName() {
		return "w";
	}

	public int getNumInputScores() {
		return 0;
	}

	public void evaluate(final TargetPhrase tp, ScoreComponentCollection out) {
		out.plusEquals(this, -(float) tp.getSize());
	}

}
