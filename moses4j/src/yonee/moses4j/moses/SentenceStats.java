package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       List µÄ³õÊ¼»¯
 */
public class SentenceStats {

	/***
	 * to be called before decoding a sentence
	 */
	public SentenceStats(final InputType source) {
		initialize(source);
	}

	public void initialize(final InputType source) {
		m_numHyposCreated = 0;
		m_numHyposPruned = 0;
		m_numHyposDiscarded = 0;
		m_numHyposEarlyDiscarded = 0;
		m_numHyposNotBuilt = 0;
		m_timeCollectOpts = 0;
		m_timeBuildHyp = 0;
		m_timeEstimateScore = 0;
		m_timeCalcLM = 0;
		m_timeOtherScore = 0;
		m_timeStack = 0;
		m_totalSourceWords = source.getSize();
		m_recombinationInfos.clear();
		m_deletedWords.clear();
		m_insertedWords.clear();
	}

	/***
	 * to be called after decoding a sentence
	 */
	public void calcFinalStats(final Hypothesis bestHypo) {
		// deleted words
		addDeletedWords(bestHypo);
		// inserted words--not implemented yet 8/1 TODO
	}

	public int getTotalHypos() {
		return m_numHyposCreated + m_numHyposNotBuilt;
	}

	public int getNumHyposRecombined() {
		return m_recombinationInfos.size();
	}

	public int getNumHyposPruned() {
		return m_numHyposPruned;
	}

	public int getNumHyposDiscarded() {
		return m_numHyposDiscarded;
	}

	public int getNumHyposEarlyDiscarded() {
		return m_numHyposEarlyDiscarded;
	}

	public int getNumHyposNotBuilt() {
		return m_numHyposNotBuilt;
	}

	public float getTimeCollectOpts() {
		return m_timeCollectOpts / (float) TypeDef.CLOCKS_PER_SEC;
	}

	public float getTimeBuildHyp() {
		return m_timeBuildHyp / (float) TypeDef.CLOCKS_PER_SEC;
	}

	public float getTimeCalcLM() {
		return m_timeCalcLM / (float) TypeDef.CLOCKS_PER_SEC;
	}

	public float getTimeEstimateScore() {
		return m_timeEstimateScore / (float) TypeDef.CLOCKS_PER_SEC;
	}

	public float getTimeOtherScore() {
		return m_timeOtherScore / (float) TypeDef.CLOCKS_PER_SEC;
	}

	public float getTimeStack() {
		return m_timeStack / (float) TypeDef.CLOCKS_PER_SEC;
	}

	public float getTimeTotal() {
		return m_timeTotal / (float) TypeDef.CLOCKS_PER_SEC;
	}

	public int getTotalSourceWords() {
		return m_totalSourceWords;
	}

	public int getNumWordsDeleted() {
		return m_deletedWords.size();
	}

	public int getNumWordsInserted() {
		return m_insertedWords.size();
	}

	public final List<Phrase> getDeletedWords() {
		return m_deletedWords;
	}

	public final List<String> getInsertedWords() {
		return m_insertedWords;
	}

	public void addRecombination(final Hypothesis worseHypo, final Hypothesis betterHypo) {
		m_recombinationInfos.add(new RecombinationInfo(worseHypo.getWordsBitmap()
				.getNumWordsCovered(), betterHypo.getTotalScore(), worseHypo.getTotalScore()));
	}

	public void addCreated() {
		m_numHyposCreated++;
	}

	public void addPruning() {
		m_numHyposPruned++;
	}

	public void addEarlyDiscarded() {
		m_numHyposEarlyDiscarded++;
	}

	public void addNotBuilt() {
		m_numHyposNotBuilt++;
	}

	public void addDiscarded() {
		m_numHyposDiscarded++;
	}

	public void addTimeCollectOpts(long t) {
		m_timeCollectOpts += t;
	}

	public void addTimeBuildHyp(long t) {
		m_timeBuildHyp += t;
	}

	public void addTimeCalcLM(long t) {
		m_timeCalcLM += t;
	}

	public void addTimeEstimateScore(long t) {
		m_timeEstimateScore += t;
	}

	public void addTimeOtherScore(long t) {
		m_timeOtherScore += t;
	}

	public void addTimeStack(long t) {
		m_timeStack += t;
	}

	public void setTimeTotal(long t) {
		m_timeTotal = t;
	}

	/***
	 * auxiliary to CalcFinalStats()
	 */
	protected void addDeletedWords(final Hypothesis hypo) {
		// don't check either a null pointer or the empty initial hypothesis (if we were given the
		// empty hypo, the null check will save us)
		if (hypo.getPrevHypo() != null
				&& hypo.getPrevHypo().getCurrSourceWordsRange().getNumWordsCovered() > 0)
			addDeletedWords(hypo.getPrevHypo());
		if (hypo.getCurrTargetWordsRange().getNumWordsCovered() == 0) {
			m_deletedWords.add(hypo.getSourcePhrase());
		}
	}

	// hypotheses
	protected List<RecombinationInfo> m_recombinationInfos = new ArrayList<RecombinationInfo>();
	protected int m_numHyposCreated;
	protected int m_numHyposPruned;
	protected int m_numHyposDiscarded;
	protected int m_numHyposEarlyDiscarded;
	protected int m_numHyposNotBuilt;
	protected long m_timeCollectOpts;
	protected long m_timeBuildHyp;
	protected long m_timeEstimateScore;
	protected long m_timeCalcLM;
	protected long m_timeOtherScore;
	protected long m_timeStack;
	protected long m_timeTotal;

	// words
	protected int m_totalSourceWords;
	protected List<Phrase> m_deletedWords = new ArrayList<Phrase>(); // count deleted words/phrases
	// in the final hypothesis
	protected List<String> m_insertedWords = new ArrayList<String>(); // count inserted words in the

	// final hypothesis

	public String toString() {
		StringBuilder os = new StringBuilder();
		float totalTime = getTimeTotal();
		float otherTime = totalTime
				- (getTimeCollectOpts() + getTimeBuildHyp() + getTimeEstimateScore()
						+ getTimeCalcLM() + getTimeOtherScore() + getTimeStack());

		os.append("total hypotheses considered = ").append(getTotalHypos()).append("\n").append(
				"           number not built = ").append(getNumHyposNotBuilt()).append("\n")
				.append("     number discarded early = ").append(getNumHyposEarlyDiscarded())
				.append("\n").append("           number discarded = ").append(
						getNumHyposDiscarded()).append("\n").append(
						"          number recombined = ").append(getNumHyposRecombined()).append(
						"\n").append("              number pruned = ").append(getNumHyposPruned())
				.append("\n"

				).append("time to collect opts    ").append(getTimeCollectOpts()).append(" (")
				.append((int) (100 * getTimeCollectOpts() / totalTime)).append("%)").append("\n")
				.append("        create hyps     ").append(getTimeBuildHyp()).append(" (").append(
						(int) (100 * getTimeBuildHyp() / totalTime)).append("%)").append("\n")
				.append("        estimate score  ").append(getTimeEstimateScore()).append(" (")
				.append((int) (100 * getTimeEstimateScore() / totalTime)).append("%)").append("\n")
				.append("        calc lm         ").append(getTimeCalcLM()).append(" (").append(
						(int) (100 * getTimeCalcLM() / totalTime)).append("%)").append("\n")
				.append("        other hyp score ").append(getTimeOtherScore()).append(" (")
				.append((int) (100 * getTimeOtherScore() / totalTime)).append("%)").append("\n")
				.append("        manage stacks   ").append(getTimeStack()).append(" (").append(
						(int) (100 * getTimeStack() / totalTime)).append("%)").append("\n").append(
						"        other           ").append(otherTime).append(" (").append(
						(int) (100 * otherTime / totalTime)).append("%)").append("\n"

				).append("total source words = ").append(getTotalSourceWords()).append("\n")
				.append("     words deleted = ").append(getNumWordsDeleted()).append(" (").append(
						Util.join(" ", getDeletedWords())).append(")").append("\n").append(
						"    words inserted = ").append(getNumWordsInserted()).append(" (").append(
						Util.join(" ", getInsertedWords())).append(")").append("\n");
		return os.toString();
	}
}
