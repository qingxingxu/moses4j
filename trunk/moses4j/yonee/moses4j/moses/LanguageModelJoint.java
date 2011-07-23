package yonee.moses4j.moses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.Ref;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public  class LanguageModelJoint extends LanguageModelMultiFactor {

	protected LanguageModelSingleFactor m_lmImpl;
	protected List<Integer> m_factorTypesOrdered = new ArrayList<Integer>();

	protected int m_implFactor;

	public LanguageModelJoint(LanguageModelSingleFactor lmImpl,
			boolean registerScore, ScoreIndexManager scoreIndexManager)

	{
		super(registerScore, scoreIndexManager);
		m_lmImpl = lmImpl;
	}

	public boolean load(final String filePath, final List<Integer> factorTypes,
			float weight, int nGramOrder) {
		m_factorTypes = new FactorMask(factorTypes);
		m_weight = weight;
		m_filePath = filePath;
		m_nGramOrder = nGramOrder;

		m_factorTypesOrdered = factorTypes;
		m_implFactor = 0;

		FactorCollection factorCollection = FactorCollection.instance();

		// sentence markers
		for (int index = 0; index < factorTypes.size(); ++index) {
			int factorType = factorTypes.get(index);
			m_sentenceStartArray.set(factorType, factorCollection.addFactor(
					FactorDirection.Output, factorType, TypeDef.BOS_));
			m_sentenceEndArray.set(factorType, factorCollection.addFactor(
					FactorDirection.Output, factorType, TypeDef.EOS_));
		}
		
		try {
			return m_lmImpl.load(filePath, m_implFactor, weight, nGramOrder);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public float getValue(final List<Word> contextFactor) {
		return getValue(contextFactor, null, null);
	}

	public float getValue(final List<Word> contextFactor, Ref<Object> finalState,
			Ref<Integer> len) {
		if (contextFactor.size() == 0) {
			return 0;
		}

		// joint context for internal LM
		List<Word> jointContext = new ArrayList<Word>();

		for (int currPos = 0; currPos < m_nGramOrder; ++currPos) {
			final Word word = contextFactor.get(currPos);

			// add word to chunked context
			StringBuilder stream = new StringBuilder("");

			Factor factor = word.get(m_factorTypesOrdered.get(0));
			stream.append(factor.getString());

			for (int index = 1; index < m_factorTypesOrdered.size(); ++index) {
				int factorType = m_factorTypesOrdered.get(index);
				final Factor factor0 = word.get(factorType);
				stream.append("|").append(factor0.getString());
			}

			factor = FactorCollection.instance().addFactor(
					FactorDirection.Output, m_implFactor, stream.toString());

			Word jointWord = new Word();
			jointWord.setFactor(m_implFactor, factor);
			jointContext.add(jointWord);
		}

		// calc score on chunked phrase
		float ret = m_lmImpl.getValue(jointContext, finalState, len);

		jointContext.clear();

		return ret;
	}

	public LanguageModelJoint(LanguageModelInternal lmImpl, boolean b,
			ScoreIndexManager scoreIndexManager) {
		super(b, scoreIndexManager);
		m_lmImpl = lmImpl;
	}




	


}
