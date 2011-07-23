package yonee.moses4j.moses;

import java.util.List;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.ASSERT;

/**
 * 
 * @author YONEE
 * @OK<<
 * @RISK
 *       typedef const Factor * FactorArray[MAX_NUM_FACTORS];
 */
public class Word {

	// typedef const Factor * FactorArray[MAX_NUM_FACTORS];

	protected Factor m_factorArray[] = new Factor[TypeDef.MAX_NUM_FACTORS];

	/** < set of factors */
	protected boolean m_isNonTerminal;

	/** deep copy */
	public Word(final Word copy) {
		m_isNonTerminal = copy.m_isNonTerminal;
		System.arraycopy(copy.m_factorArray, 0, m_factorArray, 0, copy.m_factorArray.length);
	}

	/** empty word */
	public Word() {
		this(false);
	}

	public Word(boolean isNonTerminal) {
		m_isNonTerminal = isNonTerminal;
	}

	// ! returns Factor pointer for particular FactorType
	public final Factor get(int index) {
		return m_factorArray[index];
	}

	public final void set(int index, Factor factor) {
		m_factorArray[index] = factor;
	}

	// ! Deprecated. should use operator[]
	public final Factor getFactor(int factorType) {
		return m_factorArray[factorType];
	}

	public final void setFactor(int factorType, final Factor factor) {
		m_factorArray[factorType] = factor;
	}

	public final boolean isNonTerminal() {
		return m_isNonTerminal;
	}

	public final void setIsNonTerminal(boolean val) {
		m_isNonTerminal = val;
	}

	/**
	 * add the factors from sourceWord into this representation,
	 * NULL elements in sourceWord will be skipped
	 */
	public void merge(final Word sourceWord) {
		for (int currFactor = 0; currFactor < TypeDef.MAX_NUM_FACTORS; currFactor++) {
			final Factor sourcefactor = sourceWord.m_factorArray[currFactor], targetFactor = this.m_factorArray[currFactor];
			if (targetFactor == null && sourcefactor != null) {
				m_factorArray[currFactor] = sourcefactor;
			}
		}
	}

	/**
	 * get string representation of list of factors. Used by PDTimp so supposed
	 * to be invariant to changes in format of debuggin output, therefore, doesn't
	 * use streaming output or ToString() from any class so not dependant on
	 * these debugging functions.
	 */
	public String getString(final int[] factorType, boolean endWithBlank) {
		StringBuilder strme = new StringBuilder();
		ASSERT.a(factorType != null);
		ASSERT.a(factorType.length <= TypeDef.MAX_NUM_FACTORS);
		final String factorDelimiter = StaticData.instance().getFactorDelimiter();
		boolean firstPass = true;
		for (int i = 0; i < factorType.length; i++) {
			final Factor factor = m_factorArray[factorType[i]];
			if (factor != null) {
				if (firstPass) {
					firstPass = false;
				} else {
					strme.append(factorDelimiter);
				}
				strme.append(factor.getString());
			}
		}
		if (endWithBlank)
			strme.append(" ");
		return strme.toString();
	}

	// TO_STRING();
	public String toString() {
		StringBuilder strme = new StringBuilder();
		final String factorDelimiter = StaticData.instance().getFactorDelimiter();
		boolean firstPass = true;
		for (int currFactor = 0; currFactor < TypeDef.MAX_NUM_FACTORS; currFactor++) {
			int factorType = (currFactor);
			final Factor factor = this.getFactor(factorType);
			if (factor != null) {
				if (firstPass) {
					firstPass = false;
				} else {
					strme.append(factorDelimiter);
				}
				strme.append(factor);
			}
		}
		strme.append(" ");
		return strme.toString();
	}

	// ! transitive comparison of Word objects
	public final boolean less(final Word compare) { // needed to store word in GenerationDictionary
		// map
		// uses comparison of FactorKey
		// 'proper' comparison, not address/id comparison
		return compare(this, compare) < 0;
	}

	@Override
	public int hashCode() {
		return 1996770; //this is a funny number
	}

	@Override
	public boolean equals(Object compare) { // needed to store word in
		// GenerationDictionary map
		// uses comparison of FactorKey
		// 'proper' comparison, not address/id comparison
		return compare(this, (Word) compare) == 0;
	}

	/* static functions */

	/**
	 * transitive comparison of 2 word objects. Used by operator<.
	 * Only compare the co-joined factors, ie. where factor exists for both words.
	 * Should make it non-static
	 */
	public static int compare(final Word targetWord, final Word sourceWord) {
		if (targetWord.isNonTerminal() != sourceWord.isNonTerminal()) {
			return targetWord.isNonTerminal() ? -1 : 1;
		}

		for (int factorType = 0; factorType < TypeDef.MAX_NUM_FACTORS; factorType++) {
			final Factor targetFactor = targetWord.get(factorType), sourceFactor = sourceWord
					.get(factorType);

			if (targetFactor == null || sourceFactor == null)
				continue;
			if (targetFactor.equals(sourceFactor))
				continue;

			return targetFactor.less(sourceFactor) ? -1 : +1;
		}
		return 0;
	}

	public void createFromString(FactorDirection direction, final List<Integer> factorOrder,
			final String str, boolean isNonTerminal) {
		FactorCollection factorCollection = FactorCollection.instance();

		String[] wordVec = Util.tokenize(str, "|");
		assert (wordVec.length == factorOrder.size());

		for (int ind = 0; ind < wordVec.length; ++ind) {
			int factorType = factorOrder.get(ind);
			Factor factor = factorCollection.addFactor(direction, factorType, wordVec[ind]);
			m_factorArray[factorType] = factor;
		}

		// assume term/non-term same for all factors
		m_isNonTerminal = isNonTerminal;
	}

	public void createUnknownWord(final Word sourceWord) {
		FactorCollection factorCollection = FactorCollection.instance();

		for (int currFactor = 0; currFactor < TypeDef.MAX_NUM_FACTORS; currFactor++) {
			int factorType = currFactor;

			final Factor sourceFactor = sourceWord.get(currFactor);
			if (sourceFactor == null)
				setFactor(factorType, factorCollection.addFactor(FactorDirection.Output,
						factorType, TypeDef.UNKNOWN_FACTOR));
			else
				setFactor(factorType, factorCollection.addFactor(FactorDirection.Output,
						factorType, sourceFactor.getString()));
		}
		m_isNonTerminal = sourceWord.isNonTerminal();
	}

}
