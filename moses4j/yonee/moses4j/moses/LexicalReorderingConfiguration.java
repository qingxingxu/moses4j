package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK
 */
public class LexicalReorderingConfiguration {
	private ScoreProducer m_scoreProducer;
	private ModelType m_modelType;
	private boolean m_phraseBased;
	private boolean m_collapseScores;
	private Direction m_direction;
	private Condition m_condition;

	public enum ModelType {
		Monotonic, MSD, MSLR, LeftRight, None
	};

	public enum Direction {
		Forward, Backward, Bidirectional
	};

	public enum Condition {
		F, E, FE
	};

	public LexicalReorderingConfiguration(ScoreProducer scoreProducer, final String modelType) {
		m_scoreProducer = scoreProducer;
		m_modelType = ModelType.None;
		m_phraseBased = true;
		m_collapseScores = false;
		m_direction = Direction.Backward;
		String[] config = Util.tokenize(modelType, "-");

		for (int i = 0; i < config.length; ++i) {
			if (config[i].equals("hier")) {
				m_phraseBased = false;
			} else if (config[i].equals("phrase")) {
				m_phraseBased = true;
			} else if (config[i].equals("wbe")) {
				m_phraseBased = true;
				// no word-based decoding available, fall-back to phrase-based
				// This is the old lexical reordering model combination of moses
			} else if (config[i].equals("msd")) {
				m_modelType = ModelType.MSD;
			} else if (config[i].equals("mslr")) {
				m_modelType = ModelType.MSLR;
			} else if (config[i].equals("monotonicity")) {
				m_modelType = ModelType.Monotonic;
			} else if (config[i].equals("leftright")) {
				m_modelType = ModelType.LeftRight;
			} else if (config[i].equals("backward") || config[i].equals("unidirectional")) {
				// note: unidirectional is deprecated, use backward instead
				m_direction = Direction.Backward;
			} else if (config[i].equals("forward")) {
				m_direction = Direction.Forward;
			} else if (config[i].equals("bidirectional")) {
				m_direction = Direction.Bidirectional;
			} else if (config[i].equals("f")) {
				m_condition = Condition.F;
			} else if (config[i].equals("fe")) {
				m_condition = Condition.FE;
			} else if (config[i].equals("collapseff")) {
				m_collapseScores = true;
			} else if (config[i].equals("allff")) {
				m_collapseScores = false;
			} else {
				UserMessage.add("Illegal part in the lexical reordering configuration string: "
						+ config[i]);
				System.exit(1);
			}
		}
	}

	public LexicalReorderingState createLexicalReorderingState(final InputType input) {
		LexicalReorderingState bwd = null, fwd = null;
		int offset = 0;

		switch (m_direction) {
		case Backward:
		case Bidirectional:
			if (m_phraseBased) { // Same for forward and backward
				bwd = new PhraseBasedReorderingState(this, Direction.Backward, offset);
			} else {
				bwd = new HierarchicalReorderingBackwardState(this, offset);
			}
			offset += m_collapseScores ? 1 : getNumberOfTypes();
			if (m_direction == Direction.Backward)
				return bwd; // else fall through
		case Forward:
			if (m_phraseBased) { // Same for forward and backward
				fwd = new PhraseBasedReorderingState(this, Direction.Forward, offset);
			} else {
				fwd = new HierarchicalReorderingForwardState(this, input.getSize(), offset);
			}
			offset += m_collapseScores ? 1 : getNumberOfTypes();
			if (m_direction == Direction.Forward)
				return fwd;
		}

		return new BidirectionalReorderingState(this, bwd, fwd, 0);
	}

	public int getNumScoreComponents() {
		int score_per_dir = m_collapseScores ? 1 : getNumberOfTypes();
		if (m_direction == Direction.Bidirectional) {
			return 2 * score_per_dir;
		} else {
			return score_per_dir;
		}
	}

	public int getNumberOfTypes() {
		switch (m_modelType) {
		case MSD:
			return 3;
		case MSLR:
			return 4;
		default:
			return 2;
		}
	}

	public ScoreProducer getScoreProducer() {
		return m_scoreProducer;
	}

	public ModelType getModelType() {
		return m_modelType;
	}

	public Direction getDirection() {
		return m_direction;
	}

	public Condition getCondition() {
		return m_condition;
	}

	public boolean isPhraseBased() {
		return m_phraseBased;
	}

	public boolean collapseScores() {
		return m_collapseScores;
	}

}
