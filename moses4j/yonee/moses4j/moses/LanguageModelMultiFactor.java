package yonee.moses4j.moses;

import java.io.IOException;
import java.util.List;

import yonee.moses4j.moses.TypeDef.LMType;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public abstract class LanguageModelMultiFactor extends LanguageModel {

	protected FactorMask m_factorTypes = new FactorMask();

	protected LanguageModelMultiFactor(boolean registerScore,
			ScoreIndexManager scoreIndexManager) {
		super(registerScore, scoreIndexManager);
	}

	public abstract boolean load(String filePath,
			final List<Integer> factorTypes, float weight, int nGramOrder) throws IOException;

	public LMType getLMType() {
		return LMType.MultiFactor;
	}

	public String getScoreProducerDescription() {
		StringBuilder oss = new StringBuilder();
		// what about LMs that are over multiple factors at once, POS + stem,
		// for example?
		oss.append(getNGramOrder()).append("-gram LM score, factor-type= ??? ")
				.append(", file=").append(m_filePath);
		return oss.toString();
	}

	public boolean useable(final Phrase phrase) {
		if (phrase.getSize() == 0)
			return false;

		// whether phrase contains all factors in this LM
		final Word word = phrase.getWord(0);
		for (int currFactor = 0; currFactor < TypeDef.MAX_NUM_FACTORS; ++currFactor) {
			if (m_factorTypes.get(currFactor) && word.get(currFactor) == null)
				return false;
		}
		return true;

	}

}
