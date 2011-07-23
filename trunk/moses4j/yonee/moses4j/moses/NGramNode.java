package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK
 */
public class NGramNode {

	protected float m_score, m_logBackOff;
	protected NGramCollection m_map;
	protected NGramNode m_rootNGram;

	public NGramNode() {
		m_map = new NGramCollection();
	}

	public NGramCollection getNGramColl() {
		return m_map;
	}

	public NGramNode getNGram(final Factor factor) {
		return m_map.getNGram(factor);
	}

	public NGramNode getRootNGram() {
		return m_rootNGram;
	}

	public void setRootNGram(NGramNode rootNGram) {
		m_rootNGram = rootNGram;
	}

	public float getScore() {
		return m_score;
	}

	public float getLogBackOff() {
		return m_logBackOff;
	}

	public void setScore(float score) {
		m_score = score;
	}

	public void setLogBackOff(float logBackOff) {
		m_logBackOff = logBackOff;
	}

}
