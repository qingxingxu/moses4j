package yonee.moses4j.moses;

import java.util.List;

import yonee.moses4j.moses.TypeDef.DecodeType;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public abstract class PhraseDictionary implements Dictionary {
	
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

	// ////////////////////// ////////////////////// //////////////////////
	// ////////////////////// ////////////////////

	protected int m_tableLimit;
	protected PhraseDictionaryFeature m_feature;

	public PhraseDictionary(int numScoreComponent, final PhraseDictionaryFeature feature) {
		m_numScoreComponent = numScoreComponent;
		m_tableLimit = 0;
		m_feature = feature;
	}

	// ! table limit number.
	public int getTableLimit() {
		return m_tableLimit;
	}

	public DecodeType getDecodeType() {
		return DecodeType.Translate;
	}

	public PhraseDictionaryFeature getFeature() {
		return m_feature;
	}

	/**
	 * set/change translation weights and recalc weighted score for each
	 * translation. TODO This may be redundant now we use ScoreCollection
	 */
	public abstract void setWeightTransModel(final List<Float> weightT);

	// ! find list of translations that can translates src. Only for phrase
	// input
	public abstract TargetPhraseCollection getTargetPhraseCollection(final Phrase src);

	// ! find list of translations that can translates a portion of src. Used by
	// confusion network decoding
	public TargetPhraseCollection getTargetPhraseCollection(InputType src, WordsRange range) {
		return getTargetPhraseCollection(src.getSubString(range));
	}

	// ! Create entry for translation of source to targetPhrase
	public abstract void addEquivPhrase(final Phrase source, final TargetPhrase targetPhrase);

	public abstract void initializeForInput(InputType source);

	public abstract ChartRuleCollection getChartRuleCollection(InputType src, WordsRange range,
			boolean adhereTableLimit, final CellCollection cellColl);

}
