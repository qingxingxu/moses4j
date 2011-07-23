package yonee.moses4j.moses;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public abstract class ScoreProducer {

	private static int s_globalScoreBookkeepingIdCounter;
	private int m_scoreBookkeepingId;

	/*
	 * private ScoreProducer(ScoreProducer sp) // don't implement {
	 * m_scoreBookkeepingId = UNASSIGNED; }
	 */

	public static int UNASSIGNED = 0xffffffff;

	// it would be nice to force registration here, but some Producer objects
	// are finalructed before they know how many scores they have
	protected ScoreProducer() {
	}

	// ! contiguous id
	public int getScoreBookkeepingID() {
		return m_scoreBookkeepingId;
	}

	public void createScoreBookkeepingID() {
		m_scoreBookkeepingId = s_globalScoreBookkeepingIdCounter++;
	}

	// ! returns the number of scores that a subclass produces.
	// ! For example, a language model conventionally produces 1, a translation
	// table some arbitrary number, etc
	public abstract int getNumScoreComponents();

	// ! returns a string description of this producer
	public abstract String getScoreProducerDescription();

	// ! returns the weight parameter name of this producer (used in n-best
	// list)
	public abstract String getScoreProducerWeightShortName();

	// ! returns the number of scores gathered from the input (0 by default)
	public int getNumInputScores() {
		return 0;
	}

	public abstract boolean isStateless();

}
