package yonee.moses4j.moses;
/**
 * 
 * @author YONEE
 * @OK
 */
public class TypeDef {
	public final static int CLOCKS_PER_SEC = 1000;
	
	public final static String PROJECT_NAME = "moses";
	// Beginning of sentence symbol
	public final static String BOS_ = "<s>";
	// End of sentence symbol
	public final static String EOS_ = "</s>";

	public final static String UNKNOWN_FACTOR = "UNK";
	public final static String EPSILON = "*EPS*";

	public final static int NOT_FOUND = Integer.MAX_VALUE;
	public final static int MAX_NGRAM_SIZE = 20;

	public final static int DEFAULT_CUBE_PRUNING_POP_LIMIT = 1000;
	public final static int DEFAULT_CUBE_PRUNING_DIVERSITY = 0;
	public final static int DEFAULT_MAX_HYPOSTACK_SIZE = 200;
	public final static int DEFAULT_MAX_TRANS_OPT_CACHE_SIZE = 10000;
	public final static int DEFAULT_MAX_TRANS_OPT_SIZE = 5000;
	public final static int DEFAULT_MAX_PART_TRANS_OPT_SIZE = 10000;
	public final static int DEFAULT_MAX_PHRASE_LENGTH = 20;
	public final static int DEFAULT_MAX_CHART_SPAN = 10;
	public final static int ARRAY_SIZE_INCR = 10; // amount by which a phrase
	// gets resized when
	// necessary
	public final static float LOWEST_SCORE = -100.0f;
	public final static float DEFAULT_BEAM_WIDTH = 0.00001f;
	public final static float DEFAULT_EARLY_DISCARDING_THRESHOLD = 0.0f;
	public final static float DEFAULT_TRANSLATION_OPTION_THRESHOLD = 0.0f;
	public final static int DEFAULT_VERBOSE_LEVEL = 1;

	public final static int NUM_LANGUAGES = 2;

	public final static int MAX_NUM_FACTORS = 4;

	public enum FactorDirection {
		Input, Output

	};

	public enum DecodeType {
		Translate, Generate, InsertNullFertilityWord
		// ! an optional step that attempts to insert a few closed-class words
		// to improve LM scores
	};

	public enum LexReorderType // explain values
	{
		Backward, Forward, Bidirectional, Fe, F
	};

	public enum DistortionOrientationOptions {
		Monotone, // distinguish only between monotone and non-monotone as
		// possible orientations
		Msd
		// further separate non-monotone into swapped and discontinuous
	};

	public enum LMType {
		SingleFactor, MultiFactor
	};

	public enum LMImplementation {
		SRI, IRST, Skip, Joint, Internal, RandLM, Remote, ParallelBackoff;

	};

	public enum PhraseTableImplementation {
		Memory, Binary, OnDisk, GlueRule, Joshua, MemorySourceLabel, SCFG, BerkeleyDb, SuffixArray;

	};

	public enum InputTypeEnum {
		SentenceInput, ConfusionNetworkInput, WordLatticeInput, TreeInputType
	};

	public enum XmlInputType {
		XmlPassThrough, XmlIgnore, XmlExclusive, XmlInclusive
	};

	public enum DictionaryFind {
		Best, All
	};

	public enum SearchAlgorithm {
		Normal, CubePruning, CubeGrowing, ChartDecoding
	}

	public enum SourceLabelOverlap {
		SourceLabelOverlapAdd, SourceLabelOverlapReplace, SourceLabelOverlapDiscard

	};

}
