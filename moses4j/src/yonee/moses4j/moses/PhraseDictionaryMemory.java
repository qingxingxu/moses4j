package yonee.moses4j.moses;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.ASSERT;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class PhraseDictionaryMemory extends PhraseDictionary {

	// typedef PhraseDictionary MyBase;
	// friend std::ostream& operator<<(std::ostream&, const
	// PhraseDictionaryMemory&);

	protected PhraseDictionaryNode m_collection = new PhraseDictionaryNode();

	protected TargetPhraseCollection createTargetPhraseCollection(final Phrase source) {
		final int size = source.getSize();

		PhraseDictionaryNode currNode = m_collection;
		for (int pos = 0; pos < size; ++pos) {
			final Word word = source.getWord(pos);
			currNode = currNode.getOrCreateChild(word);
			if (currNode == null)
				return null;
		}

		return currNode.createTargetPhraseCollection();
	}

	public PhraseDictionaryMemory(int numScoreComponent, PhraseDictionaryFeature feature) {
		super(numScoreComponent, feature);
	}

	public boolean load(final int[] input, final int[] output, final String filePath,
			final List<Float> weight, int tableLimit, final LMList languageModels, float weightWP)
			throws IOException {
		final StaticData staticData = StaticData.instance();

		m_tableLimit = tableLimit;

		// factors
		m_inputFactors = new FactorMask(input);
		m_outputFactors = new FactorMask(output);
		VERBOSE.v(2, "PhraseDictionaryMemory: input=" + m_inputFactors + "  output="
				+ m_outputFactors + "\n");

		// data from file
		BufferedReader inFile = new BufferedReader(new FileReader(filePath));

		// create hash file if necessary
		// ofstream tempFile;
		// String tempFilePath;

		List<String[]> phraseVector = new ArrayList<String[]>();
		String line, prevSourcePhrase = "";
		int count = 0;
		int line_num = 0;
		int numElement = TypeDef.NOT_FOUND; // 3=old format, 5=async format
		// which include word alignment info

		while ((line = inFile.readLine()) != null) {
			++line_num;
			String[] tokens = Util.tokenizeMultiCharSeparator(line, "|||");

			if (numElement == TypeDef.NOT_FOUND) { // init numElement
				numElement = tokens.length;
				ASSERT.a(numElement >= 3);
				// extended style: source ||| target ||| scores |||
				// [alignment] ||| [counts]
			}

			if (tokens.length != numElement) {
				StringBuilder strme = new StringBuilder();
				strme.append("Syntax error at ").append(filePath).append(":").append(line_num);
				UserMessage.add(strme.toString());
				System.exit(0);
			}

			final String sourcePhraseString = tokens[0], targetPhraseString = tokens[1], scoreString = tokens[2];

			boolean isLHSEmpty = (Util.findFirstNotOf(sourcePhraseString, " \t", 0) == -1);

			if (isLHSEmpty && !staticData.isWordDeletionEnabled()) {
				TRACE.err(filePath + ":" + line_num
						+ ": pt entry contains empty target, skipping\n");
				continue;
			}

			final String factorDelimiter = StaticData.instance().getFactorDelimiter();
			if (sourcePhraseString != prevSourcePhrase)
				phraseVector = Phrase.parse(sourcePhraseString, input, factorDelimiter);

			float[] scoreVector = Util.tokenizeFloat(scoreString);
			if (scoreVector.length != m_numScoreComponent) {
				StringBuilder strme = new StringBuilder();
				strme.append("Size of scoreVector != number (").append(scoreVector.length).append(
						"!=").append(m_numScoreComponent).append(") of score components on line ")
						.append(line_num);
				UserMessage.add(strme.toString());
				System.exit(0);
			}

			// source
			Phrase sourcePhrase = new Phrase(FactorDirection.Input);
			sourcePhrase.createFromString(input, phraseVector);
			// target
			TargetPhrase targetPhrase = new TargetPhrase(FactorDirection.Output);

			targetPhrase.setSourcePhrase(sourcePhrase);
			targetPhrase.createFromString(output, targetPhraseString, factorDelimiter);

			if (tokens.length > 3)
				targetPhrase.setAlignmentInfo(tokens[3]);

			// component score, for n-best output

			float[] scv = Util.floorScore(Util.transformScore(scoreVector));

			targetPhrase.setScore(m_feature, scv, weight, weightWP, languageModels);

			addEquivPhrase(sourcePhrase, targetPhrase);


			count++;
		}
		// sort each target phrase collection
		m_collection.sort(m_tableLimit);
		
		return true;

	}

	public TargetPhraseCollection getTargetPhraseCollection(final Phrase source) {
		final int size = source.getSize();

		PhraseDictionaryNode currNode = m_collection;
		for (int pos = 0; pos < size; ++pos) {
			final Word word = source.getWord(pos);
			currNode = currNode.getChild(word);
			if (currNode == null)
				return null;
		}

		return currNode.getTargetPhraseCollection();
	}

	public void addEquivPhrase(final Phrase source, final TargetPhrase targetPhrase) {
		TargetPhraseCollection phraseColl = createTargetPhraseCollection(source);
		phraseColl.add(new TargetPhrase(targetPhrase));
	}

	// for mert
	public void setWeightTransModel(final List<Float> weightT) {
		for (Entry<Word, PhraseDictionaryNode> p : m_collection.entrySet()) {
			p.getValue().setWeightTransModel(this, weightT);
		}

	}

	public void initializeForInput(InputType inputType) {/*
														 * Don't do anything
														 * source specific here
														 * as this object is
														 * shared between
														 * threads.
														 */
	}

	public final ChartRuleCollection getChartRuleCollection(InputType fsrc, WordsRange range,
			boolean adhereTableLimit, final CellCollection cellColl) {
		assert (false);
		return null;
	}

	// TO_STRING();

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<Word, PhraseDictionaryNode> e : m_collection.entrySet()) {
			sb.append(e.getKey());
		}
		return sb.toString();
	}

}
