package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK
 */
public class UnknownWordPenaltyProducer extends StatelessFeatureFunction {

	public UnknownWordPenaltyProducer(ScoreIndexManager scoreIndexManager) {
		scoreIndexManager.addScoreProducer((ScoreProducer) this);
	}

	public int getNumScoreComponents() {
		return 1;
	}

	public String getScoreProducerDescription() {
		return "!UnknownWordPenalty";
	}

	public String getScoreProducerWeightShortName() {
		return "u";
	}

	public int getNumInputScores() {
		return 0;
	}

	public boolean computeValueInTranslationOption() {
		return true;
	}

}
