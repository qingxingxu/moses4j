package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yonee.moses4j.moses.TypeDef.DecodeType;
import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.CollectionUtils;
import yonee.utils.VERBOSE;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class GenerationDictionary extends StatelessFeatureFunction implements
		Dictionary {

	class OutputWordCollection extends HashMap<Word, ScoreComponentCollection> {

		private static final long serialVersionUID = 1L;

	}

	// /////////////////////////// Dictionary implement
	int m_numScoreComponent;
	FactorMask m_inputFactors = new FactorMask();
	FactorMask m_outputFactors = new FactorMask();

	// ! returns output factor types as specified by the ini file
	public FactorMask getOutputFactorMask() {
		return m_outputFactors;
	}

	// ! returns input factor types as specified by the ini file
	public FactorMask getInputFactorMask() {
		return m_inputFactors;
	}

	public void cleanUp() {

	}

	// ////////////////////

	// typedef std::map<const Word* , OutputWordCollection, WordComparer>
	// Collection;

	protected Map<Word, OutputWordCollection> m_collection = new HashMap<Word, OutputWordCollection>();
	// 1st = source
	// 2nd = target
	protected String m_filePath;

	/**
	 * constructor. \param numFeatures number of score components, as specified
	 * in ini file
	 */
	public GenerationDictionary(int numFeatures,
			ScoreIndexManager scoreIndexManager) {
		m_numScoreComponent = numFeatures;
		scoreIndexManager.addScoreProducer(this);
	}

	// returns Generate
	public DecodeType getDecodeType() {
		return DecodeType.Generate;
	}

	// ! load data file
	public boolean load(int[] input, int[] output, final String filePath,
			FactorDirection direction) throws IOException {

		FactorCollection factorCollection = FactorCollection.instance();

		final int numFeatureValuesInConfig = getNumScoreComponents();

		// factors
		m_inputFactors = new FactorMask(input);
		m_outputFactors = new FactorMask(output);
		VERBOSE.v(2, "GenerationDictionary: input=" + m_inputFactors
				+ "  output=" + m_outputFactors + "\n");

		// data from file
		BufferedReader inFile = new BufferedReader(new FileReader(filePath));
		if (inFile == null) {
			UserMessage.add("Couldn't read " + filePath);
			return false;
		}

		m_filePath = filePath;
		String line;
		int lineNum = 0;
		while ((line = inFile.readLine()) != null) {
			++lineNum;
			String[] token = Util.tokenize(line);

			// add each line in generation file into class
			Word inputWord = new Word(); // deleted in destructor
			Word outputWord = new Word();

			// create word with certain factors filled out

			// inputs
			String[] factorString = Util.tokenize(token[0], "|");
			for (int i = 0; i < input.length; i++) {
				int factorType = input[i];
				Factor factor = factorCollection.addFactor(direction,
						factorType, factorString[i]);
				inputWord.setFactor(factorType, factor);
			}

			factorString = Util.tokenize(token[1], "|");
			for (int i = 0; i < output.length; i++) {
				int factorType = output[i];
				final Factor factor = factorCollection.addFactor(direction,
						factorType, factorString[i]);
				outputWord.setFactor(factorType, factor);
			}

			int numFeaturesInFile = token.length - 2;
			if (numFeaturesInFile < numFeatureValuesInConfig) {
				StringBuilder strme = new StringBuilder();
				strme.append(filePath).append(":").append(lineNum).append(
						": expected ").append(numFeatureValuesInConfig).append(
						" feature values, but found ")
						.append(numFeaturesInFile).append("\n");
				UserMessage.add(strme.toString());
				return false;
			}
			List<Float> scores = new ArrayList<Float>();
			CollectionUtils.init(scores, numFeatureValuesInConfig, 0.0f);
			for (int i = 0; i < numFeatureValuesInConfig; i++)
				scores.set(i, Util.floorScore(Util.transformScore(Float
						.valueOf(token[2 + i]))));

			OutputWordCollection owc = m_collection.get(inputWord);
			if (owc == null) {
				owc = new OutputWordCollection();
				ScoreComponentCollection scc = new ScoreComponentCollection();
				scc.assign(this, scores);
				owc.put(outputWord, scc);
				m_collection.put(inputWord, owc);

			} else {
				ScoreComponentCollection scc = new ScoreComponentCollection();
				scc.assign(this, scores);
				owc.put(outputWord, scc);
				inputWord = null;
			}

		}
		inFile.close();
		return true;
	}

	public int getNumScoreComponents() {
		return m_numScoreComponent;
	}

	public String getScoreProducerDescription() {
		return "Generation score, file=" + m_filePath;
	}

	public String getScoreProducerWeightShortName() {
		return "g";
	}

	/**
	 * number of unique input entries in the generation table. NOT the number of
	 * lines in the generation table
	 */
	public int getSize() {
		return m_collection.size();
	}

	/**
	 * returns a bag of output words, OutputWordCollection, for a particular
	 * input word. Or NULL if the input word isn't found. The search function
	 * used is the WordComparer functor
	 */
	public OutputWordCollection findWord(Word word) {
		return m_collection.get(word);
	}

	public boolean computeValueInTranslationOption() {
		return true;
	}

}
