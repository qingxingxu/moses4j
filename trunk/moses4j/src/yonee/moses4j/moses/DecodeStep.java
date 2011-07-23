package yonee.moses4j.moses;


import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       bitset²Ù×÷
 */
public abstract class DecodeStep {

	protected Dictionary m_ptr; // ! pointer to translation/generation table
	protected FactorMask m_outputFactors = new FactorMask(); // ! mask of what factors exist on the
	// output side after this decode
	// step
	protected int[] m_conflictFactors;// = new ArrayList<Integer>(); // ! list of the factors
	// that may conflict
	// during this step
	protected int[] m_newOutputFactors;// = new ArrayList<Integer>(); // ! list of the factors

	// that are new in this
	// step, may be empty

	public DecodeStep(final Dictionary ptr, final DecodeStep prev) {
		m_ptr = ptr;
		FactorMask prevOutputFactors = new FactorMask();
		if (prev != null)
			prevOutputFactors = prev.m_outputFactors;

		FactorMask conflictMask = new FactorMask(m_outputFactors);
		conflictMask.and(ptr.getOutputFactorMask());
		m_outputFactors = prevOutputFactors;
		m_outputFactors.and(ptr.getOutputFactorMask());
		m_outputFactors.or(ptr.getOutputFactorMask());
		FactorMask newOutputFactorMask = new FactorMask(m_outputFactors);
		newOutputFactorMask.xor(prevOutputFactors);

		m_newOutputFactors = new int[newOutputFactorMask.size()];
		m_conflictFactors = new int[conflictMask.size()];
		//CollectionUtils.resize(m_newOutputFactors, newOutputFactorMask.size());
		//CollectionUtils.resize(m_conflictFactors, conflictMask.size());

		int j = 0, k = 0;
		for (int i = 0; i < TypeDef.MAX_NUM_FACTORS; i++) {
			if (newOutputFactorMask.get(i))
				m_newOutputFactors[j++] =  i;
			if (conflictMask.get(i))
				m_conflictFactors[k++] =  i;
		}
		VERBOSE.v(2, "DecodeStep():\n\toutputFactors=" + m_outputFactors + "\n\tconflictFactors="
				+ conflictMask + "\n\tnewOutputFactors=" + newOutputFactorMask + "\n");
	}

	public void finalize() throws Throwable {
		super.finalize();
	}

	// ! mask of factors that are present after this decode step
	public final FactorMask getOutputFactorMask() {
		return m_outputFactors;
	}

	// ! returns true if this decode step must match some pre-existing factors
	public final boolean isFilteringStep() {
		return m_conflictFactors != null;
	}

	// ! returns true if this decode step produces one or more new factors
	public final boolean isFactorProducingStep() {
		return m_newOutputFactors != null;
	}

	/*
	 * ! returns a list (possibly empty) of the (target side) factors that
	 * are produced in this decoding step. For example, if a previous step
	 * generated factor 1, and this step generates 1,2, then only 2 will be
	 * in the returned vector.
	 */
	public final int[] getNewOutputFactors() {
		return m_newOutputFactors;
	}

	/*
	 * ! returns a list (possibly empty) of the (target side) factors that
	 * are produced BUT ALREADY EXIST and therefore must be checked for
	 * conflict or compatibility
	 */
	public final int[] getConflictFactors() {
		return m_conflictFactors;
	}

	/* ! returns phrase table (dictionary) for translation step */
	public final PhraseDictionary getPhraseDictionary() {
		return (PhraseDictionary) (m_ptr);
	}

	/* ! returns generation table (dictionary) for generation step */
	public GenerationDictionary getGenerationDictionary() {
		return (GenerationDictionary) m_ptr;
	}

	/* ! returns dictionary in abstract class */
	public final Dictionary getDictionaryPtr() {
		return m_ptr;
	}

	/*
	 * ! Given an input TranslationOption, extend it in some way (put results in
	 * outputPartialTranslOptColl)
	 */
	public abstract void process(final TranslationOption inputPartialTranslOpt,
			final DecodeStep decodeStep, PartialTranslOptColl outputPartialTranslOptColl,
			TranslationOptionCollection toc, boolean adhereTableLimit);
}
