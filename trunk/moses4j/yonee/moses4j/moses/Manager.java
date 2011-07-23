package yonee.moses4j.moses;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import yonee.moses4j.moses.TypeDef.SearchAlgorithm;
import yonee.utils.ASSERT;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class Manager {

	// private Manager() {
	//		
	// }
	//
	// private Manager(Manager mgr) {
	//
	// }
	//
	// private void assign(Manager mgr) {
	//
	// }

	public static void outputWordGraph(Writer out, final Hypothesis hypo, int linkId) throws IOException {
		final StaticData staticData = StaticData.instance();

		final Hypothesis prevHypo = hypo.getPrevHypo();

		out.append("J=" + linkId++ + "\tS=" + prevHypo.getId() + "\tE=" + hypo.getId() + "\ta=");

		// phrase table scores
		final List<PhraseDictionaryFeature> phraseTables = staticData.getPhraseDictionaries();

		// List<PhraseDictionaryFeature>::const_iterator iterPhraseTable;
		for (PhraseDictionaryFeature phraseTable : phraseTables) {
			float[] scores = hypo.getScoreBreakdown().getScoresForProducer(phraseTable);

			out.append(scores[0] + "");

			for (float f : scores) {
				out.append(", " + f);
			}
		}

		// language model scores
		out.append("\tl=");
		final LMList lmList = staticData.getAllLM();

		for (LanguageModel lm : lmList) {
			float[] scores = hypo.getScoreBreakdown().getScoresForProducer(lm);

			out.append(scores[0] +"");

			for (float f : scores) {
				out.append(", " + f);
			}
		}

		// re-ordering
		out.append("\tl=");

		out.append(hypo.getScoreBreakdown().getScoreForProducer(
				staticData.getDistortionScoreProducer())
				+ "");

		// lexicalised re-ordering
		List<LexicalReordering> lexOrderings = staticData.getReorderModels();
		// std::vector<LexicalReordering*>::const_iterator iterLexOrdering;
		for (LexicalReordering lexicalReordering : lexOrderings) {
			float[] scores = hypo.getScoreBreakdown().getScoresForProducer(lexicalReordering);

			out.append(scores[0] + "");

			for (float f : scores) {
				out.append(", " + f);
			}
		}

		// words !!
		out.append("\tw=" + hypo.getCurrTargetPhrase() + "\n");

	}

	// data
	// InputType const& m_source; /**< source sentence to be translated */
	protected TranslationOptionCollection m_transOptColl;
	/** < pre-computed list of translation options for the phrases in this sentence */
	protected Search m_search;

	protected HypothesisStack actual_hypoStack;
	/** actual (full expanded) stack of hypotheses */
	protected long m_start;
	/** < starting time, used for logging */
	protected int interrupted_flag;
	protected SentenceStats m_sentenceStats;
	protected int m_hypoId; // used to number the hypos as they are created.

	protected void getConnectedGraph(Map<Integer, Boolean> pConnected,
			List<Hypothesis> pConnectedList) {

	}

	protected void getWinnerConnectedGraph(Map<Integer, Boolean> pConnected,
			List<Hypothesis> pConnectedList) {

	}

	public InputType m_source;

	/** < source sentence to be translated */
	public Manager(final InputType source, SearchAlgorithm searchAlgorithm) {
		m_source = source;
		m_transOptColl = source.createTranslationOptionCollection();
		m_search = Search.createSearch(this, source, searchAlgorithm, m_transOptColl);
		m_start = System.currentTimeMillis();
		interrupted_flag = (0);
		m_hypoId = 0;

		final StaticData staticData = StaticData.instance();
		staticData.initializeBeforeSentenceProcessing(source);
	}

	public void cleanUp() {
		m_transOptColl = null;
		m_search = null;

		StaticData.instance().cleanUpAfterSentenceProcessing();

		long end = System.currentTimeMillis();
		float et = (end - m_start);
		et /= (float) TypeDef.CLOCKS_PER_SEC;
		VERBOSE.v(1, "Translation took " + et + " seconds" + "\n");
		VERBOSE.v(1, "Finished translating" + "\n");
	}

	public TranslationOptionCollection getSntTranslationOptions() {
		return m_transOptColl;
	}

	public void processSentence() {
		// reset statistics
		final StaticData staticData = StaticData.instance();
		resetSentenceStats(m_source);

		// collect translation options for this sentence
		List<DecodeGraph> decodeGraphs = staticData.getDecodeStepVL(m_source);
		m_transOptColl.createTranslationOptions(decodeGraphs);
		decodeGraphs.clear();

		// some reporting on how long this took
		long gotOptions = System.currentTimeMillis();
		float et = (gotOptions - m_start);
		if (VERBOSE.v(2)) {
			getSentenceStats().addTimeCollectOpts(gotOptions - m_start);
		}
		et /= (float) TypeDef.CLOCKS_PER_SEC;
		VERBOSE.v(1, "Collecting options took " + et + " seconds" + "\n");

		// search for best translation with the specified algorithm
		m_search.processSentence();
		VERBOSE.v(1, "Search took "
				+ ((System.currentTimeMillis() - m_start) / (float) TypeDef.CLOCKS_PER_SEC)
				+ " seconds\n");
	}

	public Hypothesis getBestHypothesis() {
		return m_search.getBestHypothesis();
	}

	// public Hypothesis getActualBestHypothesis() {
	//
	// }

	public void calcNBest(int count, TrellisPathList ret) {
		calcNBest(count, ret, false);
	}

	public void calcNBest(int count, TrellisPathList ret, boolean onlyDistinct) {
		if (count <= 0)
			return;

		final HypothesisStack[] hypoStackColl = m_search.getHypothesisStacks();

		List<Hypothesis> sortedPureHypo = hypoStackColl[hypoStackColl.length - 1].getSortedList();

		if (sortedPureHypo.size() == 0)
			return;

		TrellisPathCollection contenders = new TrellisPathCollection();

		Set<Phrase> distinctHyps = new HashSet<Phrase>();

		// add all pure paths
		for (Hypothesis iterBestHypo : sortedPureHypo) {
			contenders.add(new TrellisPath(iterBestHypo));
		}

		// factor defines stopping point for distinct n-best list if too many candidates identical
		int nBestFactor = StaticData.instance().getNBestFactor();
		if (nBestFactor < 1)
			nBestFactor = 1000; // 0 = unlimited

		// MAIN loop
		for (int iteration = 0; (onlyDistinct ? distinctHyps.size() : ret.size()) < count
				&& contenders.size() > 0 && (iteration < count * nBestFactor); iteration++) {
			// get next best from list of contenders
			TrellisPath path = contenders.pop();
			ASSERT.a(path != null);
			// create deviations from current best
			path.createDeviantPaths(contenders);
			if (onlyDistinct) {
				Phrase tgtPhrase = path.getSurfacePhrase();
				if (distinctHyps.add(tgtPhrase)) {
					ret.add(path);
				} else {

					path = null;
				}
			} else {
				ret.add(path);
			}

			if (onlyDistinct) {
				final int nBestFactor0 = StaticData.instance().getNBestFactor();
				if (nBestFactor0 > 0)
					contenders.prune(nBestFactor);
			} else {
				contenders.prune(count);
			}
		}
	}

	public void printAllDerivations(long translationId, Writer outputStream) {

	}

	public void printDivergentHypothesis(long translationId, final Hypothesis hypo,
			final List<TargetPhrase> remainingPhrases, float remainingScore,
			PrintStream outputStream) {
	}

	public void printThisHypothesis(long translationId, final Hypothesis hypo,
			final List<TargetPhrase> remainingPhrases, float remainingScore,
			PrintStream outputStream) {
	}

	public void getWordGraph(long translationId, Writer out) throws IOException {
		final StaticData staticData = StaticData.instance();
		//String fileName = staticData.getParam("output-word-graph").get(0);
		boolean outputNBest = Boolean.valueOf(staticData.getParam("output-word-graph").get(1));
		final HypothesisStack[] hypoStackColl = m_search.getHypothesisStacks();
		out.append("VERSION=1.0" + "\n" + "UTTERANCE=" + translationId + "\n");
		int linkId = 0;
		int stackNo = 1;
		for (HypothesisStack stack : hypoStackColl) {
			System.err.print("\n" + stackNo++ + "\n");
			for (Hypothesis hypo : stack) {
				outputWordGraph(out, hypo, linkId);

				if (outputNBest) {
					final List<Hypothesis> arcList = hypo.getArcList();
					if (arcList != null) {
						for (Hypothesis loserHypo : arcList) {
							outputWordGraph(out, loserHypo, linkId);
						}
					}
				} // if (outputNBest)
			} // for (iterHypo
		} // for (iterStack
	}

	public int getNextHypoId() {
		return m_hypoId++;
	}

	// #ifdef HAVE_PROTOBUF
	// void SerializeSearchGraphPB(long translationId, std::ostream& outputStream) {}
	// #endif
	public static void outputSearchNode(long translationId, Writer outputSearchGraphStream,
			final SearchGraphNode searchNode) throws IOException {
		final int[] outputFactorOrder = StaticData.instance().getOutputFactorOrder();
		boolean extendedFormat = StaticData.instance().getOutputSearchGraphExtended();
		outputSearchGraphStream.append(translationId +"");

		// special case: initial hypothesis
		if (searchNode.hypo.getId() == 0) {
			outputSearchGraphStream.append(" hyp=0 stack=0");
			if (!extendedFormat) {
				outputSearchGraphStream.append(" forward=" + searchNode.forward + " fscore="
						+ searchNode.fscore);
			}
			outputSearchGraphStream.append("\n");
			return;
		}

		final Hypothesis prevHypo = searchNode.hypo.getPrevHypo();

		// output in traditional format
		if (!extendedFormat) {
			outputSearchGraphStream.append(" hyp=" + searchNode.hypo.getId() + " stack="
					+ searchNode.hypo.getWordsBitmap().getNumWordsCovered() + " back="
					+ prevHypo.getId() + " score=" + searchNode.hypo.getScore() + " transition="
					+ (searchNode.hypo.getScore() - prevHypo.getScore()));

			if (searchNode.recombinationHypo != null)
				outputSearchGraphStream.append(" recombined="
						+ searchNode.recombinationHypo.getId());

			outputSearchGraphStream.append(" forward=" + searchNode.forward + " fscore="
					+ searchNode.fscore + " covered="
					+ searchNode.hypo.getCurrSourceWordsRange().getStartPos() + "-"
					+ searchNode.hypo.getCurrSourceWordsRange().getEndPos() + " out="
					+ searchNode.hypo.getCurrTargetPhrase().getStringRep(outputFactorOrder) + "\n");
			return;
		}

		// output in extended format
		if (searchNode.recombinationHypo != null)
			outputSearchGraphStream.append(" hyp=" + searchNode.recombinationHypo.getId());
		else
			outputSearchGraphStream.append(" hyp=" + searchNode.hypo.getId());

		outputSearchGraphStream.append(" back=" + prevHypo.getId());

		ScoreComponentCollection scoreBreakdown = searchNode.hypo.getScoreBreakdown();
		scoreBreakdown.minusEquals(prevHypo.getScoreBreakdown());
		outputSearchGraphStream.append(" [ ");
		StaticData.instance().getScoreIndexManager().printLabeledScores(outputSearchGraphStream,
				scoreBreakdown);
		outputSearchGraphStream.append(" ]");

		outputSearchGraphStream.append(" out="
				+ searchNode.hypo.getCurrTargetPhrase().getStringRep(outputFactorOrder) + "\n");
	}

	public void outputSearchGraph(long translationId, Writer outputSearchGraphStream) throws IOException {
		List<SearchGraphNode> searchGraph = new ArrayList<SearchGraphNode>();
		getSearchGraph(searchGraph);
		for (int i = 0; i < searchGraph.size(); ++i) {
			outputSearchNode(translationId, outputSearchGraphStream, searchGraph.get(i));
		}
	}

	public void getSearchGraph(List<SearchGraphNode> searchGraph) {
		Map<Integer, Boolean> connected = new HashMap<Integer, Boolean>();
		Map<Integer, Integer> forward = new HashMap<Integer, Integer>();
		Map<Integer, Double> forwardScore = new HashMap<Integer, Double>();

		// *** find connected hypotheses ***
		List<Hypothesis> connectedList = new ArrayList<Hypothesis>();
		getConnectedGraph(connected, connectedList);

		// ** compute best forward path for each hypothesis *** //

		// forward cost of hypotheses on final stack is 0
		final HypothesisStack[] hypoStackColl = m_search.getHypothesisStacks();
		final HypothesisStack finalStack = hypoStackColl[hypoStackColl.length - 1];
		for (Hypothesis hypo : finalStack) {
			forwardScore.put(hypo.getId(), 0.0);
			forward.put(hypo.getId(), -1);
		}

		// compete for best forward score of previous hypothesis
		for (HypothesisStack stack : hypoStackColl) {
			for (Hypothesis hypo : stack) {
				if (connected.get(hypo.getId()) != null) {
					// make a play for previous hypothesis
					final Hypothesis prevHypo = hypo.getPrevHypo();
					double fscore = forwardScore.get(hypo.getId()) + hypo.getScore()
							- prevHypo.getScore();
					if (forwardScore.get(prevHypo.getId()) == null
							|| forwardScore.get(prevHypo.getId()) < fscore) {
						forwardScore.put(prevHypo.getId(), fscore);
						forward.put(prevHypo.getId(), hypo.getId());
					}
					// all arcs also make a play
					final List<Hypothesis> arcList = hypo.getArcList();
					if (arcList != null) {
						for (Hypothesis loserHypo : arcList) {
							// make a play
							final Hypothesis loserPrevHypo = loserHypo.getPrevHypo();
							double fscore0 = forwardScore.get(hypo.getId()) + loserHypo.getScore()
									- loserPrevHypo.getScore();
							if (forwardScore.get(loserPrevHypo.getId()) == null
									|| forwardScore.get(loserPrevHypo.getId()) < fscore0) {
								forwardScore.put(prevHypo.getId(), fscore0);
								forward.put(prevHypo.getId(), hypo.getId());
							}
						} // end for arc list
					} // end if arc list empty
				} // end if hypo connected
			} // end for hypo
		} // end for stack

		// *** output all connected hypotheses *** //

		connected.put(0, true);
		for (HypothesisStack stack : hypoStackColl) {
			for (Hypothesis hypo : stack) {
				if (connected.get(hypo.getId()) != null) {
					searchGraph.add(new SearchGraphNode(hypo, null, forward.get(hypo.getId()),
							forwardScore.get(hypo.getId())));

					final List<Hypothesis> arcList = hypo.getArcList();
					if (arcList != null) {
						for (Hypothesis loserHypo : arcList) {
							searchGraph.add(new SearchGraphNode(loserHypo, hypo, forward.get(hypo
									.getId()), forwardScore.get(hypo.getId())));
						}
					} // end if arcList empty
				} // end if connected
			} // end for iterHypo
		} // end for iterStack
	}

	public InputType getSource() {
		return m_source;
	}

	/***
	 * to be called after processing a sentence (which may consist of more than just calling
	 * ProcessSentence() )
	 */
	public void calcDecoderStatistics() {
		Hypothesis hypo = getBestHypothesis();
		if (hypo != null) {
			getSentenceStats().calcFinalStats(hypo);
			if (VERBOSE.v(2)) {
				if (hypo != null) {
					StringBuilder buff = new StringBuilder();
					StringBuilder buff2 = new StringBuilder();
					TRACE.err("Source and Target Units:" + hypo.getInput());
					buff2.append("] ");
					buff2.append((hypo.getCurrTargetPhrase()).toString());
					buff2.append(":");
					buff2.append((hypo.getCurrSourceWordsRange()).toString());
					buff2.append("[");

					hypo = hypo.getPrevHypo();
					while (hypo != null) {
						// dont print out the empty final hypo
						buff.append(buff2);
						buff2.append(" ");
						buff2.append("] ");
						buff2.append((hypo.getCurrTargetPhrase()).toString());
						buff2.append(":");
						buff2.append((hypo.getCurrSourceWordsRange()).toString());
						buff2.append("[");
						hypo = hypo.getPrevHypo();
					}
					TRACE.err(buff + "\n");
				}
			}
		}
	}

	public void resetSentenceStats(final InputType source) {
		m_sentenceStats = new SentenceStats(source);
	}

	public SentenceStats getSentenceStats() {
		return m_sentenceStats;
	}

	/***
	 *For Lattice MBR
	 */
	public void getForwardBackwardSearchGraph(Map<Integer, Boolean> pConnected,
			List<Hypothesis> pConnectedList, Map<Hypothesis, Set<Hypothesis>> pOutgoingHyps,
			List<Float> pFwdBwdScores) {
		Map<Integer, Boolean> connected = pConnected;
		List<Hypothesis> connectedList = pConnectedList;
		Map<Integer, Integer> forward = new HashMap<Integer, Integer>();
		Map<Integer, Double> forwardScore = new HashMap<Integer, Double>();

		Map<Hypothesis, Set<Hypothesis>> outgoingHyps = pOutgoingHyps;
		List<Float> estimatedScores = pFwdBwdScores;

		// *** find connected hypotheses ***
		getWinnerConnectedGraph(connected, connectedList);

		// ** compute best forward path for each hypothesis *** //

		// forward cost of hypotheses on final stack is 0
		final HypothesisStack[] hypoStackColl = m_search.getHypothesisStacks();
		final HypothesisStack finalStack = hypoStackColl[hypoStackColl.length - 1];
		for (Hypothesis hypo : finalStack) {
			forwardScore.put(hypo.getId(), 0.0);
			forward.put(hypo.getId(), -1);

		}

		// compete for best forward score of previous hypothesis
		for (HypothesisStack stack : hypoStackColl) {
			for (Hypothesis hypo : stack) {
				if (connected.get(hypo.getId()) != null) {
					// make a play for previous hypothesis
					final Hypothesis prevHypo = hypo.getPrevHypo();
					double fscore = forwardScore.get(hypo.getId()) + hypo.getScore()
							- prevHypo.getScore();
					if (forwardScore.get(prevHypo.getId()) == null
							|| forwardScore.get(prevHypo.getId()) < fscore) {
						forwardScore.put(prevHypo.getId(), fscore);
						forward.put(prevHypo.getId(), hypo.getId());
					}
					// store outgoing info
					outgoingHyps.get(prevHypo).add(hypo);

					final List<Hypothesis> arcList = hypo.getArcList();
					if (arcList != null) {
						for (Hypothesis loserHypo : arcList) {
							// make a play
							final Hypothesis loserPrevHypo = loserHypo.getPrevHypo();
							double fscore0 = forwardScore.get(hypo.getId()) + loserHypo.getScore()
									- loserPrevHypo.getScore();
							if (forwardScore.get(loserPrevHypo.getId()) == null
									|| forwardScore.get(loserPrevHypo.getId()) < fscore0) {
								forwardScore.put(prevHypo.getId(), fscore0);
								forward.put(prevHypo.getId(), hypo.getId());
							}
							// store outgoing info
							outgoingHyps.get(prevHypo).add(hypo);
						} // end for arc list
					} // end if arc list empty

				} // end if hypo connected
			} // end for hypo
		} // end for stack

		for (Hypothesis it : connectedList) {
			float estimatedScore = (float) (it.getScore() + forwardScore.get((it).getId()));
			estimatedScores.add(estimatedScore);
		}
	}
}
