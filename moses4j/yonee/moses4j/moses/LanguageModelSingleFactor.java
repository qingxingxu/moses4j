package yonee.moses4j.moses;

import java.io.IOException;

import yonee.moses4j.moses.TypeDef.LMType;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public abstract class LanguageModelSingleFactor extends LanguageModel {

	protected Factor m_sentenceStart, m_sentenceEnd;
	protected int m_factorType;

	protected LanguageModelSingleFactor(boolean registerScore,
			ScoreIndexManager scoreIndexManager) {
		super(registerScore, scoreIndexManager);
	}

	public static Integer UnknownState;

	public abstract boolean load(final String filePath, int factorType,
			float weight, int nGramOrder) throws IOException;

	public LMType getLMType() {
		return LMType.SingleFactor;
	}

	public boolean useable(final Phrase phrase) {
		return (phrase.getSize() > 0 && phrase.getFactor(0, m_factorType) != null);
	}

	public final Factor getSentenceStart() {
		return m_sentenceStart;
	}

	public final Factor getSentenceEnd() {
		return m_sentenceEnd;
	}

	public int getFactorType() {
		return m_factorType;
	}

	public float getWeight() {
		return m_weight;
	}

	public void setWeight(float weight) {
		m_weight = weight;
	}

	public String getScoreProducerDescription() {
		StringBuilder oss = new StringBuilder();
		// what about LMs that are over multiple factors at once, POS + stem,
		// for example?
		oss.append("LM_").append(getNGramOrder()).append("gram");
		return oss.toString();
	}
}
