package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import yonee.utils.ASSERT;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class Hypothesis {
	protected static ObjectPool<Hypothesis> objectPool;
	/* ! backpointer to previous hypothesis (from which this one was created) */
	/* ! score so far */
	protected float totalScore;
	protected Hypothesis prevHypo;	//Hypothesis*
	/* ! target phrase being created at the current decoding step */
	protected TargetPhrase targetPhrase;	//const TargetPhrase&

	/* ! input sentence */
	protected Phrase sourcePhrase;		//const Phrase *
	/* ! keeps track of which words have been translated so far */
	protected WordsBitmap sourceCompleted; //WordsBitmap
	// TODO: how to integrate this into confusion network framework; what if
	// it's a confusion network in the end???
	protected InputType sourceInput; //const & m_sourceInput
	/*
	 * ! source word positions of the last phrase that was used to create this
	 * hypothesis
	 */
	protected WordsRange currSourceWordsRange;	//WordsRange
	/*
	 * ! target word positions of the last phrase that was used to create this
	 * hypothesis
	 */
	protected WordsRange currTargetWordsRange;//WordsRange
	protected boolean wordDeleted;
	/*
	 * ! estimated future cost to translate rest of sentence
	 */
	protected float futureScore;
	/*
	 * ! detailed score break-down by components (for instance language model,
	 * word penalty, etc)
	 */
	protected ScoreComponentCollection scoreBreakdown = new ScoreComponentCollection();
	protected List<FFState> ffStates;
	protected Hypothesis winningHypo;
	/*
	 * ! all arcs that end at the same trellis point as this hypothesis
	 */
	protected List<Hypothesis> arcList;
	protected TranslationOption transOpt;
	protected Manager manager;

	protected int id; /* ! numeric ID of this hypothesis, used for logging */

	/* ! used by initial seeding of the translation process */
	protected Hypothesis(Manager manager, final InputType source, final TargetPhrase emptyTarget) {
		prevHypo = null;
		targetPhrase = emptyTarget;
		sourceCompleted = new WordsBitmap(source.getSize(), manager.m_source.i().m_sourceCompleted);
		sourceInput = source;
		currSourceWordsRange = new WordsRange(sourceCompleted.getFirstGapPos() > 0 ? 0
				: TypeDef.NOT_FOUND, sourceCompleted.getFirstGapPos() > 0 ? sourceCompleted
				.getFirstGapPos() - 1 : TypeDef.NOT_FOUND);
		currTargetWordsRange = new WordsRange(0, emptyTarget.getSize() - 1);
		wordDeleted = false;
		ffStates = new ArrayList<FFState>(StaticData.instance().getScoreIndexManager()
				.getStatefulFeatureFunctions().size());
		arcList = null;
		transOpt = null;

		this.manager = manager;
		id = manager.getNextHypoId();
		resetScore();
		List<StatefulFeatureFunction> ffs = StaticData.instance().getScoreIndexManager()
				.getStatefulFeatureFunctions();
		for (int i = 0; i < ffs.size(); ++i)
			ffStates.add(i, ffs.get(i).emptyHypothesisState(source));
		manager.getSentenceStats().addCreated();
	}

	/*
	 * ! used when creating a new hypothesis using a translation option (phrase
	 * translation)
	 */
	protected Hypothesis(final Hypothesis prevHypo, final TranslationOption transOpt) {
		this.prevHypo = prevHypo;
		targetPhrase = transOpt.getTargetPhrase();
		sourcePhrase = transOpt.getSourcePhrase();
		sourceCompleted = new WordsBitmap(prevHypo.sourceCompleted);
		sourceInput = prevHypo.sourceInput;
		currSourceWordsRange = new WordsRange(transOpt.getSourceWordsRange());
		currTargetWordsRange = new WordsRange(prevHypo.currTargetWordsRange.getEndPos() + 1,
				prevHypo.currTargetWordsRange.getEndPos() + transOpt.getTargetPhrase().getSize());

		scoreBreakdown = new ScoreComponentCollection(prevHypo.scoreBreakdown);
		ffStates = new ArrayList<FFState>(prevHypo.ffStates.size());
		this.transOpt = transOpt;
		manager = prevHypo.getManager();
		id = manager.getNextHypoId();
		// assert that we are not extending our hypothesis by retranslating
		// something
		// that this hypothesis has already translated!
		ASSERT.a(!sourceCompleted.overlap(currSourceWordsRange));

		// _hash_computed = false;
		sourceCompleted.setValue(currSourceWordsRange.getStartPos(), currSourceWordsRange
				.getEndPos(), true);
		wordDeleted = transOpt.isDeletionOption();
		manager.getSentenceStats().addCreated();
	}

	public static ObjectPool<Hypothesis> getObjectPool() {
		return objectPool;
	}

	/**
	 * return the subclass of Hypothesis most appropriate to the given
	 * translation option
	 */
	public static Hypothesis create(final Hypothesis prevHypo, final TranslationOption transOpt,
			final Phrase constrainingPhrase) {

		// This method includes code for constraint decoding

		boolean createHypothesis = true;

		if (constrainingPhrase != null) {

			int constraintSize = constrainingPhrase.getSize();
			int start = 1 + prevHypo.getCurrTargetWordsRange().getEndPos();
			final Phrase transOptPhrase = transOpt.getTargetPhrase();
			int transOptSize = transOptPhrase.getSize();
			int endpoint = start + transOptSize - 1;
			if (endpoint < constraintSize) {
				WordsRange range = new WordsRange(start, endpoint);
				Phrase relevantConstraint = constrainingPhrase.getSubString(range);
				if (!relevantConstraint.isCompatible(transOptPhrase)) {
					createHypothesis = false;
				}
			} else {
				createHypothesis = false;
			}

		}

		if (createHypothesis) {
			/*
			 * YONEE #ifdef USE_HYPO_POOL Hypothesis *ptr =
			 * s_objectPool.getPtr(); return new(ptr) Hypothesis(prevHypo,
			 * transOpt); #else
			 */
			return new Hypothesis(prevHypo, transOpt);

		} else {
			// If the previous hypothesis plus the proposed translation option
			// fail to match the provided constraint,
			// return a null hypothesis.
			return null;
		}
	}

	/*
	 * NO IMPLEMENTS public static Hypothesis create(Manager manager, final
	 * WordsBitmap initialCoverage) {
	 * 
	 * }
	 */
	/**
	 * return the subclass of Hypothesis most appropriate to the given target
	 * phrase
	 */
	public static Hypothesis create(Manager manager, final InputType source,
			final TargetPhrase emptyTarget) {
		/*
		 * YONEE #ifdef USE_HYPO_POOL Hypothesis *ptr = s_objectPool.getPtr();
		 * return new(ptr) Hypothesis(manager, m_source, emptyTarget); #else
		 */
		return new Hypothesis(manager, source, emptyTarget);

	}

	/**
	 * return the subclass of Hypothesis most appropriate to the given
	 * translation option
	 */
	public final Hypothesis createNext(final TranslationOption transOpt, final Phrase constraint) {
		return Hypothesis.create(this, transOpt, constraint);
	}

	public void printHypothesis() {
		if (prevHypo == null) {
			TRACE.err("\n" + "NULL hypo" + "\n");
			return;
		}
		TRACE.err("\n" + "creating hypothesis " + id + " from " + prevHypo.id + " ( ");
		int end = (int) (prevHypo.targetPhrase.getSize() - 1);
		int start = end - 1;
		if (start < 0)
			start = 0;
		if (prevHypo.currTargetWordsRange.getStartPos() == TypeDef.NOT_FOUND) {
			TRACE.err("<s> ");
		} else {
			TRACE.err("... ");
		}
		if (end >= 0) {
			WordsRange range = new WordsRange(start, end);
			TRACE.err(prevHypo.targetPhrase.getSubString(range) + " ");
		}
		TRACE.err(")" + "\n");
		TRACE.err("\tbase score " + (prevHypo.totalScore - prevHypo.futureScore) + "\n");
		TRACE.err("\tcovering " + currSourceWordsRange.getStartPos() + "-"
				+ currSourceWordsRange.getEndPos() + ": " + sourcePhrase + "\n");
		TRACE.err("\ttranslated as: " + (Phrase) targetPhrase + "\n"); // +" => translation cost "+score[ScoreType::PhraseTrans];

		if (wordDeleted)
			TRACE.err("\tword deleted" + "\n");
		// TRACE.err(
		// "\tdistance: "+GetCurrSourceWordsRange().CalcDistortion(prevHypo->GetCurrSourceWordsRange()));
		// // + " => distortion cost "+(score[ScoreType::Distortion]*weightDistortion)+"\n";
		// TRACE.err( "\tlanguage model cost "); // +score[ScoreType::LanguageModelScore]+"\n";
		// TRACE.err( "\tword penalty "); //
		// +(score[ScoreType::WordPenalty]*weightWordPenalty)+"\n";
		TRACE.err("\tscore " + (totalScore - futureScore) + " + future cost " + futureScore + " = "
				+ totalScore + "\n");
		TRACE.err("\tunweighted feature scores: " + scoreBreakdown + "\n");
		// PrintLMScores();
	}

	public final InputType getInput() {
		return sourceInput;
	}

	/** return target phrase used to create this hypothesis */
	// const Phrase &GetCurrTargetPhrase() const
	public final TargetPhrase getCurrTargetPhrase() {
		return targetPhrase;
	}

	// void PrintLMScores(const LMList &lmListInitial, const LMList &lmListEnd)
	// const;

	/**
	 * return input positions covered by the translation option (phrasal
	 * translation) used to create this hypothesis
	 */
	public final WordsRange getCurrSourceWordsRange() {
		return currSourceWordsRange;
	}

	public final WordsRange getCurrTargetWordsRange() {
		return currTargetWordsRange;
	}

	public final Manager getManager() {
		return manager;
	}

	/** output length of the translation option used to create this hypothesis */
	public final int getCurrTargetLength() {
		return currTargetWordsRange.getNumWordsCovered();
	}

	public void resetScore() {
		scoreBreakdown.zeroAll();
		futureScore = totalScore = 0.0f;
	}

	public void calcScore(final SquareMatrix futureScore) {

		// some stateless score producers cache their values in the translation
		// option: add these here
		// language model scores for n-grams completely contained within a
		// target
		// phrase are also included here
		scoreBreakdown.plusEquals(transOpt.getScoreBreakdown());

		StaticData staticData = StaticData.instance();
		long t = 0; // used to track time

		// compute values of stateless feature functions that were not
		// cached in the translation option-- there is no principled distinction
		List<StatelessFeatureFunction> sfs = staticData.getScoreIndexManager()
				.getStatelessFeatureFunctions();
		for (int i = 0; i < sfs.size(); ++i) {
			sfs.get(i).evaluate(targetPhrase, scoreBreakdown);
		}

		List<StatefulFeatureFunction> ffs = staticData.getScoreIndexManager()
				.getStatefulFeatureFunctions();
		for (int i = 0; i < ffs.size(); ++i) {
			ffStates.add(i, ffs.get(i).evaluate(this,
					prevHypo != null ? prevHypo.ffStates.get(i) : null, scoreBreakdown));
		}

		if (VERBOSE.v(2)) {
			t = System.currentTimeMillis();
		} // track time excluding LM

		// FUTURE COST
		this.futureScore = futureScore.calcFutureScore(sourceCompleted);

		// TOTAL
		totalScore = scoreBreakdown.innerProduct(staticData.getAllWeights()) + this.futureScore;

		if (VERBOSE.v(2)) {
			manager.getSentenceStats().addTimeOtherScore(System.currentTimeMillis() - t);
		}
	}

	public float calcExpectedScore(final SquareMatrix futureScore) {
		final StaticData staticData = StaticData.instance();
		long t = 0;
		if (VERBOSE.v(2)) {
			t = System.currentTimeMillis();
		} // track time excluding LM

		ASSERT.a("Need to add code to get the distortion scores" != null);
		// CalcDistortionScore();

		// LANGUAGE MODEL ESTIMATE (includes word penalty cost)
		float estimatedLMScore = this.transOpt.getFutureScore()
				- this.transOpt.getScoreBreakdown().innerProduct(staticData.getAllWeights());

		// FUTURE COST
		this.futureScore = futureScore.calcFutureScore(this.sourceCompleted);

		// TOTAL
		float total = this.scoreBreakdown.innerProduct(staticData.getAllWeights())
				+ this.futureScore + estimatedLMScore;

		if (VERBOSE.v(2)) {
			manager.getSentenceStats().addTimeEstimateScore(System.currentTimeMillis() - t);
		}
		return total;
	}

	public void calcRemainingScore() {
		final StaticData staticData = StaticData.instance();
		long t = 0; // used to track time

		// LANGUAGE MODEL COST
		ASSERT.a("Need to add code to get the LM score(s)" == null);
		// CalcLMScore(staticData.GetAllLM());

		if (VERBOSE.v(2)) {
			t = System.currentTimeMillis();
		} // track time excluding LM

		// WORD PENALTY
		this.scoreBreakdown.plusEquals(staticData.getWordPenaltyProducer(),
				-(float) this.currTargetWordsRange.getNumWordsCovered());

		// TOTAL
		totalScore = scoreBreakdown.innerProduct(staticData.getAllWeights()) + futureScore;

		if (VERBOSE.v(2)) {
			manager.getSentenceStats().addTimeOtherScore(System.currentTimeMillis() - t);
		}
	}

	public final int getId() {
		return id;
	}

	public final Hypothesis getPrevHypo() {
		return this.prevHypo;
	}

	// typedef size_t FactorType;
	public String getSourcePhraseStringRep(final int[] factorsToPrint) {
		if (prevHypo == null) {
			return "";
		}
		return sourcePhrase.getStringRep(factorsToPrint);
		/*
		 * YONEE #if 0 if(sourcePhrase) { return
		 * m_sourcePhrase->GetSubString(m_currSourceWordsRange
		 * ).GetStringRep(factorsToPrint); } else { return
		 * m_sourceInput.GetSubString
		 * (m_currSourceWordsRange).GetStringRep(factorsToPrint); } #endif
		 */
	}

	public String getTargetPhraseStringRep(final int[] factorsToPrint) {
		if (prevHypo == null) {
			return "";
		}
		return targetPhrase.getStringRep(factorsToPrint);

	}

	public final TargetPhrase getTargetPhrase() {
		return targetPhrase;
	}

	public String getSourcePhraseStringRep() {

		int maxSourceFactors = StaticData.instance()
				.getMaxNumFactors(TypeDef.FactorDirection.Input);
		int[] allFactors = new int[maxSourceFactors];
		for (int i = 0; i < maxSourceFactors; i++) {
			allFactors[i] = i;
		}
		return getSourcePhraseStringRep(allFactors);
	}

	public String getTargetPhraseStringRep() {

		int maxTargetFactors = StaticData.instance().getMaxNumFactors(
				TypeDef.FactorDirection.Output);
		int[] allFactors = new int[maxTargetFactors];
		for (int i = 0; i < maxTargetFactors; i++) {
			allFactors[i] = i;
		}
		return getTargetPhraseStringRep(allFactors);
	}

	public final Word getCurrWord(int pos) {
		return targetPhrase.getWord(pos);
	}

	public final Factor getCurrFactor(int pos, int factorType) {
		return targetPhrase.getFactor(pos, factorType);
	}

	/** recursive - pos is relative from start of sentence */
	public final Word getWord(int pos) {
		Hypothesis hypo = this;
		while (pos < hypo.getCurrTargetWordsRange().getStartPos()) {
			hypo = hypo.getPrevHypo();
			ASSERT.a(hypo != null);
		}
		return hypo.getCurrWord(pos - hypo.getCurrTargetWordsRange().getStartPos());
	}

	public final Factor getFactor(int pos, int factorType) {
		return getWord(pos).get(factorType);
	}

	/***
	 * \return The bitmap of source words we cover
	 */
	public final WordsBitmap getWordsBitmap() {
		return sourceCompleted;
	}

	public final boolean isSourceCompleted() {
		return sourceCompleted.isComplete();
	}

	// YONEE
	public int recombineCompare(Hypothesis compare) {
		// -1 = this < compare
		// +1 = this > compare
		// 0 = this ==compare
		int comp = sourceCompleted.compare(compare.sourceCompleted);
		if (comp != 0)
			return comp;

		for (int i = 0; i < ffStates.size(); ++i) {
			if (ffStates.get(i) == null || compare.ffStates.get(i) == null) {
				// comp = ffStates.get(i) - compare.ffStates.get(i); YONEE
			} else {
				// comp = ffStates.get(i).compare(compare.ffStates.get(i));
			}
			if (comp != 0)
				return comp;
		}

		return 0;
	}

	public boolean printAlignmentInfo() {
		return getCurrTargetPhrase().printAlignmentInfo();
	}

	public final void setWinningHypo(final Hypothesis hypo) {
		winningHypo = hypo;
	}

	public final Hypothesis getWinningHypo() {
		return winningHypo;
	}

	public void addArc(Hypothesis loserHypo) {
		if (arcList == null) {
			if (loserHypo.arcList != null) {
				this.arcList = loserHypo.arcList;
			} else {
				this.arcList = new ArrayList<Hypothesis>();
			}
		} else {
			if (loserHypo.arcList != null) {
				arcList.addAll(loserHypo.arcList);
			} else {
				// DO NOTHING
			}
		}
		arcList.add(loserHypo);
	}

	// YONEE
	public void cleanupArcList() {
		// point this hypo's main hypo to itself
		setWinningHypo(this);

		if (arcList == null)
			return;

		/*
		 * keep only number of arcs we need to create all n-best paths. However,
		 * may not be enough if only unique candidates are needed, so we'll keep
		 * all of arc list if nedd distinct n-best list
		 */
		final StaticData staticData = StaticData.instance();
		int nBestSize = staticData.getNBestSize();
		boolean distinctNBest = staticData.getDistinctNBest() || staticData.useMBR()
				|| staticData.getOutputSearchGraph() || staticData.useLatticeMBR();

		// prune arc list only if there too many arcs
		if (!distinctNBest && arcList.size() > nBestSize * 5) {

			Collections.sort(arcList, new Hypothesis.CompareHypothesisTotalScore());

			int i = 0;
			for (Iterator<Hypothesis> iter = arcList.iterator(); iter.hasNext(); i++) {
				if (i >= nBestSize) {
					arcList.remove(iter);
				} else {
					iter.next();
				}
			}
			// arcList->remove(m_arcList->begin() + nBestSize
			// , m_arcList->end());
		}

		for (Hypothesis arc : arcList) {
			arc.setWinningHypo(this);
		}
	}

	// ! returns a list alternative previous hypotheses (or NULL if n-best
	// support is disabled)
	public final List<Hypothesis> getArcList() {
		return arcList;
	}

	public ScoreComponentCollection getScoreBreakdown() {
		return scoreBreakdown;
	}

	final public float getTotalScore() {
		return totalScore;
	}

	final public float getScore() {
		return totalScore - futureScore;
	}

	// ! target span that trans opt would populate if applied to this hypo. Used
	// for alignment check
	// NO IMPLEMENTS
	// public int getNextStartPos(final TranslationOption transOpt) ;

	public List<List<Integer>> getLMStats() {
		return null;
	}

	public TranslationOption getTranslationOption() {
		return transOpt;
	}

	/** length of the partial translation (from the start of the sentence) */
	public final int getSize() {
		return currTargetWordsRange.getEndPos() + 1;
	}

	public final Phrase getSourcePhrase() {
		return sourcePhrase;
	}

	public void toStream(StringBuilder out) {
		if (prevHypo != null) {
			prevHypo.toStream(out);
		}
		out.append((Phrase) getCurrTargetPhrase());
	}

	// TO_STRING();
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toStream(sb);
		// words bitmap
		sb.append("[").append(sourceCompleted).append("] ");

		// scores
		sb.append(" [total=").append(getTotalScore()).append("]");
		sb.append(" ").append(getScoreBreakdown());

		// alignment
		sb.append(" ").append(getCurrTargetPhrase().getAlignmentInfo());

		/*
		 * const Hypothesis *prevHypo = hypo.GetPrevHypo();
		 * if (prevHypo)
		 * sb.append( "\n" ).append( *prevHypo;
		 */

		return sb.toString();
	}

	// //////////////////////////////////////////////////////////////////////////////
	// Allows to compare two Hypothesis objects by the corresponding scores.
	// //////////////////////////////////////////////////////////////////////////////
	static public class HypothesisScoreOrderer implements Comparator<Hypothesis> {
		public int compare(final Hypothesis hypoA, final Hypothesis hypoB) {
			return (int) (hypoA.getTotalScore() - hypoB.getTotalScore());
		}
	}

	static public class CompareHypothesisTotalScore implements Comparator<Hypothesis> {
		public int compare(final Hypothesis hypoA, final Hypothesis hypoB) {
			return (int) (hypoA.getTotalScore() - hypoB.getTotalScore());
		}
	}

}
