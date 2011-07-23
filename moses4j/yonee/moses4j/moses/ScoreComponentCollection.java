package yonee.moses4j.moses;

import java.util.Arrays;
import java.util.List;

import yonee.utils.ASSERT;

/**
 * 
 * @author YONEE
 * @OK<<
 */
public class ScoreComponentCollection {

	// friend std::ostream& operator<<(std::ostream& os, const ScoreComponentCollection& rhs);
	// friend class ScoreIndexManager;
	private float[] m_scores;
	private final ScoreIndexManager m_sim;

	// ! Create a new score collection with all values set to 0.0
	public ScoreComponentCollection() {
		m_scores = new float[StaticData.instance().getTotalScoreComponents()];
		Arrays.fill(m_scores, 0f);
		m_sim = StaticData.instance().getScoreIndexManager();
	}

	// ! Clone a score collection
	public ScoreComponentCollection(final ScoreComponentCollection rhs)

	{
		m_scores = rhs.m_scores;
		m_sim = rhs.m_sim;
	}

	public final int size() {
		return m_scores.length;
	}

	public final float get(int x) {
		return m_scores[x];
	}

	// ! Set all values to 0.0
	public void zeroAll() {
		for (int i = 0; i < m_scores.length; i++) {
			m_scores[i] = 0;
		}

	}

	// ! add the score in rhs
	public void plusEquals(final ScoreComponentCollection rhs) {
		assert (m_scores.length >= rhs.m_scores.length);
		final int l = rhs.m_scores.length;
		for (int i = 0; i < l; i++) {
			m_scores[i] += rhs.m_scores[i];
		}
	}

	// ! subtract the score in rhs
	public void minusEquals(final ScoreComponentCollection rhs) {
		assert (m_scores.length >= rhs.m_scores.length);
		final int l = rhs.m_scores.length;
		for (int i = 0; i < l; i++) {
			m_scores[i] -= rhs.m_scores[i];
		}
	}

	// ! Add scores from a single ScoreProducer only
	// ! The length of scores must be equal to the number of score components
	// ! produced by sp
	public void plusEquals(final ScoreProducer sp, final float[] scores) {
		ASSERT.a(scores.length == sp.getNumScoreComponents());
		int i = m_sim.getBeginIndex(sp.getScoreBookkeepingID());
		for (float vi : scores) {
			m_scores[i] += vi;
			i++;
		}
	}

	// ! Add scores from a single ScoreProducer only
	// ! The length of scores must be equal to the number of score components
	// ! produced by sp
	public void plusEquals(final ScoreProducer sp, final ScoreComponentCollection scores) {
		int i = m_sim.getBeginIndex(sp.getScoreBookkeepingID());
		final int end = m_sim.getEndIndex(sp.getScoreBookkeepingID());
		for (; i < end; ++i) {
			m_scores[i] += scores.m_scores[i];
		}
	}

	// ! Special version PlusEquals(ScoreProducer, vector<float>)
	// ! to add the score from a single ScoreProducer that produces
	// ! a single value
	public void plusEquals(final ScoreProducer sp, float score) {
		ASSERT.a(1 == sp.getNumScoreComponents());
		final int i = m_sim.getBeginIndex(sp.getScoreBookkeepingID());
		m_scores[i] += score;
	}

	public void assign(final ScoreProducer sp, final List<Float> scores) {
		assert (scores.size() == sp.getNumScoreComponents());
		int i = m_sim.getBeginIndex(sp.getScoreBookkeepingID());
		for (float vi : scores) {
			m_scores[i] += vi;

		}
	}

	public void assign(final ScoreComponentCollection copy) {
		m_scores = copy.m_scores;
	}

	// ! Special version PlusEquals(ScoreProducer, vector<float>)
	// ! to add the score from a single ScoreProducer that produces
	// ! a single value
	public void assign(final ScoreProducer sp, float score) {
		ASSERT.a(1 == sp.getNumScoreComponents());
		final int i = m_sim.getBeginIndex(sp.getScoreBookkeepingID());
		m_scores[i] = score;
	}

	// ! Used to find the weighted total of scores. rhs should contain a vector of weights
	// ! of the same length as the number of scores.
	public float innerProduct(final List<Float> rhs) {
		return Util.innerProduct(m_scores, 0, m_scores.length, rhs, 0, 0.0f);
	}

	public float partialInnerProduct(final ScoreProducer sp, final List<Float> rhs) {
		float[] lhs = getScoresForProducer(sp);
		ASSERT.a(lhs.length == rhs.size());
		return Util.innerProduct(lhs, 0, lhs.length, rhs, 0, 0.0f);
	}

	// ! return a vector of all the scores associated with a certain ScoreProducer
	public float[] getScoresForProducer(final ScoreProducer sp) {
		int id = sp.getScoreBookkeepingID();
		final int begin = m_sim.getBeginIndex(id);
		final int end = m_sim.getEndIndex(id);
		float[] res = new float[end - begin];
		int j = 0;
		for (int i = begin; i < end; i++) {
			res[j++] = m_scores[i];
		}
		return res;

	}

	// ! if a ScoreProducer produces a single score (for example, a language model score)
	// ! this will return it. If not, this method will throw
	public float getScoreForProducer(final ScoreProducer sp) {
		int id = sp.getScoreBookkeepingID();
		final int begin = m_sim.getBeginIndex(id);
		// #ifndef NDEBUG
		// final int end = m_sim.getEndIndex(id);
		// assert(end-begin == 1);
		// #endif
		return m_scores[begin];
	}

	public float getWeightedScore() {
		return innerProduct(StaticData.instance().getAllWeights());
	}

	public void zeroAllLM() {
		final LMList lmList = StaticData.instance().getAllLM();

		for (int ind = lmList.getMinIndex(); ind <= lmList.getMaxIndex(); ++ind) {
			m_scores[ind] = 0f;
		}
	}

	public void plusEqualsAllLM(final ScoreComponentCollection rhs) {
		final LMList lmList = StaticData.instance().getAllLM();

		for (int ind = lmList.getMinIndex(); ind <= lmList.getMaxIndex(); ++ind) {
			m_scores[ind] += rhs.m_scores[ind];
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<<").append(m_scores[0]);
		for (int i = 1; i < m_scores.length; i++)
			sb.append(", ").append(m_scores[i]);
		return sb.append(">>").toString();
	}
}
