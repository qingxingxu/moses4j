package yonee.moses4j.moses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.moses4j.moses.TypeDef.PhraseTableImplementation;
import yonee.utils.ASSERT;
import yonee.utils.VERBOSE;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class PhraseDictionaryFeature extends StatelessFeatureFunction {

	// ////////////////////

	private int m_numScoreComponent;
	private int m_numInputScores;
	private int[] m_input;
	private int[] m_output;
	private String m_filePath;
	private List<Float> m_weight = new ArrayList<Float>();
	private int m_tableLimit;
	// We instantiate either the the thread-safe or non-thread-safe dictionary,
	// but not both. The thread-safe one can be instantiated in the constructor
	// and shared
	// between threads, however the non-thread-safe one (eg
	// PhraseDictionaryTree) must be instantiated
	// on demand, and stored in thread-specific storage.
	private PhraseDictionary m_phraseDictionary ;
	// #ifdef WITH_THREADS
	// boost::thread_specific_ptr<PhraseDictionary>
	// m_threadUnsafePhraseDictionary;
	// #else
	// private PhraseDictionary m_threadUnsafePhraseDictionary;
	// #endif

	// private boolean m_useThreadSafePhraseDictionary;
	private PhraseTableImplementation m_implementation;
//	private String m_targetFile;
//	private String m_alignmentsFile;

	/** Load the appropriate phrase table */
	private PhraseDictionary loadPhraseTable() {
		final StaticData staticData = StaticData.instance();
		if (m_implementation == PhraseTableImplementation.Memory) { // memory
			// phrase
			// table
			VERBOSE.v(2, "using standard phrase tables\n");
			if (!Util.fileExists(m_filePath)
					&& Util.fileExists(m_filePath + ".gz")) {
				m_filePath += ".gz";
				VERBOSE.v(2, "Using gzipped file\n");
			}
			if (staticData.getInputType() != InputTypeEnum.SentenceInput) {
				UserMessage
						.add("Must use binary phrase table for this input type");
				assert (false);
			}

			PhraseDictionaryMemory pdm = new PhraseDictionaryMemory(
					m_numScoreComponent, this);
			try {
				ASSERT.a(pdm.load(m_input, m_output, m_filePath, m_weight,
						m_tableLimit, staticData.getAllLM(), staticData
								.getWeightWordPenalty()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return pdm;
		}
		return null;
		// else if (m_implementation == Binary)
		// {
		// const StaticData& staticData = StaticData::Instance();
		// PhraseDictionaryTreeAdaptor* pdta = new
		// PhraseDictionaryTreeAdaptor(m_numScoreComponent,
		// m_numInputScores,this);
		// assert(pdta->Load(
		// m_input
		// , m_output
		// , m_filePath
		// , m_weight
		// , m_tableLimit
		// , staticData.GetAllLM()
		// , staticData.GetWeightWordPenalty()));
		// return pdta;
		// }
		// else if (m_implementation == SCFG)
		// { // memory phrase table
		// VERBOSE(2,"using New Format phrase tables" << std::endl);
		// if (!FileExists(m_filePath) && FileExists(m_filePath + ".gz")) {
		// m_filePath += ".gz";
		// VERBOSE(2,"Using gzipped file" << std::endl);
		// }
		//						
		// PhraseDictionarySCFG* pdm = new
		// PhraseDictionarySCFG(m_numScoreComponent,this);
		// assert(pdm->Load(m_input
		// , m_output
		// , m_filePath
		// , m_weight
		// , m_tableLimit
		// , staticData.GetAllLM()
		// , staticData.GetWeightWordPenalty()));
		// return pdm;
		// }
		// else if (m_implementation == OnDisk)
		// {
		//						
		// PhraseDictionaryOnDisk* pdta = new
		// PhraseDictionaryOnDisk(m_numScoreComponent, this);
		// pdta->Load(
		// m_input
		// , m_output
		// , m_filePath
		// , m_weight
		// , m_tableLimit);
		// //, staticData.GetAllLM()
		// //, staticData.GetWeightWordPenalty()));
		// assert(pdta);
		// return pdta;
		// }
		// else if (m_implementation == Binary)
		// {
		// PhraseDictionaryTreeAdaptor* pdta = new
		// PhraseDictionaryTreeAdaptor(m_numScoreComponent,
		// m_numInputScores,this);
		// assert(pdta->Load(
		// m_input
		// , m_output
		// , m_filePath
		// , m_weight
		// , m_tableLimit
		// , staticData.GetAllLM()
		// , staticData.GetWeightWordPenalty()));
		// return pdta;
		// }
		// else if (m_implementation == SuffixArray)
		// {
		// #ifndef WIN32
		// PhraseDictionaryDynSuffixArray *pd = new
		// PhraseDictionaryDynSuffixArray(m_numScoreComponent, this);
		// if(!(pd->Load(
		// m_input
		// ,m_output
		// ,m_filePath
		// ,m_targetFile
		// , m_alignmentsFile
		// , m_weight, m_tableLimit
		// , staticData.GetAllLM()
		// , staticData.GetWeightWordPenalty())))
		// {
		// std::cerr << "FAILED TO LOAD\n" << endl;
		// delete pd;
		// pd = NULL;
		// }
		// std::cerr << "Suffix array phrase table loaded" << std::endl;
		// return pd;
		// #else
		// assert(false);
		// #endif
		// }
	}

	public PhraseDictionaryFeature(PhraseTableImplementation implementation,
			int numScoreComponent, int numInputScores, final int[] input,
			final int[] output, final String filePath,
			final List<Float> weight, int tableLimit, final String targetFile,
			final String alignmentsFile) {
		m_numScoreComponent = numScoreComponent;
		m_numInputScores = numInputScores;
		m_input = input;
		m_output = output;
		m_filePath = filePath;
		m_weight = weight;
		m_tableLimit = tableLimit;
		m_implementation = implementation;
//		m_targetFile = targetFile;
//		m_alignmentsFile = alignmentsFile;

		final StaticData staticData = StaticData.instance();
		staticData.getScoreIndexManager().addScoreProducer(this);
		// Thread-safe phrase dictionaries get loaded now
		if (implementation == PhraseTableImplementation.Memory
				|| implementation == PhraseTableImplementation.SCFG
				|| implementation == PhraseTableImplementation.OnDisk
				|| implementation == PhraseTableImplementation.SuffixArray) {
			m_phraseDictionary = loadPhraseTable();
		}
	}

	public boolean computeValueInTranslationOption() {
		return true;
	}

	public String getScoreProducerDescription() {
		return "PhraseModel";
	}

	public String getScoreProducerWeightShortName() {
		return "tm";
	}

	public int getNumScoreComponents() {
		return m_numScoreComponent;
	}

	public int getNumInputScores() {
		return m_numInputScores;
	}

	public PhraseDictionary getDictionary(InputType source) {
		PhraseDictionary dict = getDictionary();
		dict.initializeForInput(source);
		return dict;
	}

	// TODO - get rid of this, make Cleanup() const. only to be called by static
	// data
	public PhraseDictionary getDictionary() {
		assert (m_phraseDictionary != null);
		return m_phraseDictionary;
	}

}
