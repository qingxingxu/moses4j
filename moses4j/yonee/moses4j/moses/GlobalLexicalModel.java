package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class GlobalLexicalModel extends StatelessFeatureFunction {

	class DoubleHash extends HashMap<Word, Map<Word, Float>> {

		private static final long serialVersionUID = 1L;

	}

	private DoubleHash m_hash = new DoubleHash();
	private Map<TargetPhrase, Float> m_cache;
	private Sentence m_input;
	private Word m_bias;

	// private FactorMask m_inputFactors = new FactorMask();
	// private FactorMask m_outputFactors = new FactorMask();

	private void loadData(String filePath, final int[] inFactors, final int[] outFactors)
			throws IOException {
		FactorCollection factorCollection = FactorCollection.instance();
		final String factorDelimiter = StaticData.instance().getFactorDelimiter();

		VERBOSE.v(2, "Loading global lexical model from file " + filePath + "\n");

		// m_inputFactors = new FactorMask(inFactors);
		// m_outputFactors = new FactorMask(outFactors);

		BufferedReader inFile = new BufferedReader(new FileReader(filePath));
		// reading in data one line at a time
		int lineNum = 0;
		String line;
		while ((line = inFile.readLine()) != null) {
			++lineNum;
			String token[] = Util.tokenize(line, " ");

			if (token.length != 3) // format checking
			{
				StringBuilder errorMessage = new StringBuilder();
				errorMessage.append("Syntax error at ").append(filePath).append(":")
						.append(lineNum).append('\n').append(line).append('\n');
				UserMessage.add(errorMessage.toString());
				System.exit(0);
			}

			// create the output word
			Word outWord = new Word();
			String[] factorString = Util.tokenize(token[0], factorDelimiter);
			for (int i = 0; i < outFactors.length; i++) {
				final FactorDirection direction = FactorDirection.Output;
				final int factorType = outFactors[i];
				final Factor factor = factorCollection.addFactor(direction, factorType,
						factorString[i]);
				outWord.setFactor(factorType, factor);
			}

			// create the input word
			Word inWord = new Word();
			factorString = Util.tokenize(token[1], factorDelimiter);
			for (int i = 0; i < inFactors.length; i++) {
				final FactorDirection direction = FactorDirection.Input;
				final int factorType = inFactors[i];
				final Factor factor = factorCollection.addFactor(direction, factorType,
						factorString[i]);
				inWord.setFactor(factorType, factor);
			}

			// maximum entropy feature score
			float score = Float.valueOf(token[2]);

			// std::cerr << "storing word " << *outWord << " " << *inWord <<
			// " " << score << endl;

			// store feature in hash
			Map<Word, Float> dh = m_hash.get(outWord);
			if (dh == null) {
				dh = new HashMap<Word, Float>();
				dh.put(inWord, score);
				m_hash.put(outWord, dh);
			} else {
				dh.put(inWord, score);
				outWord = null;
			}
		}
	}

	private float scorePhrase(final TargetPhrase targetPhrase) {
		float score = 0;
		for (int targetIndex = 0; targetIndex < targetPhrase.getSize(); targetIndex++) {
			float sum = 0;
			final Word targetWord = targetPhrase.getWord(targetIndex);
			VERBOSE.v(2, "glm " + targetWord + ": ");
			Map<Word, Float> dh = m_hash.get(targetWord);
			if (dh != null) {
				Float f = dh.get(m_bias);
				if (f != null) {
					VERBOSE.v(2, "*BIAS* " + f);
					sum += f;
				}
				Set<Word> alreadyScored = new HashSet<Word>();
				for (int inputIndex = 0; inputIndex < m_input.getSize(); inputIndex++) {
					final Word inputWord = m_input.getWord(inputIndex);
					if (!alreadyScored.contains(inputWord)) {
						Float w = dh.get(inputWord);
						if (w != null) {
							VERBOSE.v(2, " " + inputWord + " " + w);
							sum += w;
						}
						alreadyScored.add(inputWord);
					}
				}
			}

			final Map<Word, Float> targetWordHash = m_hash.get(targetWord);
			if (targetWordHash != null) {
				Float inputWordHash = targetWordHash.get(m_bias);
				if (inputWordHash != null) {
					VERBOSE.v(2, "*BIAS* " + inputWordHash);
					sum += inputWordHash;
				}

				Set<Word> alreadyScored = new HashSet<Word>(); // do not score a
				// word twice
				for (int inputIndex = 0; inputIndex < m_input.getSize(); inputIndex++) {
					final Word inputWord = m_input.getWord(inputIndex);
					if (!alreadyScored.contains(inputWord)) {
						Float inputWordHash0 = targetWordHash.get(inputWord);
						if (inputWordHash0 != null) {
							VERBOSE.v(2, " " + inputWord + " " + inputWordHash0);
							sum += inputWordHash0;
						}
						alreadyScored.add(inputWord);
					}
				}
			}
			// Hal Daume says: 1/( 1 + exp [ - sum_i w_i * f_i ] )
			VERBOSE
					.v(2, " p=" + Util.floorScore((float) Math.log(1 / (1 + Math.exp(-sum))))
							+ '\n');
			score += Util.floorScore((float) Math.log(1 / (1 + Math.exp(-sum))));
		}
		return score;
	}

	private float getFromCacheOrScorePhrase(final TargetPhrase targetPhrase) {
		Float query = m_cache.get(targetPhrase);
		if (query != null) {
			return query;
		}

		float score = scorePhrase(targetPhrase);
		m_cache.put(targetPhrase, score);
		System.err.println("add to cache " + targetPhrase + ": " + score + '\n');
		return score;
	}

	public GlobalLexicalModel(final String filePath, final float weight, final int[] inFactors,
			final int[] outFactors) {
		System.err.print("Creating global lexical model...\n");

		// register as score producer
		StaticData.instance().getScoreIndexManager().addScoreProducer(this);

		List<Float> weights = new ArrayList<Float>();
		weights.add(weight);
		StaticData.instance().setWeightsForScoreProducer(this, weights);

		// load model

		try {
			loadData(filePath, inFactors, outFactors);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// define bias word
		FactorCollection factorCollection = FactorCollection.instance();
		m_bias = new Word();
		final Factor factor = factorCollection.addFactor(FactorDirection.Input, inFactors[0],
				"**BIAS**");
		m_bias.setFactor(inFactors[0], factor);

		m_cache = null;
	}

	public int getNumScoreComponents() {
		return 1;
	}

	public String getScoreProducerDescription() {
		return "GlobalLexicalModel";
	}

	public String getScoreProducerWeightShortName() {
		return "lex";
	}

	public void initializeForInput(Sentence in) {
		m_input = in;
		if (m_cache != null)
			m_cache = null;
		m_cache = new HashMap<TargetPhrase, Float>();
	}

	public void evaluate(final TargetPhrase targetPhrase, ScoreComponentCollection accumulator) {
		accumulator.plusEquals(this, getFromCacheOrScorePhrase(targetPhrase));
	}

}
