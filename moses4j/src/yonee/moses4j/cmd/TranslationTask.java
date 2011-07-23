package yonee.moses4j.cmd;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import yonee.moses4j.moses.Hypothesis;
import yonee.moses4j.moses.InputType;
import yonee.moses4j.moses.Manager;
import yonee.moses4j.moses.StaticData;
import yonee.moses4j.moses.TrellisPath;
import yonee.moses4j.moses.TrellisPathList;
import yonee.moses4j.moses.Util;
import yonee.moses4j.moses.Word;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class TranslationTask {

	public TranslationTask(int lineNumber, InputType source, OutputCollector outputCollector,
			OutputCollector nbestCollector, OutputCollector wordGraphCollector,
			OutputCollector searchGraphCollector, OutputCollector detailedTranslationCollector) {
		m_source = source;
		m_lineNumber = (lineNumber);
		m_outputCollector = (outputCollector);
		m_nbestCollector = (nbestCollector);

		m_wordGraphCollector = (wordGraphCollector);
		m_searchGraphCollector = (searchGraphCollector);
		m_detailedTranslationCollector = (detailedTranslationCollector);
	}

	public void run() throws IOException {
		// #ifdef BOOST_HAS_PTHREADS
		// TRACE_ERR("Translating line " << m_lineNumber << "  in thread id " << pthread_self() <<
		// std::endl);
		// #endif
		final StaticData staticData = StaticData.instance();
		// Sentence sentence = new Sentence(FactorDirection.Input);
		Manager manager = new Manager(m_source, staticData.getSearchAlgorithm());
		manager.processSentence();

		// Word Graph
		if (m_wordGraphCollector != null) {
			StringWriter out = new StringWriter();
			manager.getWordGraph(m_lineNumber, out);
			m_wordGraphCollector.write(m_lineNumber, out.toString());
		}

		// Search Graph
		if (m_searchGraphCollector != null) {
			StringWriter out = new StringWriter();
			manager.outputSearchGraph(m_lineNumber, out);
			m_searchGraphCollector.write(m_lineNumber, out.toString());

			// #ifdef HAVE_PROTOBUF
			// if (staticData.getOutputSearchGraphPB()) {
			// ostringstream sfn;
			// sfn << staticData.getParam("output-search-graph-pb")[0] << '/' << m_lineNumber <<
			// ".pb" << ends;
			// string fn = sfn.str();
			// VERBOSE(2, "Writing search graph to " << fn << endl);
			// fstream output(fn.c_str(), ios.trunc | ios.binary | ios.out);
			// manager.SerializeSearchGraphPB(m_lineNumber, output);
			// }
			// #endif
		}

		if (m_outputCollector != null) {
			StringWriter out = new StringWriter();
			StringWriter debug = new StringWriter();

			// All derivations - send them to debug stream
			if (staticData.printAllDerivations()) {
				manager.printAllDerivations(m_lineNumber, debug);
			}

			// Best hypothesis
			Hypothesis bestHypo = null;
			if (!staticData.useMBR()) {
				bestHypo = manager.getBestHypothesis();
				if (bestHypo != null) {
					if (staticData.isPathRecoveryEnabled() != 0) {
						IOWrapper.outputInput(out, bestHypo);
						out.append("||| ");
					}
					IOWrapper.outputSurface(out, bestHypo, staticData.getOutputFactorOrder(),
							staticData.getReportSegmentation(), staticData.getReportAllFactors());
					if (VERBOSE.v(1)) {
						debug.append("BEST TRANSLATION: ").append(bestHypo.toString()).append("\n");
					}
				}
				out.append("\n");
			} else {
				int nBestSize = staticData.getMBRSize();
				if (nBestSize <= 0) {
					System.err
							.print("ERROR: negative size for number of MBR candidate translations not allowed (option mbr-size)\n");
					System.exit(1);
				}
				TrellisPathList nBestList = new TrellisPathList();
				manager.calcNBest(nBestSize, nBestList, true);
				VERBOSE.v(2, "size of n-best: " + nBestList.size() + " (" + nBestSize + ")" + "\n");
				if (VERBOSE.v(2)) {
					Util.printUserTime("calculated n-best list for (L)MBR decoding");
				}

				if (staticData.useLatticeMBR()) {
					if (m_nbestCollector != null) {
						// lattice mbr nbest
						List<LatticeMBRSolution> solutions = new ArrayList<LatticeMBRSolution>();
						int n = Math.min(nBestSize, staticData.getNBestSize());
						LatticeMBR.getLatticeMBRNBest(manager, nBestList, solutions, n);
						// PrintStream out;
						IOWrapper.outputLatticeMBRNBest(out, solutions, m_lineNumber);
						m_nbestCollector.write(m_lineNumber, out.toString());
					} else {
						// Lattice MBR decoding
						List<Word> mbrBestHypo = LatticeMBR.doLatticeMBR(manager, nBestList);
						IOWrapper.outputBestHypo(mbrBestHypo, m_lineNumber, staticData
								.getReportSegmentation(), staticData.getReportAllFactors(), out);
						if (VERBOSE.v(2)) {
							Util.printUserTime("finished Lattice MBR decoding");
						}
					}
				} else if (staticData.useConsensusDecoding()) {
					final TrellisPath conBestHypo = LatticeMBR.doConsensusDecoding(manager,
							nBestList);
					IOWrapper.outputBestHypo(conBestHypo, m_lineNumber, staticData
							.getReportSegmentation(), staticData.getReportAllFactors(), out);
					if (VERBOSE.v(2)) {
						Util.printUserTime("finished Consensus decoding");
					}
				} else {
					// MBR decoding
					final TrellisPath mbrBestHypo = LatticeMBR.doMBR(nBestList);
					IOWrapper.outputBestHypo(mbrBestHypo, m_lineNumber, staticData
							.getReportSegmentation(), staticData.getReportAllFactors(), out);
					if (VERBOSE.v(2)) {
						Util.printUserTime("finished MBR decoding");
					}

				}
			}
			m_outputCollector.write(m_lineNumber, out.toString(), debug.toString());
		}
		if (m_nbestCollector != null && !staticData.useLatticeMBR()) {
			TrellisPathList nBestList = new TrellisPathList();
			StringWriter out = new StringWriter();
			manager.calcNBest(staticData.getNBestSize(), nBestList, staticData.getDistinctNBest());
			IOWrapper.outputNBest(out, nBestList, staticData.getOutputFactorOrder(), m_lineNumber);
			m_nbestCollector.write(m_lineNumber, out.toString());
		}

		// detailed translation reporting
		if (m_detailedTranslationCollector != null) {
			StringWriter out = new StringWriter();
			TranslationAnalysis.printTranslationAnalysis(out, manager.getBestHypothesis());
			m_detailedTranslationCollector.write(m_lineNumber, out.toString());
		}

		if (VERBOSE.v(2)) {
			Util.printUserTime("Sentence Decoding Time:");
		}

		manager.calcDecoderStatistics();
		manager.cleanUp();
	}

	private InputType m_source;
	private int m_lineNumber;
	private OutputCollector m_outputCollector;
	private OutputCollector m_nbestCollector;
	private OutputCollector m_wordGraphCollector;
	private OutputCollector m_searchGraphCollector;
	private OutputCollector m_detailedTranslationCollector;

}
