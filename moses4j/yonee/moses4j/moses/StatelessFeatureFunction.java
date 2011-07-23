package yonee.moses4j.moses;

import yonee.utils.ASSERT;
/**
 * 
 * @author YONEE
 * OK
 */
public abstract class StatelessFeatureFunction extends FeatureFunction {

	public void evaluate(TargetPhrase targetPhrase,
			ScoreComponentCollection accumulator) {
		ASSERT
				.a("Please implement Evaluate or set ComputeValueInTranslationOption to true" != null);
	}

	public boolean computeValueInTranslationOption() {
		return false;
	}

	public boolean isStateless() {
		return true;
	}

}
