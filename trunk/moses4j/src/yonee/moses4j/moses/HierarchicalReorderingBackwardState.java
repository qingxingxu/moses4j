package yonee.moses4j.moses;


import yonee.moses4j.moses.LexicalReorderingConfiguration.Direction;
import yonee.moses4j.moses.LexicalReorderingConfiguration.ModelType;
/**
 * 
 * @author YONEE
 * @OK
 */
public class HierarchicalReorderingBackwardState extends LexicalReorderingState {

	private ReorderingStack m_reoStack = new ReorderingStack();

	public HierarchicalReorderingBackwardState(final LexicalReorderingConfiguration config,
			int offset) {
		super(config, Direction.Backward, offset);
	}

	public HierarchicalReorderingBackwardState(final HierarchicalReorderingBackwardState prev,
			final TranslationOption topt, ReorderingStack reoStack) {
		super(prev, topt);
		m_reoStack = reoStack;
	}

	public int compare(final FFState o) {
		final HierarchicalReorderingBackwardState other = (HierarchicalReorderingBackwardState) (o);
		return m_reoStack.compare(other.m_reoStack);
	}

	public LexicalReorderingState expand(final TranslationOption topt, float[] scores) {
		HierarchicalReorderingBackwardState nextState = new HierarchicalReorderingBackwardState(
				this, topt, m_reoStack);
		int reoType;
		final ModelType modelType = m_configuration.getModelType();

		int reoDistance = nextState.m_reoStack.shiftReduce(topt.getSourceWordsRange());

		if (modelType == ModelType.MSD) {
			reoType = getOrientationTypeMSD(reoDistance);
		} else if (modelType == ModelType.MSLR) {
			reoType = getOrientationTypeMSLR(reoDistance);
		} else if (modelType == ModelType.LeftRight) {
			reoType = getOrientationTypeLeftRight(reoDistance);
		} else {
			reoType = getOrientationTypeMonotonic(reoDistance);
		}

		copyScores(scores, topt, reoType);
		return nextState;
	}

	private int getOrientationTypeMSD(int reoDistance) {
		if (reoDistance == 1) {
			return M;
		} else if (reoDistance == -1) {
			return S;
		}
		return D;
	}

	private int getOrientationTypeMSLR(int reoDistance) {
		if (reoDistance == 1) {
			return M;
		} else if (reoDistance == -1) {
			return S;
		} else if (reoDistance > 1) {
			return DR;
		}
		return DL;
	}

	private int getOrientationTypeMonotonic(int reoDistance) {
		if (reoDistance == 1) {
			return M;
		}
		return NM;
	}

	private int getOrientationTypeLeftRight(int reoDistance) {
		if (reoDistance == 1) {
			return M;
		}
		return NM;
	}

}
