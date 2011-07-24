package yonee.moses4j.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import yonee.moses4j.moses.ConfusionNet;
import yonee.moses4j.moses.Factor;
import yonee.moses4j.moses.FactorMask;
import yonee.moses4j.moses.GenerationDictionary;
import yonee.moses4j.moses.Hypothesis;
import yonee.moses4j.moses.InputType;
import yonee.moses4j.moses.Phrase;
import yonee.moses4j.moses.PhraseDictionaryFeature;
import yonee.moses4j.moses.Sentence;
import yonee.moses4j.moses.StatefulFeatureFunction;
import yonee.moses4j.moses.StatelessFeatureFunction;
import yonee.moses4j.moses.StaticData;
import yonee.moses4j.moses.TrellisPath;
import yonee.moses4j.moses.TrellisPathList;
import yonee.moses4j.moses.Util;
import yonee.moses4j.moses.Word;
import yonee.moses4j.moses.WordLattice;
import yonee.moses4j.moses.WordsRange;
import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.utils.ASSERT;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK
 */
public class IOWrapper {

	protected long translationId;

	protected int[] inputFactorOrder;
	protected int[] outputFactorOrder;
	protected FactorMask inputFactorUsed;
	protected String inputFilePath;
	protected BufferedReader inputFile;
	protected BufferedReader inputStream;

	protected Writer nBestStream, outputWordGraphStream, outputSearchGraphStream;
	protected Writer detailedTranslationReportingStream;
	protected boolean surpressSingleBestOutput;

	protected void initialization(int[] inputFactorOrder, int[] outputFactorOrder,
			FactorMask inputFactorUsed, int nBestSize, String nBestFilePath) {

		final StaticData staticData = StaticData.instance();

		// n-best
		surpressSingleBestOutput = false;

		try {
			if (nBestSize > 0) {
				if (nBestFilePath.equals("-") || nBestFilePath.equals("/dev/stdout")) {
					nBestStream = new PrintWriter(System.out);
					surpressSingleBestOutput = true;
				} else {
					nBestStream = new PrintWriter(new File(nBestFilePath));
				}
			}

			// wordgraph output
			if (staticData.getOutputWordGraph()) {
				String fileName = staticData.getParam("output-word-graph").get(0);
				outputWordGraphStream = new PrintWriter(new File(fileName));

			}

			// search graph output
			if (staticData.getOutputSearchGraph()) {
				String fileName;
				if (staticData.getOutputSearchGraphExtended())
					fileName = staticData.getParam("output-search-graph-extended").get(0);
				else
					fileName = staticData.getParam("output-search-graph").get(0);

				outputSearchGraphStream = new PrintWriter(new File(fileName));

			}

			// detailed translation reporting
			if (staticData.isDetailedTranslationReportingEnabled()) {
				String path = staticData.getDetailedTranslationReportingFilePath();
				detailedTranslationReportingStream = new PrintWriter(new File(path));
				ASSERT.a(detailedTranslationReportingStream != null);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public IOWrapper(int[] inputFactorOrder, final int[] outputFactorOrder,
			final FactorMask inputFactorUsed, int nBestSize, String nBestFilePath) {
		this.inputFactorOrder = inputFactorOrder;
		this.outputFactorOrder = outputFactorOrder;
		this.inputFactorUsed = inputFactorUsed;
		this.inputFile = null;
		this.inputStream = new BufferedReader(new InputStreamReader(System.in));// @RISK

		this.nBestStream = null;
		this.outputWordGraphStream = null;
		this.outputSearchGraphStream = null;
		this.detailedTranslationReportingStream = null;
		initialization(inputFactorOrder, outputFactorOrder, inputFactorUsed, nBestSize,
				nBestFilePath);
	}

	public IOWrapper(final int[] inputFactorOrder, final int[] outputFactorOrder,
			final FactorMask inputFactorUsed, int nBestSize, String nBestFilePath,
			final String infilePath) {
		this.inputFactorOrder = inputFactorOrder;
		this.outputFactorOrder = outputFactorOrder;
		this.inputFactorUsed = inputFactorUsed;
		// this.inputFilePath = inputFilePath;
		try {// YONEE
			this.inputFile = new BufferedReader(new FileReader(inputFilePath));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (inputFile != null)
					inputFile.close();
			} catch (IOException e) {
				System.err.println(e);
			}
		}
		this.nBestStream = null;
		this.outputWordGraphStream = null;
		this.outputSearchGraphStream = null;
		this.detailedTranslationReportingStream = null;
	}

	public InputType getInput(InputType inputType) {
		try {
			if (inputType.read(inputStream, inputFactorOrder) != 0) {
				long x = inputType.i().getTranslationId();
				if (x != 0) {
					if (x >= translationId)
						translationId = x + 1;
				} else
					inputType.i().setTranslationId(translationId++);

				return inputType;
			} else {
				inputType = null;
				return null;
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			inputType = null;
			return null;
		}
	}

	public void outputBestHypo(Hypothesis hypo, long translationId, boolean reportSegmentation,
			boolean reportAllFactors) throws IOException {
		if (hypo != null) {
			VERBOSE.v(1, "BEST TRANSLATION: " + hypo + "\n");
			VERBOSE.v(3, "Best path: ");
			backtrack(hypo);
			VERBOSE.v(3, "0\n");
			if (!surpressSingleBestOutput) {
				if (StaticData.instance().isPathRecoveryEnabled() != 0) {
					outputInput(new PrintWriter(System.out), hypo);
					System.out.print("||| ");
				}
				outputSurface(new PrintWriter(System.out), hypo, outputFactorOrder,
						reportSegmentation, reportAllFactors);
				System.out.println();
			}
		} else {
			VERBOSE.v(1, "NO BEST TRANSLATION\n");
			if (!surpressSingleBestOutput) {
				System.out.println();
			}
		}
	}

	public void outputNBestList(final TrellisPathList nBestList, long translationId)
			throws IOException {
		outputNBest(nBestStream, nBestList, outputFactorOrder, translationId);
	}

	public void outputLatticeMBRNBestList(List<LatticeMBRSolution> solutions, long translationId)
			throws IOException {
		outputLatticeMBRNBest(nBestStream, solutions, translationId);

	}

	public void backtrack(Hypothesis hypo) {
		if (hypo.getPrevHypo() != null) {
			VERBOSE.v(3, hypo.getId() + " <= ");
			backtrack(hypo.getPrevHypo());
		}
	}

	public void resetTranslationId() {
		translationId = 0;
	}

	public Writer getOutputWordGraphStream() {
		return outputWordGraphStream;
	}

	public Writer getOutputSearchGraphStream() {
		return outputSearchGraphStream;
	}

	public Writer getDetailedTranslationReportingStream() {
		ASSERT.a(detailedTranslationReportingStream != null);
		return detailedTranslationReportingStream;
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////
	/***
	 * print surface factor only for the given phrase
	 * 
	 * @throws IOException
	 */
	public static void outputSurface(Writer out, final Phrase phrase,
			final int[] outputFactorOrder, boolean reportAllFactors) throws IOException {
		ASSERT.a(outputFactorOrder.length > 0);

		if (reportAllFactors) {
			out.append(phrase.toString()); // YONEE

		} else {
			int size = phrase.getSize();
			for (int pos = 0; pos < size; pos++) {
				final Factor factor = phrase.getFactor(pos, outputFactorOrder[0]);
				out.append(factor.toString()); // YONEE

				for (int i = 1; i < outputFactorOrder.length; i++) {
					final Factor factor0 = phrase.getFactor(pos, outputFactorOrder[0]);
					out.append("|"); // YONEE
					out.append(factor0.toString());
				}
				out.append(" ");
			}
		}

	}

	public static void outputSurface(Writer out, final Hypothesis hypo,
			final int[] outputFactorOrder, boolean reportSegmentation, boolean reportAllFactors)
			throws IOException {
		if (hypo != null) {
			outputSurface(out, hypo.getPrevHypo(), outputFactorOrder, reportSegmentation,
					reportAllFactors);
			outputSurface(out, hypo.getCurrTargetPhrase(), outputFactorOrder, reportAllFactors);

			if (reportSegmentation && hypo.getCurrTargetPhrase().getSize() > 0) {
				out.append("|");
				out.append(hypo.getCurrSourceWordsRange().getStartPos() + "");
				out.append("-");
				out.append(hypo.getCurrSourceWordsRange().getEndPos() + "");
				out.append("|");

			}
		}
	}

	public static void outputBestHypo(TrellisPath path, long translationId,
			boolean reportSegmentation, boolean reportAllFactors, Writer out) throws IOException {
		final List<Hypothesis> edges = path.getEdges();
		for (int currEdge = (int) edges.size() - 1; currEdge >= 0; currEdge--) {
			final Hypothesis edge = edges.get(currEdge);
			outputSurface(out, edge.getCurrTargetPhrase(), StaticData.instance()
					.getOutputFactorOrder(), reportAllFactors);
			if (reportSegmentation && edge.getCurrTargetPhrase().getSize() > 0) {

				out.append("|");
				out.append(edge.getCurrSourceWordsRange().getStartPos() + "");
				out.append("-");
				out.append(edge.getCurrSourceWordsRange().getEndPos() + "");
				out.append("| ");
			}
		}
		out.append("\n");
	}

	public static void outputBestHypo(final List<Word> mbrBestHypo, long translationId,
			boolean reportSegmentation, boolean reportAllFactors, Writer out) throws IOException {
		for (int i = 0; i < mbrBestHypo.size(); i++) {
			final Factor factor = mbrBestHypo.get(i).getFactor(
					StaticData.instance().getOutputFactorOrder()[0]);
			if (i > 0)
				out.append(" ");
			out.append(factor.toString());
		}
		out.append("\n");
	}

	public static void outputInput(List<Phrase> map, final Hypothesis hypo) {
		if (hypo.getPrevHypo() != null) {
			outputInput(map, hypo.getPrevHypo());
			map.set(hypo.getCurrSourceWordsRange().getStartPos(), hypo.getSourcePhrase());
		}
	}

	public static void outputInput(Writer os, final Hypothesis hypo) throws IOException {
		int len = hypo.getInput().getSize();
		List<Phrase> inp_phrases = new ArrayList<Phrase>(len); // YONEE
		outputInput(inp_phrases, hypo);
		for (Phrase p : inp_phrases) {
			if (p != null)
				os.append(p.toString());
		}
	}

	public static void outputNBest(Writer out, final TrellisPathList nBestList,
			final int[] outputFactorOrder, long translationId) throws IOException {
		final StaticData staticData = StaticData.instance();
		boolean labeledOutput = staticData.isLabeledNBestList();
		boolean reportAllFactors = staticData.getReportAllFactorsNBest();
		boolean includeAlignment = staticData.nBestIncludesAlignment();
		// bool includeWordAlignment = staticData.PrintAlignmentInfoInNbest();

		for (TrellisPath path : nBestList) {

			final List<Hypothesis> edges = path.getEdges();

			// print the surface factor of the translation
			out.append(translationId + "");
			out.append(" ||| ");
			for (int currEdge = (int) edges.size() - 1; currEdge >= 0; currEdge--) {
				final Hypothesis edge = edges.get(currEdge);
				outputSurface(out, edge.getCurrTargetPhrase(), outputFactorOrder, reportAllFactors);
			}
			out.append(" ||| ");

			String lastName = "";
			List<StatefulFeatureFunction> sff = staticData.getScoreIndexManager()
					.getStatefulFeatureFunctions();
			for (int i = 0; i < sff.size(); i++) {
				if (labeledOutput && lastName != sff.get(i).getScoreProducerWeightShortName()) {
					lastName = sff.get(i).getScoreProducerWeightShortName();
					out.append(" ");
					out.append(lastName);
					out.append(":");
				}
				float[] scores = path.getScoreBreakdown().getScoresForProducer(sff.get(i));
				for (int j = 0; j < scores.length; ++j) {
					out.append((" " + scores[j]));
				}
			}

			List<StatelessFeatureFunction> slf = staticData.getScoreIndexManager()
					.getStatelessFeatureFunctions();
			for (int i = 0; i < slf.size(); i++) {
				if (labeledOutput && lastName != slf.get(i).getScoreProducerWeightShortName()) {
					lastName = slf.get(i).getScoreProducerWeightShortName();
					out.append((" " + lastName + ":"));
				}
				float[] scores = path.getScoreBreakdown().getScoresForProducer(slf.get(i));
				for (int j = 0; j < scores.length; ++j) {
					out.append((" " + scores[j]));
				}
			}

			// translation components
			if (StaticData.instance().getInputType() == InputTypeEnum.SentenceInput) {
				// translation components for text input
				List<PhraseDictionaryFeature> pds = StaticData.instance().getPhraseDictionaries();
				if (pds.size() > 0) {
					if (labeledOutput)
						out.append(" tm:");

					for (PhraseDictionaryFeature iter : pds) {
						float[] scores = path.getScoreBreakdown().getScoresForProducer(iter);
						for (int j = 0; j < scores.length; ++j)
							out.append((" " + scores[j]));
					}
				}
			} else {
				// translation components for Confusion Network input
				// first translation component has GetNumInputScores() scores
				// from the input Confusion Network
				// at the beginning of the vector
				List<PhraseDictionaryFeature> pds = StaticData.instance().getPhraseDictionaries();
				if (pds.size() > 0) {
					PhraseDictionaryFeature phf = pds.get(0);

					float[] scores = path.getScoreBreakdown().getScoresForProducer(phf);

					int pd_numinputscore = phf.getNumInputScores();

					if (pd_numinputscore != 0) {

						if (labeledOutput)
							out.append(" I:");

						for (int j = 0; j < pd_numinputscore; ++j)
							out.append((" " + scores[j]));
					}

					for (PhraseDictionaryFeature iter : pds) {
						float[] scores0 = path.getScoreBreakdown().getScoresForProducer(iter);

						int pd_numinputscore0 = iter.getNumInputScores();

						if (iter.equals(pds.get(0)) && labeledOutput) // @BUG equals
							out.append(" tm:");
						for (int j = pd_numinputscore0; j < scores0.length; ++j)
							out.append((" " + scores[j]));
					}
				}
			}

			// generation
			List<GenerationDictionary> gds = StaticData.instance().getGenerationDictionaries();
			if (gds.size() > 0) {
				if (labeledOutput)
					out.append(" g: ");

				for (GenerationDictionary gd : gds) {
					float[] scores = path.getScoreBreakdown().getScoresForProducer(gd);
					for (int j = 0; j < scores.length; j++) {
						out.append((scores[j] + " "));
					}
				}
			}

			// total
			out.append((" ||| " + path.getTotalScore()));

			// phrase-to-phrase alignment
			if (includeAlignment) {
				out.append(" |||");
				for (int currEdge = (int) edges.size() - 2; currEdge >= 0; currEdge--) {
					final Hypothesis edge = edges.get(currEdge);
					final WordsRange sourceRange = edge.getCurrSourceWordsRange();
					WordsRange targetRange = path.getTargetWordsRange(edge);
					out.append((" " + sourceRange.getStartPos()));
					if (sourceRange.getStartPos() < sourceRange.getEndPos()) {
						out.append(("-" + sourceRange.getEndPos()));
					}
					out.append(("=" + targetRange.getStartPos()));
					if (targetRange.getStartPos() < targetRange.getEndPos()) {
						out.append(("-" + targetRange.getEndPos()));
					}
				}
			}

			if (StaticData.instance().isPathRecoveryEnabled() != 0) {
				out.append("|||");
				outputInput(out, edges.get(0));
			}

			out.append("\n");
		}
		out.flush();
	}

	public static void outputLatticeMBRNBest(Writer out, final List<LatticeMBRSolution> solutions,
			long translationId) throws IOException {
		for (LatticeMBRSolution si : solutions) {
			out.append(Long.toString(translationId));
			out.append(" ||| ");
			final List<Word> mbrHypo = si.getWords();
			for (int i = 0; i < mbrHypo.size(); i++) {
				final Factor factor = mbrHypo.get(i).getFactor(
						StaticData.instance().getOutputFactorOrder()[0]);
				if (i > 0)
					out.append(" ");
				out.append(factor.toString());
			}
			out.append(" ||| ");
			out.append(("map: " + si.getMapScore()));
			out.append((" w: " + mbrHypo.size()));
			final List<Float> ngramScores = si.getNgramScores();
			for (int i = 0; i < ngramScores.size(); ++i) {
				out.append((" " + ngramScores.get(i)));
			}
			out.append(" ||| ");
			out.append(Float.toString(si.getScore()));

			out.append("\n");
		}
	}

	public static InputType readInput(IOWrapper ioWrapper, InputTypeEnum inputType) {
		InputType source = null;
		switch (inputType) {
		case SentenceInput:
			source = ioWrapper.getInput(new Sentence(FactorDirection.Input));
			break;
		case ConfusionNetworkInput:
			source = ioWrapper.getInput(new ConfusionNet());
			break;
		case WordLatticeInput:
			source = ioWrapper.getInput(new WordLattice());
			break;
		default:
			TRACE.err("Unknown input type: " + inputType + "\n");
		}
		return source;
		// return (source != null ? true : false);
	}

	public static IOWrapper getIODevice(final StaticData staticData) {
		IOWrapper ioWrapper;
		final int[] inputFactorOrder = staticData.getInputFactorOrder(), outputFactorOrder = staticData
				.getOutputFactorOrder();
		FactorMask inputFactorUsed = new FactorMask(inputFactorOrder);

		// io
		if (staticData.getParam("input-file").size() == 1) {
			VERBOSE.v(2, "IO from File\n");
			String filePath = staticData.getParam("input-file").get(0);

			ioWrapper = new IOWrapper(inputFactorOrder, outputFactorOrder, inputFactorUsed,
					staticData.getNBestSize(), staticData.getNBestFilePath(), filePath);
		} else {
			VERBOSE.v(1, "IO from STDOUT/STDIN\n");
			ioWrapper = new IOWrapper(inputFactorOrder, outputFactorOrder, inputFactorUsed,
					staticData.getNBestSize(), staticData.getNBestFilePath());
		}
		ioWrapper.resetTranslationId();

		if (VERBOSE.v(1))
			Util.printUserTime("Created input-output object");

		return ioWrapper;
	}

}
