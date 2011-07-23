package yonee.moses4j.moses;


import java.util.List;

import yonee.moses4j.moses.TypeDef.FactorDirection;


/**
 * 
 * @author YONEE
 * @OK
 */
public class LexicalReordering extends StatefulFeatureFunction {

	// private boolean decodeCondition(String s) {
	//
	// }
	//
	// private boolean decodeDirection(String s) {
	//
	// }
	//
	// private boolean decodeNumFeatureFunctions(String s) {
	//
	// }

	private LexicalReorderingConfiguration m_configuration;
	private String m_modelTypeString;
	//private List<String> m_modelType = new ArrayList<String>();
	private LexicalReorderingTable m_table;
	private int m_numScoreComponents;
	// List<Direction> m_direction;
	//private List<LexicalReorderingConfiguration.Condition> m_condition = new ArrayList<LexicalReorderingConfiguration.Condition>();
	// List<int> m_scoreOffset;
	// boolean m_oneScorePerDirection;
	private int[] m_factorsE, m_factorsF;

	public LexicalReordering(int[] f_factors, int[] e_factors, String modelType, String filePath,
			List<Float> weights) {
		m_configuration = new LexicalReorderingConfiguration(this, modelType);
		System.err.print("Creating lexical reordering...\n");
		System.err.print("weights: ");
		for (int w = 0; w < weights.size(); ++w) {
			System.err.print(weights.get(w) + " ");
		}
		System.err.print("\n");

		m_modelTypeString = modelType;

		switch (m_configuration.getCondition()) {
		case FE:
		case E:
			m_factorsE = e_factors;
			if (m_factorsE == null || m_factorsE.length == 0) {
				UserMessage.add("TL factor mask for lexical reordering is unexpectedly empty");
				System.exit(1);
			}
			if (m_configuration.getCondition() == LexicalReorderingConfiguration.Condition.E)
				break; // else fall through
		case F:
			m_factorsF = f_factors;
			if (m_factorsF == null || m_factorsF.length == 0) {
				UserMessage.add("SL factor mask for lexical reordering is unexpectedly empty");
				System.exit(1);
			}
			break;
		default:
			UserMessage.add("Unknown conditioning option!");
			System.exit(1);
		}

		if (weights.size() != m_configuration.getNumScoreComponents()) {
			StringBuilder os = new StringBuilder();
			os.append("Lexical reordering model (type ").append(modelType).append("): expected ")
					.append(m_numScoreComponents).append(" weights, got ").append(weights.size())
					.append('\n');
			UserMessage.add(os.toString());
			System.exit(1);
		}

		// add ScoreProducer - don't do this before our object is set up
		StaticData.instance().getScoreIndexManager().addScoreProducer(this);
		StaticData.instance().setWeightsForScoreProducer(this, weights);

		m_table = LexicalReorderingTable.loadAvailable(filePath, m_factorsF, m_factorsE,
				new int[1]);
	}

	public int getNumScoreComponents() {
		return m_configuration.getNumScoreComponents();
	}

	public FFState evaluate(final Hypothesis hypo, final FFState prev_state,
			ScoreComponentCollection out) {
		float[] score = new float[getNumScoreComponents()];
		//CollectionUtils.init(score, getNumScoreComponents(), null);
		LexicalReorderingState prev = (LexicalReorderingState) (prev_state);
		LexicalReorderingState next_state = prev.expand(hypo.getTranslationOption(), score);

		out.plusEquals(this, score);

		return next_state;
	}

	public FFState emptyHypothesisState(final InputType input) {
		return m_configuration.createLexicalReorderingState(input);
	}

	public String getScoreProducerDescription() {
		return "LexicalReordering_" + m_modelTypeString;
	}

	public String getScoreProducerWeightShortName() {
		return "d";
	};

	public void initializeForInput(InputType i) {
		m_table.initializeForInput(i);
	}

	public List<Float> getProb(final Phrase f, final Phrase e) {
		return m_table.getScore(f, e, new Phrase(FactorDirection.Output));
	}


}
