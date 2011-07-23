package yonee.moses4j.moses;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public abstract class StatefulFeatureFunction extends FeatureFunction {

	/**
	 * \brief This interface should be implemented. Notes: When evaluating the
	 * value of this feature function, you should avoid calling
	 * hypo.GetPrevHypo(). If you need something from the "previous" hypothesis,
	 * you should store it in an FFState object which will be passed in as
	 * prev_state. If you don't do this, you will get in trouble.
	 */
	public abstract FFState evaluate(final Hypothesis cur_hypo,
			final FFState prev_state, ScoreComponentCollection accumulator);

	// ! return the state associated with the empty hypothesis for a given
	// sentence
	public abstract FFState emptyHypothesisState(final InputType input);

	public boolean isStateless() {
		return false;
	}
}
