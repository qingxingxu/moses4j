package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.moses4j.moses.TypeDef.SearchAlgorithm;
import yonee.moses4j.moses.TypeDef.XmlInputType;
import yonee.utils.ASSERT;
import yonee.utils.CollectionUtils;
import yonee.utils.TRACE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class Sentence extends Phrase implements InputType {
	//
	InputType.Impl base = new InputType.Impl();

	public InputType.Impl i() {
		return this.base;
	}

	//

	/**
	 * Utility method that takes in a string representing an XML tag and the name of the attribute,
	 * and returns the value of that tag if present, empty string otherwise
	 */
	private List<TranslationOption> m_xmlOptionsList;
	private List<Boolean> m_xmlCoverageMap;

	private List<Word> m_defaultLabelList = new ArrayList<Word>();

	private void initStartEndWord() {
		FactorCollection factorCollection = FactorCollection.instance();

		Word startWord = new Word();
		Factor factor = factorCollection.addFactor(FactorDirection.Input, 0, TypeDef.BOS_); // TODO
		// -
		// non-factored
		startWord.setFactor(0, factor);
		prependWord(startWord);

		Word endWord = new Word();
		factor = factorCollection.addFactor(FactorDirection.Input, 0, TypeDef.EOS_); // TODO -
		// non-factored
		endWord.setFactor(0, factor);
		addWord(endWord);
	}

	public Sentence(FactorDirection direction) {
		super(direction);
		// InputType()

		ASSERT.a(direction == FactorDirection.Input);
		m_defaultLabelList.add(StaticData.instance().getInputDefaultNonTerminal());
	}

	public InputTypeEnum getType() {
		return InputTypeEnum.SentenceInput;
	}

	// ! Calls Phrase::GetSubString(). Implements abstract InputType::GetSubString()
	public Phrase getSubString(final WordsRange r) {
		return super.getSubString(r);
	}

	// ! Calls Phrase::GetWord(). Implements abstract InputType::GetWord()
	public final Word getWord(int pos) {
		return super.getWord(pos);
	}

	// ! Calls Phrase::GetSize(). Implements abstract InputType::GetSize()
	public int getSize() {
		return super.getSize();
	}

	// ! Returns true if there were any XML tags parsed that at least partially covered the range
	// passed
	public boolean xmlOverlap(int startPos, int endPos) {
		for (int pos = startPos; pos <= endPos; pos++) {
			if (pos < m_xmlCoverageMap.size() && m_xmlCoverageMap.get(pos) != null) {
				return true;
			}
		}
		return false;
	}

	// ! populates vector argument with XML force translation options for the specific range passed
	public void getXmlTranslationOptions(List<TranslationOption> list, int startPos, int endPos) {
		for (TranslationOption xmlOPts : m_xmlOptionsList) {
			if (startPos == xmlOPts.getSourceWordsRange().getStartPos()
					&& endPos == xmlOPts.getSourceWordsRange().getEndPos()) {
				list.add(xmlOPts);
			}
		}
	}

	public int read(BufferedReader in, final int[] factorOrder) throws IOException {
		final String factorDelimiter = StaticData.instance().getFactorDelimiter();
		String line;
		Map<String, String> meta = new HashMap<String, String>();

		if ((line = in.readLine()) == null)
			return 0;

		// get covered words - if continual-partial-translation is switched on, parse input
		final StaticData staticData = StaticData.instance();
		base.m_frontSpanCoveredLength = 0;
		CollectionUtils.resize(base.m_sourceCompleted, 0, null);
		if (staticData.continuePartialTranslation()) {
			String initialTargetPhrase;
			String sourceCompletedStr;
			int loc1 = line.indexOf("|||", 0);
			int loc2 = line.indexOf("|||", loc1 + 3);
			if (loc1 > -1 && loc2 > -1) {
				initialTargetPhrase = line.substring(0, loc1);
				sourceCompletedStr = line.substring(loc1 + 3, loc2);
				line = line.substring(loc2 + 3);
				sourceCompletedStr = sourceCompletedStr.trim();
				initialTargetPhrase = initialTargetPhrase.trim();
				base.m_initialTargetPhrase = initialTargetPhrase;
				int len = sourceCompletedStr.length();
				CollectionUtils.resize(base.m_sourceCompleted, len, Boolean.class);

				int contiguous = 1;
				for (int i = 0; i < len; ++i) {
					if (sourceCompletedStr.charAt(i) == '1') {
						base.m_sourceCompleted.set(i, true);
						if (contiguous != 0)
							base.m_frontSpanCoveredLength++;
					} else {
						base.m_sourceCompleted.set(i, false);
						contiguous = 0;
					}
				}
			}
		}

		// remove extra spaces
		line = line.trim();

		// if sentences is specified as "<seg id=1> ... </seg>", extract id
		meta = Util.processAndStripSGML(line);
		String s = meta.get("id");
		if (s != null) {
			base.setTranslationId(Long.valueOf(meta.get("id")));
		}

		// parse XML markup in translation line
		// final StaticData &staticData = StaticData.Instance();
		List<List<XmlOption>> xmlOptionsList = new ArrayList<List<XmlOption>>();
		List<Integer> xmlWalls = new ArrayList<Integer>();
		if (staticData.getXmlInputType() != XmlInputType.XmlPassThrough) {
			if (!XmlOption.processAndStripXMLTags(line, xmlOptionsList,
					base.m_reorderingConstraint, xmlWalls)) {
				final String msg = ("Unable to parse XML in line: " + line);
				TRACE.err(msg + "\n");
				throw new RuntimeException(msg);
			}
		}
		super.createFromString(factorOrder, line, factorDelimiter);

		if (staticData.getSearchAlgorithm() == SearchAlgorithm.ChartDecoding) {
			initStartEndWord();
		}

		// now that we have final word positions in phrase (from CreateFromString),
		// we can make input phrase objects to go with our XmlOptions and create TranslationOptions

		// only fill the vector if we are parsing XML
		if (staticData.getXmlInputType() != XmlInputType.XmlPassThrough) {
			for (int i = 0; i < getSize(); i++) {
				m_xmlCoverageMap.add(false);
			}

			// iterXMLOpts will be empty for XmlIgnore
			// look at each column
			for (List<XmlOption> iterXmlOpts : xmlOptionsList) {

				// now we are looking through one column of linked things.
				// TODO: We could drop this inner loop if we didn't support linked opts.
				// we could loop once, make the new TranslationOption, note its pos. in the
				// coverageMap,
				// and delete the XmlOption -JS
				List<TranslationOption> linkedTransOpts = new ArrayList<TranslationOption>();
				for (XmlOption iterLinkedXmlOpts : iterXmlOpts) {
					// make each item into a translation option
					TranslationOption transOpt = new TranslationOption(iterLinkedXmlOpts.range,
							iterLinkedXmlOpts.targetPhrase, this);
					// store it temporarily in the linkedTransOpts vector
					linkedTransOpts.add(transOpt);
					iterLinkedXmlOpts = null;
				}

				// now link them up and add to m_XmlOptionsList TODO: this is complicated by linked
				// options. Drop it? -JS
				for (TranslationOption iterLinkedTransOpts1 : linkedTransOpts) {

					for (TranslationOption iterLinkedTransOpts2 : linkedTransOpts) {

						if (iterLinkedTransOpts1 != iterLinkedTransOpts2) {
							iterLinkedTransOpts1.addLinkedTransOpt(iterLinkedTransOpts2);
						}
					} // inner linked opts loop

					// ok everything is linked up and initialized, add it to our list of options and
					// mark locations in coverage map
					TranslationOption transOpt = iterLinkedTransOpts1;

					m_xmlOptionsList.add(transOpt);

					for (int j = transOpt.getSourceWordsRange().getStartPos(); j <= transOpt
							.getSourceWordsRange().getEndPos(); j++) {
						m_xmlCoverageMap.set(j, true);
					}
				}// outer linked opts loop
			}

		}

		base.m_reorderingConstraint.initializeWalls(getSize());

		// set reordering walls, if "-monotone-at-punction" is set
		if (staticData.UseReorderingConstraint()) {
			base.m_reorderingConstraint.setMonotoneAtPunctuation(getSubString(new WordsRange(0,
					getSize() - 1)));
		}

		// set walls obtained from xml
		for (int i = 0; i < xmlWalls.size(); i++)
			if (xmlWalls.get(i) < getSize()) // no buggy walls, please
				base.m_reorderingConstraint.setWall(xmlWalls.get(i), true);
		base.m_reorderingConstraint.finalizeWalls();

		return 1;
	}

	public void print(PrintStream out) {
		out.println((Phrase) (this));
	}

	public TranslationOptionCollection createTranslationOptionCollection() {
		int maxNoTransOptPerCoverage = StaticData.instance().getMaxNoTransOptPerCoverage();
		float transOptThreshold = StaticData.instance().getTranslationOptionThreshold();
		TranslationOptionCollection rv = new TranslationOptionCollectionText(this,
				maxNoTransOptPerCoverage, transOptThreshold);
		ASSERT.a(rv != null);
		return rv;
	}

	public void createFromString(final int[] factorOrder, final String phraseString,
			final String factorDelimiter) {
		super.createFromString(factorOrder, phraseString, factorDelimiter);
	}

	public final List<Word> getLabelList(int startPos, int endPos) {
		return m_defaultLabelList;
	}

}
