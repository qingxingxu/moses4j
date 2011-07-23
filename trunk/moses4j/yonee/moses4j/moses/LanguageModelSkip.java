package yonee.moses4j.moses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.Ref;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class LanguageModelSkip extends LanguageModelSingleFactor {

	protected int m_realNGramOrder;
	protected LanguageModelSingleFactor m_lmImpl;

	/**
	 * Constructor \param lmImpl SRI or IRST LM which this LM can use to load
	 * data
	 */
	public LanguageModelSkip(LanguageModelSingleFactor lmImpl,
			boolean registerScore, ScoreIndexManager scoreIndexManager)

	{
		super(registerScore, scoreIndexManager);
		m_lmImpl = lmImpl;
	}

	public boolean load(final String filePath, int factorType, float weight,
			int nGramOrder) {
		m_factorType = factorType;
		m_weight = weight;
		m_filePath = filePath;
		m_nGramOrder = nGramOrder;

		m_realNGramOrder = 3;

		FactorCollection factorCollection = FactorCollection.instance();

		m_sentenceStartArray.set(m_factorType, factorCollection.addFactor(
				FactorDirection.Output, m_factorType, TypeDef.BOS_));
		m_sentenceEndArray.set(m_factorType, factorCollection.addFactor(
				FactorDirection.Output, m_factorType, TypeDef.EOS_));

		try {
			return m_lmImpl.load(filePath, m_factorType, weight, nGramOrder);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public float getValue(List<Word> contextFactor, Ref<Object> finalState,
			Ref<Integer> len) {
		if (contextFactor.size() == 0) {
			return 0;
		}

		// only process context where last word is a word we want
		Factor factor = contextFactor.get(contextFactor.size() - 1).get(
				m_factorType);
		String strWord = factor.getString();
		if (!strWord.contains("---"))
			return 0;

		// add last word
		List<Word> chunkContext = new ArrayList<Word>();
		Word chunkWord = new Word();
		chunkWord.setFactor(m_factorType, factor);
		chunkContext.add(chunkWord);

		// create context in reverse 'cos we skip words we don't want
		for (int currPos = (int) contextFactor.size() - 2; currPos >= 0
				&& chunkContext.size() < m_realNGramOrder; --currPos) {
			final Word word = contextFactor.get(currPos);
			factor = word.get(m_factorType);
			String strWord0 = factor.getString();
			boolean skip = !strWord0.contains("---");
			if (skip)
				continue;

			// add word to chunked context
			Word chunkWord0 = new Word();
			chunkWord0.setFactor(m_factorType, factor);
			chunkContext.add(chunkWord0);
		}

		// create context factor the right way round
		// std::reverse(chunkContext.begin(), chunkContext.end());
		Collections.reverse(chunkContext);

		// calc score on chunked phrase
		float ret = m_lmImpl.getValue(chunkContext, finalState, len);

		chunkContext.clear();

		return ret;
	}

	public LanguageModelSkip(LanguageModelInternal languageModelInternal,
			boolean b, ScoreIndexManager scoreIndexManager) {
		super(b, scoreIndexManager);
	}

}
