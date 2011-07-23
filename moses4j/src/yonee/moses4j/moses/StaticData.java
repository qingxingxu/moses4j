package yonee.moses4j.moses;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import java.util.concurrent.PriorityBlockingQueue;

import yonee.moses4j.moses.TypeDef.DecodeType;
import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.moses4j.moses.TypeDef.LMImplementation;
import yonee.moses4j.moses.TypeDef.PhraseTableImplementation;
import yonee.moses4j.moses.TypeDef.SearchAlgorithm;
import yonee.moses4j.moses.TypeDef.SourceLabelOverlap;
import yonee.moses4j.moses.TypeDef.XmlInputType;
import yonee.utils.ASSERT;
import yonee.utils.Pair;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 * @UPDATE
 * 		1. setBooleanParameter （指针返回参数，java无法实现）-> getBooleanParameter,
 */
public class StaticData {
	class UnknownLHSList extends ArrayList<Pair<String, Float>> {
		private static final long serialVersionUID = -2472231744822706208L;
	};

	static int calcMax(int x, final int y[], final int z[]) {
		int max = x;
		for (int i : y)
			if (i > max)
				max = i;
		for (int i : z)
			if (i > max)
				max = i;
		return max;

	}

	static int calcMax(int x, final int[] y) {
		int max = x;
		for (Integer i : y) {
			if (i > max)
				max = i;
		}
		return max;

	}

	// ////////////////

	private static StaticData s_instance = new StaticData();

	protected Map<Long, Phrase> m_constraints = new HashMap<Long, Phrase>();
	protected List<PhraseDictionaryFeature> m_phraseDictionary = new ArrayList<PhraseDictionaryFeature>();
	protected List<GenerationDictionary> m_generationDictionary = new ArrayList<GenerationDictionary>();
	protected Parameter m_parameter;
	protected int[] m_inputFactorOrder, m_outputFactorOrder;
	protected LMList m_languageModel = new LMList();
	protected ScoreIndexManager m_scoreIndexManager = new ScoreIndexManager();
	protected List<Float> m_allWeights = new ArrayList<Float>();
	protected List<LexicalReordering> m_reorderModels = new ArrayList<LexicalReordering>();
	protected List<GlobalLexicalModel> m_globalLexicalModels = new ArrayList<GlobalLexicalModel>();
	// Initial = 0 = can be used when creating poss trans
	// Other = 1 = used to calculate LM score once all steps have been processed
	protected float m_beamWidth, m_earlyDiscardingThreshold, m_translationOptionThreshold,
			m_weightDistortion, m_weightWordPenalty, m_wordDeletionWeight, m_weightUnknownWord;

	// PhraseTrans, Generation & LanguageModelScore has multiple weights.
	protected int m_maxDistortion;
	// do it differently from old pharaoh
	// -ve = no limit on distortion
	// 0 = no disortion (monotone in old pharaoh)
	protected boolean m_reorderingConstraint; // use additional reordering
	// constraints
	protected int m_maxHypoStackSize // hypothesis-stack size that triggers
			// pruning
			,
			m_minHypoStackDiversity // minimum number of hypothesis in stack for
			// each source word coverage
			, m_nBestSize, m_nBestFactor,
			m_maxNoTransOptPerCoverage,
			m_maxNoPartTransOpt,
			m_maxPhraseLength, m_numLinkParams;

	protected String m_constraintFileName;

	protected String m_nBestFilePath;
	protected boolean m_fLMsLoaded, m_labeledNBestList, m_nBestIncludesAlignment;
	/***
	 * false = treat unknown words as unknowns, and translate them as
	 * themselves; true = drop (ignore) them
	 */
	protected boolean m_dropUnknown;
	protected boolean m_wordDeletionEnabled;

	protected boolean m_disableDiscarding;
	protected boolean m_printAllDerivations;

	protected boolean m_sourceStartPosMattersForRecombination;
	protected boolean m_recoverPath;
	protected boolean m_outputHypoScore;

	protected SearchAlgorithm m_searchAlgorithm;
	protected InputTypeEnum m_inputType;
	protected int m_numInputScores;

	protected int m_verboseLevel;
	protected DistortionScoreProducer m_distortionScoreProducer;
	protected WordPenaltyProducer m_wpProducer;
	protected UnknownWordPenaltyProducer m_unknownWordPenaltyProducer;
	protected boolean m_reportSegmentation;
	protected boolean m_reportAllFactors;
	protected boolean m_reportAllFactorsNBest;
	protected String m_detailedTranslationReportingFilePath;
	protected boolean m_onlyDistinctNBest;
	protected boolean m_UseAlignmentInfo;
	protected boolean m_PrintAlignmentInfo;
	protected boolean m_PrintAlignmentInfoNbest;

	protected String m_factorDelimiter; // ! by default, |, but it can be
	// changed
	protected int m_maxFactorIdx[] = new int[2]; // ! number of factors on
	// source and target side
	protected int m_maxNumFactors; // ! max number of factors on both source and
	// target sides

	protected XmlInputType m_xmlInputType; // ! method for handling sentence XML
	// input

	protected boolean m_mbr; // ! use MBR decoder
	protected boolean m_useLatticeMBR; // ! use MBR decoder
	protected boolean m_useConsensusDecoding; // ! Use Consensus decoding
	// (DeNero et al 2009)
	protected int m_mbrSize; // ! number of translation candidates considered
	protected float m_mbrScale; // ! scaling factor for computing marginal
	// probability of candidate translation
	protected int m_lmbrPruning; // ! average number of nodes per word wanted in
	// pruned lattice
	protected List<Float> m_lmbrThetas; // ! theta(s) for lattice mbr
	// calculation
	protected boolean m_useLatticeHypSetForLatticeMBR; // ! to use nbest as
	// hypothesis set during
	// lattice MBR
	protected float m_lmbrPrecision; // ! unigram precision theta - see Tromble
	// et al 08 for more details
	protected float m_lmbrPRatio; // ! decaying factor for ngram thetas - see
	// Tromble et al 08 for more details
	protected float m_lmbrMapWeight; // ! Weight given to the map solution. See
	// Kumar et al 09 for details

	protected int m_lmcache_cleanup_threshold; // ! number of translations after
	// which LM claenup is performed
	// (0=never, N=after N
	// translations; default is 1)

	protected boolean m_timeout; // ! use timeout
	protected int m_timeout_threshold; // ! seconds after which time out is
	// activated

	protected boolean m_useTransOptCache; // ! flag indicating, if the
	// persistent translation option
	// cache should be used
	protected Map<Pair<Integer, Phrase>, Pair<TranslationOptionList, Long>> m_transOptCache = new HashMap<Pair<Integer, Phrase>, Pair<TranslationOptionList, Long>>();// !
	// persistent
	// translation
	// option
	// cache
	protected int m_transOptCacheMaxSize; // ! maximum size for persistent
	// translation option cache
	// FIXME: Single lock for cache not most efficient. However using a
	// reader-writer for LRU cache is tricky - how to record last used time?
	// #ifdef WITH_THREADS
	// mutable boost::mutex m_transOptCacheMutex;
	// #endif
	protected boolean m_isAlwaysCreateDirectTranslationOption;
	// ! constructor. only the 1 static variable can be created

	protected boolean m_outputWordGraph; // ! whether to output word graph
	protected boolean m_outputSearchGraph; // ! whether to output search graph
	protected boolean m_outputSearchGraphExtended; // ! ... in extended format
	// #ifdef HAVE_PROTOBUF
	// boolean m_outputSearchGraphPB; //! whether to output search graph as a
	// protobuf
	// #endif

	protected int m_cubePruningPopLimit;
	protected int m_cubePruningDiversity;
	protected int m_ruleLimit;

	// Initial = 0 = can be used when creating poss trans
	// Other = 1 = used to calculate LM score once all steps have been processed
	protected Word m_inputDefaultNonTerminal = new Word(), m_outputDefaultNonTerminal = new Word();
	protected SourceLabelOverlap m_sourceLabelOverlap;

	protected UnknownLHSList m_unknownLHS;

	protected StaticData() {
		m_fLMsLoaded = false;
		m_inputType = InputTypeEnum.SentenceInput;
		m_numInputScores = 0;
		m_distortionScoreProducer = null;
		m_wpProducer = null;
		m_detailedTranslationReportingFilePath = "";
		m_onlyDistinctNBest = false;
		m_factorDelimiter = "|"; // default delimiter between factors
		m_isAlwaysCreateDirectTranslationOption = false;
		m_sourceStartPosMattersForRecombination = false;
		m_numLinkParams = 1;

		m_maxFactorIdx[0] = 0; // source side
		m_maxFactorIdx[1] = 0; // target side

		// memory pools
		Phrase.initializeMemPool();

	}

	protected void loadPhraseBasedParameters() {
		final List<String> distortionWeights = m_parameter.getParam("weight-d");

		m_weightDistortion = Float.parseFloat(distortionWeights.get(0));

		m_distortionScoreProducer = new DistortionScoreProducer(m_scoreIndexManager);
		m_allWeights.add(m_weightDistortion);
	}

	protected void loadChartDecodingParameters() {
		try {
			loadNonTerminals();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// source label overlap
		if (m_parameter.getParam("source-label-overlap").size() > 0) {

			m_sourceLabelOverlap = SourceLabelOverlap.values()[Integer.parseInt(m_parameter
					.getParam("source-label-overlap").get(0))];
		} else {
			m_sourceLabelOverlap = SourceLabelOverlap.SourceLabelOverlapAdd;
		}

		m_ruleLimit = (m_parameter.getParam("rule-limit").size() > 0) ? Integer
				.parseInt(m_parameter.getParam("rule-limit").get(0))
				: TypeDef.DEFAULT_MAX_TRANS_OPT_SIZE;
	}

	protected void loadNonTerminals() throws IOException {
		String defaultNonTerminals;

		if (m_parameter.getParam("non-terminals").size() == 0) {
			defaultNonTerminals = "X";
		} else {
			String tokens[] = Util.tokenize(m_parameter.getParam("non-terminals").get(0));
			defaultNonTerminals = tokens[0];
		}

		FactorCollection factorCollection = FactorCollection.instance();

		m_inputDefaultNonTerminal.setIsNonTerminal(true);
		final Factor sourceFactor = factorCollection.addFactor(FactorDirection.Input, 0,
				defaultNonTerminals);
		m_inputDefaultNonTerminal.setFactor(0, sourceFactor);

		m_outputDefaultNonTerminal.setIsNonTerminal(true);
		final Factor targetFactor = factorCollection.addFactor(FactorDirection.Output, 0,
				defaultNonTerminals);
		m_outputDefaultNonTerminal.setFactor(0, targetFactor);

		// for unknwon words
		if (m_parameter.getParam("unknown-lhs").size() == 0) {
			Pair<String, Float> entry = new Pair<String, Float>(defaultNonTerminals, 0.0f);
			m_unknownLHS.add(entry);
		} else {
			final String filePath = m_parameter.getParam("unknown-lhs").get(0);

			BufferedReader br = new BufferedReader(new FileReader(filePath));

			String line = null;
			while ((line = br.readLine()) != null) {
				String tokens[] = Util.tokenize(line);
				ASSERT.a(tokens.length == 2);
				Pair<String, Float> entry = new Pair<String, Float>(tokens[0], Float
						.parseFloat(tokens[1]));
				m_unknownLHS.add(entry);
			}
			br.close();

		}
	}

	// ! helper fn to set boolean param from ini file/command line
	protected boolean getBooleanParameter(String parameterName, boolean defaultValue) {
		// default value if nothing is specified

		boolean paramter = defaultValue;
		if (!m_parameter.isParamSpecified(parameterName)) {
			return paramter;
		}

		// if parameter is just specified as, e.g. "-parameter" set it true
		if (m_parameter.getParam(parameterName).size() == 0) {
			paramter = true;
		}

		// if paramter is specified "-parameter true" or "-parameter false"
		else if (m_parameter.getParam(parameterName).size() == 1) {
			paramter = Boolean.valueOf(m_parameter.getParam(parameterName).get(0));
		}
		return paramter;
	}

	/***
	 * load all language models as specified in ini file
	 */
	protected boolean loadLanguageModels() {
		if (m_parameter.getParam("lmodel-file").size() > 0) {
			// weights
			List<Float> weightAll = Scan.toFloat(m_parameter.getParam("weight-l"));

			for (int i = 0; i < weightAll.size(); i++) {
				m_allWeights.add(Float.valueOf(weightAll.get(i)));
			}

			// dictionary upper-bounds fo all IRST LMs
			List<Integer> LMdub = new ArrayList<Integer>();// m_parameter.getParam("lmodel-dub");
			if (m_parameter.getParam("lmodel-dub").size() == 0) {
				for (int i = 0; i < m_parameter.getParam("lmodel-file").size(); i++)
					LMdub.add(0);
			}

			// initialize n-gram order for each factor. populated only by
			// factored lm
			final List<String> lmVector = m_parameter.getParam("lmodel-file");

			for (int i = 0; i < lmVector.size(); i++) {
				String[] token = Util.tokenize(lmVector.get(i));
				if (token.length != 4 && token.length != 5) {
					UserMessage
							.add("Expected format 'LM-TYPE FACTOR-TYPE NGRAM-ORDER filePath [mapFilePath (only for IRSTLM)]'");
					return false;
				}
				// type = implementation, SRI, IRST etc
				LMImplementation lmImplementation = LMImplementation.values()[Integer
						.valueOf(token[0])];

				// factorType = 0 = Surface, 1 = POS, 2 = Stem, 3 = Morphology,
				// etc
				Integer[] factorTypes = Util.tokenizeInteger(token[1], ",");

				// nGramOrder = 2 = bigram, 3 = trigram, etc
				int nGramOrder = Integer.valueOf(token[2]);

				String languageModelFile = token[3];
				if (token.length == 5) {
					if (lmImplementation == LMImplementation.IRST)
						languageModelFile += " " + token[4];
					else {
						UserMessage
								.add("Expected format 'LM-TYPE FACTOR-TYPE NGRAM-ORDER filePath [mapFilePath (only for IRSTLM)]'");
						return false;
					}
				}

				if (VERBOSE.v(1)) {
					Util.printUserTime("Start loading LanguageModel " + languageModelFile);
				}

				LanguageModel lm = null;
				try {
					lm = LanguageModelFactory.createLanguageModel(lmImplementation, Arrays
							.asList(factorTypes), nGramOrder, languageModelFile, weightAll.get(i),
							m_scoreIndexManager, LMdub.get(i));
				} catch (IOException e) {
					UserMessage.add("no LM created. We probably don't have it compiled" + e);
					return false;
				}
				if (lm == null) {
					UserMessage.add("no LM created. We probably don't have it compiled");
					return false;
				}

				m_languageModel.add(lm);
			}
		}
		// flag indicating that language models were loaded,
		// since phrase table loading requires their presence
		m_fLMsLoaded = true;
		if (VERBOSE.v(1)) {
			Util.printUserTime("Finished loading LanguageModels");
		}
		return true;
	}

	/***
	 * load not only the main phrase table but also any auxiliary tables that
	 * depend on which features are being used (eg word-deletion, word-insertion
	 * tables)
	 */
	protected boolean loadPhraseTables() {
		VERBOSE.v(2, "About to LoadPhraseTables\n");

		// language models must be loaded prior to loading phrase tables
		ASSERT.a(m_fLMsLoaded);
		// load phrase translation tables
		if (m_parameter.getParam("ttable-file").size() > 0) {
			// weights
			List<Float> weightAll = Scan.toFloat(m_parameter.getParam("weight-t"));

			final List<String> translationVector = m_parameter.getParam("ttable-file");
			final List<Integer> maxTargetPhrase = Scan.toInt(m_parameter.getParam("ttable-limit"));

			if (maxTargetPhrase.size() == 1 && translationVector.size() > 1) {
				VERBOSE.v(1, "Using uniform ttable-limit of " + maxTargetPhrase.get(0)
						+ " for all translation tables.\n");
				for (Integer i = 1; i < translationVector.size(); i++)
					maxTargetPhrase.add(maxTargetPhrase.get(0));
			} else if (maxTargetPhrase.size() != 1
					&& maxTargetPhrase.size() < translationVector.size()) {
				StringBuilder strme = new StringBuilder();
				strme.append("You specified ").append(translationVector.size()).append(
						" translation tables, but only ").append(maxTargetPhrase.size()).append(
						" ttable-limits.");
				UserMessage.add(strme.toString());
				return false;
			}

			int index = 0;
			int weightAllOffset = 0;
			boolean oldFileFormat = false;
			for (Integer currDict = 0; currDict < translationVector.size(); currDict++) {
				String token[] = Util.tokenize(translationVector.get(currDict));

				if (currDict == 0 && token.length == 4) {
					VERBOSE
							.v(
									1,
									"Warning: Phrase table specification in old 4-field format. Assuming binary phrase tables (type 1)!\n");
					oldFileFormat = true;
				}

				if (!oldFileFormat && token.length < 5 || oldFileFormat && token.length != 4) {
					UserMessage.add("invalid phrase table specification");
					return false;
				}

				PhraseTableImplementation implementation = PhraseTableImplementation.values()[Integer
						.valueOf(token[0])];
				if (oldFileFormat) {
					String tmp[] = token;
					token = new String[tmp.length + 1];
					System.arraycopy(tmp, 0, token, 1, tmp.length);
					/*
					 * token[4] = token[3]; token[3] = token[2]; token[2] =
					 * token[1]; token[1] = token[0];
					 */
					token[0] = "1";
					implementation = PhraseTableImplementation.Binary;
				} else
					implementation = PhraseTableImplementation.values()[Integer.valueOf(token[0])];

				ASSERT.a(token.length >= 5);
				// characteristics of the phrase table

				int input[] = Util.tokenizeInt(token[1], ",");
				int output[] = Util.tokenizeInt(token[2], ",");
				m_maxFactorIdx[0] = calcMax(m_maxFactorIdx[0], input);
				m_maxFactorIdx[1] = calcMax(m_maxFactorIdx[1], output);
				m_maxNumFactors = Math.max(m_maxFactorIdx[0], m_maxFactorIdx[1]) + 1;
				Integer numScoreComponent = Integer.valueOf(token[3]);
				String filePath = token[4];

				ASSERT.a(weightAll.size() >= weightAllOffset + numScoreComponent);

				// weights for this phrase dictionary
				// first InputScores (if any), then translation scores
				List<Float> weight = new ArrayList<Float>();

				if (currDict == 0
						&& (m_inputType == InputTypeEnum.ConfusionNetworkInput || m_inputType == InputTypeEnum.WordLatticeInput)) { // TODO.
					// find
					// what
					// the
					// assumptions
					// made
					// by
					// confusion
					// network
					// about
					// phrase
					// table
					// output
					// which
					// makes
					// it only work with binrary file. This is a hack

					m_numInputScores = m_parameter.getParam("weight-i").size();
					for (int k = 0; k < m_numInputScores; ++k)
						weight.add(Float.valueOf(m_parameter.getParam("weight-i").get(k)));

					if (m_parameter.getParam("link-param-count").size() != 0)
						m_numLinkParams = Integer.valueOf(m_parameter.getParam("link-param-count")
								.get(0));

					// print some info about this interaction:
					if (m_numLinkParams == m_numInputScores) {
						VERBOSE
								.v(
										1,
										"specified equal numbers of link parameters and insertion weights, not using non-epsilon 'real' word link count.\n");
					} else if ((m_numLinkParams + 1) == m_numInputScores) {
						VERBOSE
								.v(
										1,
										"WARN: "
												+ m_numInputScores
												+ " insertion weights found and only "
												+ m_numLinkParams
												+ " link parameters specified, applying non-epsilon 'real' word link count for last feature weight.\n");
					} else {
						StringBuilder strme = new StringBuilder();
						strme.append("You specified ").append(m_numInputScores).append(
								" input weights (weight-i), but you specified ").append(
								m_numLinkParams).append(" link parameters (link-param-count)!");
						UserMessage.add(strme.toString());
						return false;
					}

				}
				if (m_inputType.ordinal() == 0) {
					m_numInputScores = 0;
				}
				// this number changes depending on what phrase table we're
				// talking about: only 0 has the weights on it
				Integer tableInputScores = (currDict == 0 ? m_numInputScores : 0);

				for (Integer currScore = 0; currScore < numScoreComponent; currScore++)
					weight.add(Float.valueOf(weightAll.get(weightAllOffset + currScore)));

				if (weight.size() - tableInputScores != numScoreComponent) {
					StringBuilder strme = new StringBuilder();
					strme.append("Your phrase table has ").append(numScoreComponent).append(
							" scores, but you specified ").append(
							(weight.size() - tableInputScores)).append(" weights!");
					UserMessage.add(strme.toString());
					return false;
				}

				weightAllOffset += numScoreComponent;
				numScoreComponent += tableInputScores;

				String targetPath = null, alignmentsFile = null;
				if (implementation == PhraseTableImplementation.SuffixArray) {
					targetPath = token[5];
					alignmentsFile = token[6];
				}

				ASSERT.a(numScoreComponent == weight.size());
				m_allWeights.addAll(weight);

				if (VERBOSE.v(1))
					Util.printUserTime("Start loading PhraseTable " + filePath);
				VERBOSE.v(1, "filePath: " + filePath + "\n");

				PhraseDictionaryFeature pdf = new PhraseDictionaryFeature(implementation,
						numScoreComponent.intValue(), (currDict == 0 ? m_numInputScores : 0),
						input, output, filePath, weight, maxTargetPhrase.get(index), targetPath,
						alignmentsFile);

				m_phraseDictionary.add(pdf);

				index++;
			}
		}

		if (VERBOSE.v(1))
			Util.printUserTime("Finished loading phrase tables");
		return true;
	}

	// ! load all generation tables as specified in ini file
	protected boolean loadGenerationTables() {
		if (m_parameter.getParam("generation-file").size() > 0) {
			final List<String> generationVector = m_parameter.getParam("generation-file");
			final List<Float> weight = Scan.toFloat(m_parameter.getParam("weight-generation"));

			if (VERBOSE.v(1)) {
				TRACE.err("weight-generation: ");
				for (int i = 0; i < weight.size(); i++) {
					TRACE.err(weight.get(i) + "\t");
				}
				TRACE.err("\n");
			}
			int currWeightNum = 0;

			for (int currDict = 0; currDict < generationVector.size(); currDict++) {
				String[] token = Util.tokenize(generationVector.get(currDict));
				int[] input = Util.tokenizeInt(token[0], ",");
				int[] output = Util.tokenizeInt(token[1], ",");
				m_maxFactorIdx[1] = calcMax(m_maxFactorIdx[1], input, output);
				String filePath;
				int numFeatures;

				numFeatures = Integer.valueOf(token[2]);
				filePath = token[3];

				if (!Util.fileExists(filePath) && Util.fileExists(filePath + ".gz")) {
					filePath += ".gz";
				}

				VERBOSE.v(1, filePath + "\n");

				m_generationDictionary.add(new GenerationDictionary(numFeatures,
						m_scoreIndexManager));
				GenerationDictionary back = m_generationDictionary.get(m_generationDictionary
						.size() - 1);
				ASSERT.a(back != null && "could not create GenerationDictionary" != null);
				try {

					if (!back.load(input, output, filePath, FactorDirection.Output)) {
						back = null;
						return false;
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				for (int i = 0; i < numFeatures; i++) {
					ASSERT.a(currWeightNum < weight.size());
					m_allWeights.add(weight.get(currWeightNum++));
				}
			}
			if (currWeightNum != weight.size()) {
				TRACE
						.err("  [WARNING] config file has "
								+ weight.size()
								+ " generation weights listed, but the configuration for generation files indicates there should be "
								+ currWeightNum + "!\n");
			}
		}

		return true;
	}

	// ! load decoding steps
	protected boolean loadLexicalReorderingModel() {
		VERBOSE.v(1, "Loading lexical distortion models...");
		final List<String> fileStr = m_parameter.getParam("distortion-file");
		final List<String> weightsStr = m_parameter.getParam("weight-d");

		List<Float> weights = new ArrayList<Float>();
		int w = 1; // cur weight
		int f = 0; // cur file
		// get weights values
		VERBOSE.v(1, "have " + fileStr.size() + " models\n");
		for (int j = 0; j < weightsStr.size(); ++j) {
			weights.add(Float.valueOf(weightsStr.get(j)));
		}
		// load all models
		for (int i = 0; i < fileStr.size(); ++i) {
			String[] spec = Util.tokenize(fileStr.get(f), " ");
			++f; // mark file as consumed
			if (spec.length != 4) {
				UserMessage
						.add("Invalid Lexical Reordering Model Specification: " + fileStr.get(f));
				return false;
			}

			// spec[0] = factor map
			// spec[1] = name
			// spec[2] = num weights
			// spec[3] = fileName

			// decode factor map

			int[] input = null, output;
			String[] inputfactors = Util.tokenize(spec[0], "-");
			if (inputfactors.length == 2) {
				input = Util.tokenizeInt(inputfactors[0], ",");
				output = Util.tokenizeInt(inputfactors[1], ",");
			} else if (inputfactors.length == 1) {
				// if there is only one side assume it is on e side... why?
				output = Util.tokenizeInt(inputfactors[0], ",");
			} else {
				// format error
				return false;
			}

			String modelType = spec[1];

			// decode num weights and fetch weights from array
			List<Float> mweights = new ArrayList<Float>();
			int numWeights = Integer.valueOf(spec[2]);
			for (int k = 0; k < numWeights; ++k, ++w) {
				if (w >= weights.size()) {
					UserMessage
							.add("Lexicalized distortion model: Not enough weights, add to [weight-d]");
					return false;
				} else
					mweights.add(weights.get(w));
			}

			String filePath = spec[3];

			m_reorderModels
					.add(new LexicalReordering(input, output, modelType, filePath, mweights));
		}
		return true;
	}

	protected boolean loadGlobalLexicalModel() {
		final List<Float> weight = Scan.toFloat(m_parameter.getParam("weight-lex"));
		final List<String> file = m_parameter.getParam("global-lexical-file");

		if (weight.size() != file.size()) {
			System.err
					.println("number of weights and models for the global lexical model does not match ("
							+ weight.size() + " != " + file.size() + ")");
			return false;
		}

		for (int i = 0; i < weight.size(); i++) {
			String[] spec = Util.tokenize(file.get(i), " ");
			if (spec.length != 2) {
				System.err.println("wrong global lexical model specification: " + file.get(i));
				return false;
			}
			String factors[] = Util.tokenize(spec[0], "-");
			if (factors.length != 2) {
				System.err.println("wrong factor definition for global lexical model: " + spec[0]);
				return false;
			}
			int inputFactors[] = Util.tokenizeInt(factors[0], ",");
			int outputFactors[] = Util.tokenizeInt(factors[1], ",");
			m_globalLexicalModels.add(new GlobalLexicalModel(spec[1], weight.get(i), inputFactors,
					outputFactors));
		}
		return true;
	}

	protected void reduceTransOptCache() {
		if (m_transOptCache.size() <= m_transOptCacheMaxSize)
			return; // not full
		long t = System.currentTimeMillis();

		// find cutoff for last used time
		Queue<Long> lastUsedTimes = new PriorityBlockingQueue<Long>();

		for (Map.Entry<Pair<Integer, Phrase>, Pair<TranslationOptionList, Long>> e : m_transOptCache
				.entrySet()) {
			lastUsedTimes.add(e.getValue().second);
		}

		for (int i = 0; i < lastUsedTimes.size() - m_transOptCacheMaxSize / 2; i++)
			lastUsedTimes.poll();

		long cutoffLastUsedTime = lastUsedTimes.peek();

		// remove all old entries
		Iterator<Map.Entry<Pair<Integer, Phrase>, Pair<TranslationOptionList, Long>>> iter = m_transOptCache
				.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Pair<Integer, Phrase>, Pair<TranslationOptionList, Long>> e = iter.next();
			if (e.getValue().second < cutoffLastUsedTime) {
				iter.remove();
			}
		}

		VERBOSE.v(2, "Reduced persistent translation option cache in "
				+ ((System.currentTimeMillis() - t) / (float) 1000) + " seconds.\n");
	}

	protected boolean m_continuePartialTranslation;

	public final boolean isAlwaysCreateDirectTranslationOption() {
		return m_isAlwaysCreateDirectTranslationOption;
	}

	// ! return static instance for use like global variable
	public static final StaticData instance() {
		return s_instance;
	}

	/**
	 * delete current static instance and replace with another. Used by gui
	 * front end
	 */

	public static void reset() {
		s_instance = new StaticData();
	}

	/**
	 * load data into static instance. This function is required as LoadData()
	 * is not final
	 */
	public static boolean loadDataStatic(Parameter parameter) {

		try {
			return s_instance.loadData(parameter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Main function to load everything. Also initialize the Parameter object
	 * 
	 * @throws IOException
	 */
	public boolean loadData(Parameter parameter) throws IOException {
		Util.resetUserTime();
		m_parameter = parameter;

		// verbose level
		m_verboseLevel = 1;
		if (m_parameter.getParam("verbose").size() == 1) {
			m_verboseLevel = Integer.valueOf(m_parameter.getParam("verbose").get(0));
		}

		// to cube or not to cube
		m_searchAlgorithm = (m_parameter.getParam("search-algorithm").size() > 0) ? SearchAlgorithm
				.values()[Integer.valueOf(m_parameter.getParam("search-algorithm").get(0))]
				: SearchAlgorithm.Normal;

		if (m_searchAlgorithm == SearchAlgorithm.ChartDecoding)
			loadChartDecodingParameters();
		else
			loadPhraseBasedParameters();

		// input type has to be specified BEFORE loading the phrase tables!
		if (m_parameter.getParam("inputtype").size() != 0)
			m_inputType = InputTypeEnum.values()[Integer.valueOf(m_parameter.getParam("inputtype")
					.get(0))];
		String s_it = "text input";
		if (m_inputType.ordinal() == 1) {
			s_it = "confusion net";
		}
		if (m_inputType.ordinal() == 2) {
			s_it = "word lattice";
		}
		VERBOSE.v(2, "input type is: " + s_it + "\n");

		if (m_parameter.getParam("recover-input-path").size() != 0) {
			m_recoverPath = Boolean.valueOf(m_parameter.getParam("recover-input-path").get(0));
			if (m_recoverPath && m_inputType == InputTypeEnum.SentenceInput) {
				TRACE
						.err("--recover-input-path should only be used with confusion net or word lattice input!\n");
				m_recoverPath = false;
			}
		}

		// factor delimiter
		if (m_parameter.getParam("factor-delimiter").size() > 0) {
			m_factorDelimiter = m_parameter.getParam("factor-delimiter").get(0);
		}

		m_continuePartialTranslation = getBooleanParameter("continue-partial-translation", false);

		// word-to-word alignment
		m_UseAlignmentInfo = getBooleanParameter("use-alignment-info", false);
		m_PrintAlignmentInfo = getBooleanParameter("print-alignment-info", false);
		m_PrintAlignmentInfoNbest = getBooleanParameter("print-alignment-info-in-n-best", false);

		m_outputHypoScore = getBooleanParameter("output-hypo-score", false);

		if (!m_UseAlignmentInfo && m_PrintAlignmentInfo) {
			TRACE
					.err("--print-alignment-info should only be used together with \"--use-alignment-info true\". Continue forcing to false.\n");
			m_PrintAlignmentInfo = false;
		}
		if (!m_UseAlignmentInfo && m_PrintAlignmentInfoNbest) {
			TRACE
					.err("--print-alignment-info-in-n-best should only be used together with \"--use-alignment-info true\". Continue forcing to false.\n");
			m_PrintAlignmentInfoNbest = false;
		}

		// n-best
		if (m_parameter.getParam("n-best-list").size() >= 2) {
			m_nBestFilePath = m_parameter.getParam("n-best-list").get(0);
			m_nBestSize = Integer.valueOf(m_parameter.getParam("n-best-list").get(1));
			m_onlyDistinctNBest = (m_parameter.getParam("n-best-list").size() > 2 && m_parameter
					.getParam("n-best-list").get(2) == "distinct");
		} else if (m_parameter.getParam("n-best-list").size() == 1) {
			UserMessage.add("ERROR: wrong format for switch -n-best-list file size");
			return false;
		} else {
			m_nBestSize = 0;
		}
		if (m_parameter.getParam("n-best-factor").size() > 0) {
			m_nBestFactor = Integer.valueOf(m_parameter.getParam("n-best-factor").get(0));
		} else {
			m_nBestFactor = 20;
		}

		// word graph
		if (m_parameter.getParam("output-word-graph").size() == 2)
			m_outputWordGraph = true;
		else
			m_outputWordGraph = false;

		// search graph
		if (m_parameter.getParam("output-search-graph").size() > 0) {
			if (m_parameter.getParam("output-search-graph").size() != 1) {
				UserMessage.add(("ERROR: wrong format for switch -output-search-graph file"));
				return false;
			}
			m_outputSearchGraph = true;
		}
		// ... in extended format
		else if (m_parameter.getParam("output-search-graph-extended").size() > 0) {
			if (m_parameter.getParam("output-search-graph-extended").size() != 1) {
				UserMessage
						.add(("ERROR: wrong format for switch -output-search-graph-extended file"));
				return false;
			}
			m_outputSearchGraph = true;
			m_outputSearchGraphExtended = true;
		} else
			m_outputSearchGraph = false;
		// #ifdef HAVE_PROTOBUF
		// if (m_parameter.getParam("output-search-graph-pb").size() > 0)
		// {
		// if (m_parameter.getParam("output-search-graph-pb").size() != 1) {
		// UserMessage.add(string("ERROR: wrong format for switch -output-search-graph-pb path"));
		// return false;
		// }
		// m_outputSearchGraphPB = true;
		// }
		// else
		// m_outputSearchGraphPB = false;
		// #endif

		// include feature names in the n-best list
		m_labeledNBestList = getBooleanParameter("labeled-n-best-list", true);

		// include word alignment in the n-best list
		m_nBestIncludesAlignment = getBooleanParameter("include-alignment-in-n-best", false);

		// printing source phrase spans
		m_reportSegmentation = getBooleanParameter("report-segmentation", false);

		// print all factors of output translations
		m_reportAllFactors = getBooleanParameter("report-all-factors", false);

		// print all factors of output translations
		m_reportAllFactorsNBest = getBooleanParameter("report-all-factors-in-n-best", false);

		// 
		if (m_inputType == InputTypeEnum.SentenceInput) {
			m_useTransOptCache = getBooleanParameter("use-persistent-cache", true);
			m_transOptCacheMaxSize = (m_parameter.getParam("persistent-cache-size").size() > 0) ? Integer
					.valueOf(m_parameter.getParam("persistent-cache-size").get(0))
					: TypeDef.DEFAULT_MAX_TRANS_OPT_CACHE_SIZE;
		} else {
			m_useTransOptCache = false;
		}

		// input factors
		final List<String> inputFactorVector = m_parameter.getParam("input-factors");
		m_inputFactorOrder = new int[inputFactorVector.size()];
		for (int i = 0; i < inputFactorVector.size(); i++) {
			m_inputFactorOrder[i] = Integer.valueOf(inputFactorVector.get(i));
		}
		if (m_inputFactorOrder == null) {
			UserMessage.add(("no input factor specified in config file"));
			return false;
		}

		// output factors
		final List<String> outputFactorVector = m_parameter.getParam("output-factors");

		m_outputFactorOrder = new int[outputFactorVector.size()];
		for (int i = 0; i < outputFactorVector.size(); i++) {
			m_outputFactorOrder[i] = (Integer.valueOf(outputFactorVector.get(i)));
		}
		if (m_outputFactorOrder.length == 0) { // default. output factor 0
			m_outputFactorOrder = new int[] { 0 };
		}

		// source word deletion
		m_wordDeletionEnabled = getBooleanParameter("phrase-drop-allowed", false);

		// Disable discarding
		m_disableDiscarding = getBooleanParameter("disable-discarding", false);

		// Print All Derivations
		m_printAllDerivations = getBooleanParameter("print-all-derivations", false);

		// additional output
		if (m_parameter.isParamSpecified("translation-details")) {
			final List<String> args = m_parameter.getParam("translation-details");
			if (args.size() == 1) {
				m_detailedTranslationReportingFilePath = args.get(0);
			} else {
				UserMessage
						.add(("the translation-details option requires exactly one filename argument"));
				return false;
			}
		}

		// score weights
		m_weightWordPenalty = Float.valueOf(m_parameter.getParam("weight-w").get(0));
		m_wpProducer = new WordPenaltyProducer(m_scoreIndexManager);
		m_allWeights.add(m_weightWordPenalty);

		m_weightUnknownWord = (m_parameter.getParam("weight-u").size() > 0) ? Float
				.valueOf(m_parameter.getParam("weight-u").get(0)) : 1;
		m_unknownWordPenaltyProducer = new UnknownWordPenaltyProducer(m_scoreIndexManager);
		m_allWeights.add(m_weightUnknownWord);

		// reordering constraints
		m_maxDistortion = (m_parameter.getParam("distortion-limit").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("distortion-limit").get(0)) : -1;
		m_reorderingConstraint = getBooleanParameter("monotone-at-punctuation", false);

		// settings for pruning
		m_maxHypoStackSize = (m_parameter.getParam("stack").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("stack").get(0)) : TypeDef.DEFAULT_MAX_HYPOSTACK_SIZE;
		m_minHypoStackDiversity = 0;
		if (m_parameter.getParam("stack-diversity").size() > 0) {
			if (m_maxDistortion > 15) {
				UserMessage
						.add("stack diversity > 0 is not allowed for distortion limits larger than 15");
				return false;
			}
			if (m_inputType == InputTypeEnum.WordLatticeInput) {
				UserMessage.add("stack diversity > 0 is not allowed for lattice input");
				return false;
			}
			m_minHypoStackDiversity = Integer.valueOf(m_parameter.getParam("stack-diversity")
					.get(0));
		}

		m_beamWidth = (m_parameter.getParam("beam-threshold").size() > 0) ? Util
				.transformScore(Float.valueOf(m_parameter.getParam("beam-threshold").get(0)))
				: Util.transformScore(TypeDef.DEFAULT_BEAM_WIDTH);
		m_earlyDiscardingThreshold = (m_parameter.getParam("early-discarding-threshold").size() > 0) ? Util
				.transformScore(Float.valueOf(m_parameter.getParam("early-discarding-threshold")
						.get(0)))
				: Util.transformScore(TypeDef.DEFAULT_EARLY_DISCARDING_THRESHOLD);
		m_translationOptionThreshold = (m_parameter.getParam("translation-option-threshold").size() > 0) ? Util
				.transformScore(Float.valueOf(m_parameter.getParam("translation-option-threshold")
						.get(0)))
				: Util.transformScore(TypeDef.DEFAULT_TRANSLATION_OPTION_THRESHOLD);

		m_maxNoTransOptPerCoverage = (m_parameter.getParam("max-trans-opt-per-coverage").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("max-trans-opt-per-coverage").get(0))
				: TypeDef.DEFAULT_MAX_TRANS_OPT_SIZE;

		m_maxNoPartTransOpt = (m_parameter.getParam("max-partial-trans-opt").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("max-partial-trans-opt").get(0))
				: TypeDef.DEFAULT_MAX_PART_TRANS_OPT_SIZE;

		m_maxPhraseLength = (m_parameter.getParam("max-phrase-length").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("max-phrase-length").get(0))
				: TypeDef.DEFAULT_MAX_PHRASE_LENGTH;

		m_cubePruningPopLimit = (m_parameter.getParam("cube-pruning-pop-limit").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("cube-pruning-pop-limit").get(0))
				: TypeDef.DEFAULT_CUBE_PRUNING_POP_LIMIT;

		m_cubePruningDiversity = (m_parameter.getParam("cube-pruning-diversity").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("cube-pruning-diversity").get(0))
				: TypeDef.DEFAULT_CUBE_PRUNING_DIVERSITY;

		// unknown word processing
		m_dropUnknown = getBooleanParameter("drop-unknown", false);

		// minimum Bayes risk decoding
		m_mbr = getBooleanParameter("minimum-bayes-risk", false);
		m_mbrSize = (m_parameter.getParam("mbr-size").size() > 0) ? Integer.valueOf(m_parameter
				.getParam("mbr-size").get(0)) : 200;
		m_mbrScale = (m_parameter.getParam("mbr-scale").size() > 0) ? Float.valueOf(m_parameter
				.getParam("mbr-scale").get(0)) : 1.0f;

		// lattice mbr
		m_useLatticeMBR = getBooleanParameter("lminimum-bayes-risk", false);
		if (m_useLatticeMBR && m_mbr) {
			System.err.println("Errror: Cannot use both n-best mbr and lattice mbr together");
			System.exit(1);
		}

		if (m_useLatticeMBR)
			m_mbr = true;

		m_lmbrPruning = (m_parameter.getParam("lmbr-pruning-factor").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("lmbr-pruning-factor").get(0)) : 30;
		m_lmbrThetas = Scan.toFloat(m_parameter.getParam("lmbr-thetas"));
		m_useLatticeHypSetForLatticeMBR = getBooleanParameter("lattice-hypo-set", false);
		m_lmbrPrecision = (m_parameter.getParam("lmbr-p").size() > 0) ? Float.valueOf(m_parameter
				.getParam("lmbr-p").get(0)) : 0.8f;
		m_lmbrPRatio = (m_parameter.getParam("lmbr-r").size() > 0) ? Float.valueOf(m_parameter
				.getParam("lmbr-r").get(0)) : 0.6f;
		m_lmbrMapWeight = (m_parameter.getParam("lmbr-map-weight").size() > 0) ? Float
				.valueOf(m_parameter.getParam("lmbr-map-weight").get(0)) : 0.0f;

		// consensus decoding
		m_useConsensusDecoding = getBooleanParameter("consensus-decoding", false);
		if (m_useConsensusDecoding && m_mbr) {
			System.err.println("Error: Cannot use consensus decoding together with mbr");
			System.exit(1);
		}
		if (m_useConsensusDecoding)
			m_mbr = true;

		m_timeout_threshold = (m_parameter.getParam("time-out").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("time-out").get(0)) : Integer.MAX_VALUE;  //c++ -1
		m_timeout = (getTimeoutThreshold() == -1) ? false : true;

		m_lmcache_cleanup_threshold = (m_parameter.getParam("clean-lm-cache").size() > 0) ? Integer
				.valueOf(m_parameter.getParam("clean-lm-cache").get(0)) : 1;

		// Read in constraint decoding file, if provided
		if (m_parameter.getParam("constraint").size() != 0) {
			if (m_parameter.getParam("search-algorithm").size() > 0
					&& Integer.valueOf(m_parameter.getParam("search-algorithm").get(0)) != 0) {
				System.err
						.println("Can use -constraint only with stack-based search (-search-algorithm 0)");
				System.exit(1);
			}
			m_constraintFileName = m_parameter.getParam("constraint").get(0);

			BufferedReader br = new BufferedReader(new FileReader(m_constraintFileName));

			String line;

			long sentenceID = -1;
			while ((line = br.readLine()) != null) {
				String vecStr[] = Util.tokenize(line, "\t");

				if (vecStr.length == 1) {
					sentenceID++;
					Phrase phrase = new Phrase(FactorDirection.Output);

					phrase
							.createFromString(getOutputFactorOrder(), vecStr[0],
									getFactorDelimiter());

					m_constraints.put(sentenceID, phrase);
				} else if (vecStr.length == 2) {
					sentenceID = Long.valueOf(vecStr[0]);
					Phrase phrase = new Phrase(FactorDirection.Output);
					phrase
							.createFromString(getOutputFactorOrder(), vecStr[1],
									getFactorDelimiter());
					m_constraints.put(sentenceID, phrase);
				} else {
					ASSERT.a(false);
				}
			}
		}

		// use of xml in input
		if (m_parameter.getParam("xml-input").size() == 0)
			m_xmlInputType = XmlInputType.XmlPassThrough;
		else if (m_parameter.getParam("xml-input").get(0) == "exclusive")
			m_xmlInputType = XmlInputType.XmlExclusive;
		else if (m_parameter.getParam("xml-input").get(0) == "inclusive")
			m_xmlInputType = XmlInputType.XmlInclusive;
		else if (m_parameter.getParam("xml-input").get(0) == "ignore")
			m_xmlInputType = XmlInputType.XmlIgnore;
		else if (m_parameter.getParam("xml-input").get(0) == "pass-through")
			m_xmlInputType = XmlInputType.XmlPassThrough;
		else {
			UserMessage
					.add("invalid xml-input value, must be pass-through, exclusive, inclusive, or ignore");
			return false;
		}

		if (!loadLexicalReorderingModel())
			return false;
		if (!loadLanguageModels())
			return false;
		if (!loadGenerationTables())
			return false;
		if (!loadPhraseTables())
			return false;
		if (!loadGlobalLexicalModel())
			return false;

		m_scoreIndexManager.initFeatureNames();
		if (m_parameter.getParam("weight-file").size() > 0) {
			UserMessage.add("ERROR: weight-file option is broken\n");
			System.exit(0);
			// if (m_parameter.getParam("weight-file").size() != 1) {
			// UserMessage.add(string("ERROR: weight-file takes a single parameter"));
			// return false;
			// }
			// string fnam = m_parameter.getParam("weight-file").get(0);
			// m_scoreIndexManager.InitWeightVectorFromFile(fnam, m_allWeights);
		}

		return true;
	}

	public final List<String> getParam(final String paramName) {
		return m_parameter.getParam(paramName);
	}

	public final int[] getInputFactorOrder() {
		return m_inputFactorOrder;
	}

	public final int[] getOutputFactorOrder() {
		return m_outputFactorOrder;
	}

	public List<DecodeGraph> getDecodeStepVL(final InputType source) {
		List<DecodeGraph> decodeGraphs = new ArrayList<DecodeGraph>();
		// mapping
		final List<String> mappingVector = m_parameter.getParam("mapping");
		final List<Integer> maxChartSpans = Scan.toInt(m_parameter.getParam("max-chart-span"));

		DecodeStep prev = null;
		int prevDecodeGraphInd = 0;
		for (int i = 0; i < mappingVector.size(); i++) {
			String[] token = Util.tokenize(mappingVector.get(i));
			int decodeGraphInd = 0;
			DecodeType decodeType = DecodeType.Translate;
			int index = 0;
			if (token.length == 2) {
				decodeGraphInd = 0;
				decodeType = token[0].equals("T") ? DecodeType.Translate : DecodeType.Generate;
				index = Integer.valueOf(token[1]);
			} else if (token.length == 3) { // For specifying multiple
				// translation model
				decodeGraphInd = Integer.valueOf(token[0]);
				// the vectorList index can only increment by one
				ASSERT.a(decodeGraphInd == prevDecodeGraphInd
						|| decodeGraphInd == prevDecodeGraphInd + 1);
				if (decodeGraphInd > prevDecodeGraphInd) {
					prev = null;
				}
				decodeType = token[1] == "T" ? DecodeType.Translate : DecodeType.Generate;
				index = Integer.valueOf(token[2]);
			} else {
				UserMessage.add("Malformed mapping!");
				assert (false);
			}

			DecodeStep decodeStep = null;
			switch (decodeType) {
			case Translate:
				if (index >= m_phraseDictionary.size()) {
					StringBuilder strme = new StringBuilder();
					strme.append("No phrase dictionary with index ").append(index).append(
							" available!");
					UserMessage.add(strme.toString());
					ASSERT.a(false);
				}
				decodeStep = new DecodeStepTranslation(m_phraseDictionary.get(index).getDictionary(
						source), prev);
				break;
			case Generate:
				if (index >= m_generationDictionary.size()) {
					StringBuilder strme = new StringBuilder();
					strme.append("No generation dictionary with index ").append(index).append(
							" available!");
					UserMessage.add(strme.toString());
					ASSERT.a(false);
				}
				decodeStep = new DecodeStepGeneration(m_generationDictionary.get(index), prev);
				break;
			case InsertNullFertilityWord:
				ASSERT.a("Please implement NullFertilityInsertion." != null);
				break;
			}

			ASSERT.a(decodeStep != null);
			if (decodeGraphs.size() < decodeGraphInd + 1) {
				DecodeGraph decodeGraph = null;
				if (m_searchAlgorithm == SearchAlgorithm.ChartDecoding) {
					int maxChartSpan = (decodeGraphInd < maxChartSpans.size()) ? maxChartSpans
							.get(decodeGraphInd) : TypeDef.DEFAULT_MAX_CHART_SPAN;
					decodeGraph = new DecodeGraph(decodeGraphs.size(), maxChartSpan);
				} else {
					decodeGraph = new DecodeGraph(decodeGraphs.size());
				}

				decodeGraphs.add(decodeGraph); // TODO max chart span
			}

			decodeGraphs.get(decodeGraphInd).add(decodeStep);
			prev = decodeStep;
			prevDecodeGraphInd = decodeGraphInd;
		}

		return decodeGraphs;
	}

	public boolean getSourceStartPosMattersForRecombination() {
		return m_sourceStartPosMattersForRecombination;
	}

	public boolean getDropUnknown() {
		return m_dropUnknown;
	}

	public boolean getDisableDiscarding() {
		return m_disableDiscarding;
	}

	public int getMaxNoTransOptPerCoverage() {
		return m_maxNoTransOptPerCoverage;
	}

	public int getMaxNoPartTransOpt() {
		return m_maxNoPartTransOpt;
	}

	public final Phrase getConstrainingPhrase(long sentenceID) {
		Phrase phrase = m_constraints.get(sentenceID);
		if (phrase != null) {
			return phrase;
		} else {
			return null;
		}
	}

	public int getMaxPhraseLength() {
		return m_maxPhraseLength;
	}

	public final List<LexicalReordering> getReorderModels() {
		return m_reorderModels;
	}

	public float getWeightDistortion() {
		return m_weightDistortion;
	}

	public float getWeightWordPenalty() {
		return m_weightWordPenalty;
	}

	public float getWeightUnknownWord() {
		return m_weightUnknownWord;
	}

	public boolean isWordDeletionEnabled() {
		return m_wordDeletionEnabled;
	}

	public int getMaxHypoStackSize() {
		return m_maxHypoStackSize;
	}

	public int getMinHypoStackDiversity() {
		return m_minHypoStackDiversity;
	}

	public int getCubePruningPopLimit() {
		return m_cubePruningPopLimit;
	}

	public int getCubePruningDiversity() {
		return m_cubePruningDiversity;
	}

	public int isPathRecoveryEnabled() {
		return m_recoverPath ? 1 : 0;
	}

	public int getMaxDistortion() {
		return m_maxDistortion;
	}

	public boolean UseReorderingConstraint() {
		return m_reorderingConstraint;
	}

	public float getBeamWidth() {
		return m_beamWidth;
	}

	public float getEarlyDiscardingThreshold() {
		return m_earlyDiscardingThreshold;
	}

	public boolean useEarlyDiscarding() {
		return m_earlyDiscardingThreshold != Float.NEGATIVE_INFINITY;

	}

	public float getTranslationOptionThreshold() {
		return m_translationOptionThreshold;
	}

	// ! returns the total number of score components across all types, all
	// factors
	public int getTotalScoreComponents() {
		return m_scoreIndexManager.getTotalNumberOfScores();
	}

	public final ScoreIndexManager getScoreIndexManager() {
		return m_scoreIndexManager;
	}

	public int getLMSize() {
		return m_languageModel.size();
	}

	public final LMList getAllLM() {
		return m_languageModel;
	}

	public int getPhraseDictionarySize() {
		return m_phraseDictionary.size();
	}

	public final List<PhraseDictionaryFeature> getPhraseDictionaries() {
		return m_phraseDictionary;
	}

	public final List<GenerationDictionary> getGenerationDictionaries() {
		return m_generationDictionary;
	}

	public int getGenerationDictionarySize() {
		return m_generationDictionary.size();
	}

	public int getVerboseLevel() {
		return m_verboseLevel;
	}

	public void setVerboseLevel(int x) {
		m_verboseLevel = x;
	}

	public boolean getReportSegmentation() {
		return m_reportSegmentation;
	}

	public boolean getReportAllFactors() {
		return m_reportAllFactors;
	}

	public boolean getReportAllFactorsNBest() {
		return m_reportAllFactorsNBest;
	}

	public boolean isDetailedTranslationReportingEnabled() {
		return !m_detailedTranslationReportingFilePath.isEmpty();
	}

	public final String getDetailedTranslationReportingFilePath() {
		return m_detailedTranslationReportingFilePath;
	}

	public boolean isLabeledNBestList() {
		return m_labeledNBestList;
	}

	public boolean nBestIncludesAlignment() {
		return m_nBestIncludesAlignment;
	}

	public int getNumLinkParams() {
		return m_numLinkParams;
	}

	public final List<String> getDescription() {
		return m_parameter.getParam("description");
	}

	// for mert
	public int getNBestSize() {
		return m_nBestSize;
	}

	public final String getNBestFilePath() {
		return m_nBestFilePath;
	}

	public boolean isNBestEnabled() {
		return (!m_nBestFilePath.isEmpty()) || m_mbr || m_useLatticeMBR || m_outputSearchGraph
				|| m_useConsensusDecoding
		// #ifdef HAVE_PROTOBUF
		// || m_outputSearchGraphPB
		// #endif
		;
	}

	public int getNBestFactor() {
		return m_nBestFactor;
	}

	public boolean getOutputWordGraph() {
		return m_outputWordGraph;
	}

	// ! Sets the global score vector weights for a given ScoreProducer.
	public void setWeightsForScoreProducer(final ScoreProducer sp, final List<Float> weights) {
		final int id = sp.getScoreBookkeepingID();

		final int begin = m_scoreIndexManager.getBeginIndex(id);
		final int end = m_scoreIndexManager.getEndIndex(id);
		ASSERT.a(end - begin == weights.size());
		// if (m_allWeights.size() < end)
		// m_allWeights.resize(end);

		for (int i = 0; i < weights.size(); i++) {
			m_allWeights.set(i, weights.get(i));
		}

	}

	public InputTypeEnum getInputType() {
		return m_inputType;
	}

	public SearchAlgorithm getSearchAlgorithm() {
		return m_searchAlgorithm;
	}

	public int getNumInputScores() {
		return m_numInputScores;
	}

	public void initializeBeforeSentenceProcessing(InputType in) {
		for (int i = 0; i < m_reorderModels.size(); ++i) {
			m_reorderModels.get(i).initializeForInput(in);
		}
		for (int i = 0; i < m_globalLexicalModels.size(); ++i) {
			m_globalLexicalModels.get(i).initializeForInput((Sentence) in);
		}
		// something LMs could do before translating a sentence

		for (LanguageModel languageModel : m_languageModel) {

			languageModel.initializeBeforeSentenceProcessing();
		}
	}

	public void cleanUpAfterSentenceProcessing() {
		for (int i = 0; i < m_phraseDictionary.size(); ++i) {
			PhraseDictionaryFeature phraseDictionaryFeature = m_phraseDictionary.get(i);
			PhraseDictionary phraseDictionary = phraseDictionaryFeature.getDictionary();
			phraseDictionary.cleanUp();

		}

		for (int i = 0; i < m_generationDictionary.size(); ++i)
			m_generationDictionary.get(i).cleanUp();

		// something LMs could do after each sentence
		for (LanguageModel languageModel : m_languageModel) {
			languageModel.cleanUpAfterSentenceProcessing();
		}

	}

	public final List<Float> getAllWeights() {
		return m_allWeights;
	}

	public final DistortionScoreProducer getDistortionScoreProducer() {
		return m_distortionScoreProducer;
	}

	public final WordPenaltyProducer getWordPenaltyProducer() {
		return m_wpProducer;
	}

	public final UnknownWordPenaltyProducer getUnknownWordPenaltyProducer() {
		return m_unknownWordPenaltyProducer;
	}

	public boolean useAlignmentInfo() {
		return m_UseAlignmentInfo;
	}

	public void useAlignmentInfo(boolean a) {
		m_UseAlignmentInfo = a;
	};

	public boolean printAlignmentInfo() {
		return m_PrintAlignmentInfo;
	}

	public boolean printAlignmentInfoInNbest() {
		return m_PrintAlignmentInfoNbest;
	}

	public boolean getDistinctNBest() {
		return m_onlyDistinctNBest;
	}

	public final String getFactorDelimiter() {
		return m_factorDelimiter;
	}

	public int getMaxNumFactors(FactorDirection direction) {
		return m_maxFactorIdx[direction.ordinal()] + 1;
	}

	public int getMaxNumFactors() {
		return m_maxNumFactors;
	}

	public boolean useMBR() {
		return m_mbr;
	}

	public boolean useLatticeMBR() {
		return m_useLatticeMBR;
	}

	public boolean useConsensusDecoding() {
		return m_useConsensusDecoding;
	}

	public void setUseLatticeMBR(boolean flag) {
		m_useLatticeMBR = flag;
	}

	public int getMBRSize() {
		return m_mbrSize;
	}

	public float getMBRScale() {
		return m_mbrScale;
	}

	public void setMBRScale(float scale) {
		m_mbrScale = scale;
	}

	public int getLatticeMBRPruningFactor() {
		return m_lmbrPruning;
	}

	public void setLatticeMBRPruningFactor(int prune) {
		m_lmbrPruning = prune;
	}

	public final List<Float> getLatticeMBRThetas() {
		return m_lmbrThetas;
	}

	public boolean useLatticeHypSetForLatticeMBR() {
		return m_useLatticeHypSetForLatticeMBR;
	}

	public float getLatticeMBRPrecision() {
		return m_lmbrPrecision;
	}

	public void setLatticeMBRPrecision(float p) {
		m_lmbrPrecision = p;
	}

	public float getLatticeMBRPRatio() {
		return m_lmbrPRatio;
	}

	public void setLatticeMBRPRatio(float r) {
		m_lmbrPRatio = r;
	}

	public float getLatticeMBRMapWeight() {
		return m_lmbrMapWeight;
	}

	public boolean UseTimeout() {
		return m_timeout;
	}

	public int getTimeoutThreshold() {
		return m_timeout_threshold;
	}

	public int getLMCacheCleanupThreshold() {
		return m_lmcache_cleanup_threshold;
	}

	public boolean getOutputSearchGraph() {
		return m_outputSearchGraph;
	}

	public void setOutputSearchGraph(boolean outputSearchGraph) {
		m_outputSearchGraph = outputSearchGraph;
	}

	public boolean getOutputSearchGraphExtended() {
		return m_outputSearchGraphExtended;
	}

	// #ifdef HAVE_PROTOBUF
	// boolean getOutputSearchGraphPB() { return m_outputSearchGraphPB; }
	// #endif

	public XmlInputType getXmlInputType() {
		return m_xmlInputType;
	}

	public boolean getUseTransOptCache() {
		return m_useTransOptCache;
	}

	public void addTransOptListToCache(final DecodeGraph decodeGraph, final Phrase sourcePhrase,
			final TranslationOptionList transOptList) {
		Pair<Integer, Phrase> key = new Pair<Integer, Phrase>(decodeGraph.getPosition(),
				sourcePhrase);
		TranslationOptionList storedTransOptList = new TranslationOptionList(transOptList);
		// #ifdef WITH_THREADS
		// boost::mutex::scoped_lock lock(m_transOptCacheMutex);
		// #endif
		m_transOptCache.put(key, new Pair<TranslationOptionList, Long>(storedTransOptList, System
				.currentTimeMillis()));
		reduceTransOptCache();
	}

	public final TranslationOptionList findTransOptListInCache(final DecodeGraph decodeGraph,
			final Phrase sourcePhrase) {
		Pair<Integer, Phrase> key = new Pair<Integer, Phrase>(decodeGraph.getPosition(),
				sourcePhrase);
		// #ifdef WITH_THREADS
		// boost::mutex::scoped_lock lock(m_transOptCacheMutex);
		// #endif

		Pair<TranslationOptionList, Long> v = m_transOptCache.get(key);
		if (v == null) {
			return null;
		}
		v.second = System.currentTimeMillis();

		return v.first;

	}

	public boolean printAllDerivations() {
		return m_printAllDerivations;
	}

	public final UnknownLHSList getUnknownLHS() {
		return m_unknownLHS;
	}

	public final Word getInputDefaultNonTerminal() {
		return m_inputDefaultNonTerminal;
	}

	public final Word getOutputDefaultNonTerminal() {
		return m_outputDefaultNonTerminal;
	}

	public SourceLabelOverlap getSourceLabelOverlap() {
		return m_sourceLabelOverlap;
	}

	public boolean getOutputHypoScore() {
		return m_outputHypoScore;
	}

	public int getRuleLimit() {
		return m_ruleLimit;
	}

	public float getRuleCountThreshold() {
		return 999999; /* TODO wtf! */
	}

	public boolean continuePartialTranslation() {
		return m_continuePartialTranslation;
	}

}
