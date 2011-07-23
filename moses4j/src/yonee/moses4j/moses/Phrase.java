package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.ASSERT;
import yonee.utils.TRACE;

/**
 * 
 * @author YONEE
 * @OK
 */
public class Phrase {
	// friend std::ostream& operator<<(std::ostream&, const Phrase&);

	private FactorDirection m_direction = FactorDirection.Input;
	/**
	 * Reusing Direction enum to really mean which language
	 * Input = Source, Output = Target.
	 * Not really used, but nice to know for debugging purposes
	 */
	private List<Word> m_words = new ArrayList<Word>();
	private int m_arity;

	/** No longer does anything as not using mem pool for Phrase class anymore */
	public static void initializeMemPool() {

	}

	public static void finalizeMemPool() {

	}

	/** copy constructor */
	public Phrase(final Phrase copy) {
		m_direction = copy.m_direction;
		m_words = copy.m_words;
	}

	// 赋值构造，同上
	// Phrase& Phrase::operator=(const Phrase& x)
	public void set(final Phrase x) {
		if (x != this) {
			m_direction = x.m_direction;
			m_words = x.m_words;
		}
	}

	/**
	 * create empty phrase
	 * \param direction = language (Input = Source, Output = Target)
	 */
	public Phrase(FactorDirection direction) {
		this(direction, TypeDef.ARRAY_SIZE_INCR);
	}

	public Phrase(FactorDirection direction, int reserveSize) {
		// m_words = new ArrayList<Word>(reserveSize); 
		// m_words.reserve(reserveSize);
	}

	/** create phrase from vectors of words */
	public Phrase(FactorDirection direction, final Word[] mergeWords) {
		m_direction = direction;
		for (int currPos = 0; currPos < mergeWords.length; currPos++) {
			addWord(mergeWords[currPos]);
		}
	}

	/** destructor */
	// public virtual ~Phrase();

	/**
	 * parse a string from phrase table or sentence input and create a 2D vector of strings
	 * \param phraseString string to parse
	 * \param factorOrder factors in the parse string. This argument is not fully used, only as a
	 * check to make ensure
	 * number of factors is what was promised
	 * \param factorDelimiter what char to use to separate factor strings from each other. Usually
	 * use '|'. Can be multi-char
	 */
	public static List<String[]> parse(final String phraseString, final int[] factorOrder,
			final String factorDelimiter) {
		boolean isMultiCharDelimiter = factorDelimiter.length() > 1;
		// parse
		List<String[]> phraseVector = new ArrayList<String[]>();
		String annotatedWordVector[] = Util.tokenize(phraseString);
		// KOMMA|none ART|Def.Z NN|Neut.NotGen.Sg VVFIN|none
		// to
		// "KOMMA|none" "ART|Def.Z" "NN|Neut.NotGen.Sg" "VVFIN|none"

		for (int phrasePos = 0; phrasePos < annotatedWordVector.length; phrasePos++) {
			String annotatedWord = annotatedWordVector[phrasePos];
			String[] factorStrVector;
			if (isMultiCharDelimiter) {
				factorStrVector = Util.tokenizeMultiCharSeparator(annotatedWord, factorDelimiter);
			} else {
				factorStrVector = Util.tokenize(annotatedWord, factorDelimiter);
			}
			// KOMMA|none
			// to
			// "KOMMA" "none"
			if (factorStrVector.length != factorOrder.length) {
				TRACE.err("[ERROR] Malformed input at " + /*
														 * StaticData::Instance().GetCurrentInputPosition
														 * () <<
														 */"\n"
						+ "  Expected input to have words composed of " + factorOrder.length
						+ " factor(s) (form FAC1|FAC2|...)" + "\n"
						+ "  but instead received input with " + factorStrVector.length
						+ " factor(s).\n");
				System.exit(0);
			}
			phraseVector.add(factorStrVector);
		}
		return phraseVector;
	}

	/**
	 * Fills phrase with words from 2D string vector
	 * \param factorOrder factor types of each element in 2D string vector
	 * \param phraseVector 2D string vector
	 */
	public void createFromString(final int[] factorOrder, final List<String[]> phraseVector) {
		FactorCollection factorCollection = FactorCollection.instance();

		for (int phrasePos = 0; phrasePos < phraseVector.size(); phrasePos++) {
			// add word this phrase
			Word word = addWord();
			for (int currFactorIndex = 0; currFactorIndex < factorOrder.length; currFactorIndex++) {
				int factorType = factorOrder[currFactorIndex];
				final String factorStr = phraseVector.get(phrasePos)[currFactorIndex];
				final Factor factor = factorCollection
						.addFactor(m_direction, factorType, factorStr);
				word.set(factorType, factor);
			}
		}
	}

	/**
	 * Fills phrase with words from format string, typically from phrase table or sentence input
	 * \param factorOrder factor types of each element in 2D string vector
	 * \param phraseString formatted input string to parse
	 * \param factorDelimiter delimiter, as used by Parse()
	 */
	public void createFromString(final int[] factorOrder, final String phraseString,
			final String factorDelimiter) {
		List<String[]> phraseVector = parse(phraseString, factorOrder, factorDelimiter);
		createFromString(factorOrder, phraseVector);

	}

	public void createFromStringNewFormat(FactorDirection direction,
			final List<Integer> factorOrder, final String phraseString,
			final String factorDelimiter, Word lhs) {
		m_arity = 0;

		// parse
		String[] annotatedWordVector = Util.tokenize(phraseString);
		// KOMMA|none ART|Def.Z NN|Neut.NotGen.Sg VVFIN|none
		// to
		// "KOMMA|none" "ART|Def.Z" "NN|Neut.NotGen.Sg" "VVFIN|none"

		for (int phrasePos = 0; phrasePos < annotatedWordVector.length - 1; phrasePos++) {
			String annotatedWord = annotatedWordVector[phrasePos];
			boolean isNonTerminal;
			if (annotatedWord.charAt(0) == '['
					&& annotatedWord.charAt(annotatedWord.length() - 1) == ']') { // non-term
				isNonTerminal = true;

				int nextPos = annotatedWord.indexOf('[', 1);
				ASSERT.a(nextPos != -1);

				if (direction == FactorDirection.Input)
					annotatedWord = annotatedWord.substring(1, nextPos - 2 + 1);
				else
					annotatedWord = annotatedWord.substring(nextPos + 1, annotatedWord.length()
							- nextPos - 2 + nextPos + 1);

				m_arity++;
			} else {
				isNonTerminal = false;
			}

			Word word = addWord();
			word.createFromString(direction, factorOrder, annotatedWord, isNonTerminal);

		}

		// lhs
		String annotatedWord = annotatedWordVector[annotatedWordVector.length - 1];
		ASSERT.a(annotatedWord.charAt(0) == '['
				&& annotatedWord.charAt(annotatedWord.length() - 1) == ']');
		annotatedWord = annotatedWord.substring(1, annotatedWord.length() - 2 + 1);

		lhs.createFromString(direction, factorOrder, annotatedWord, true);
		ASSERT.a(lhs.isNonTerminal());
	}

	/**
	 * copy factors from the other phrase to this phrase.
	 * IsCompatible() must be run beforehand to ensure incompatible factors aren't overwritten
	 */
	public void mergeFactors(final Phrase copy) {
		ASSERT.a(getSize() == copy.getSize());
		int size = getSize();
		final int maxNumFactors = StaticData.instance().getMaxNumFactors(getDirection());
		for (int currPos = 0; currPos < size; currPos++) {
			for (int currFactor = 0; currFactor < maxNumFactors; currFactor++) {
				int factorType = (currFactor);
				final Factor factor = copy.getFactor(currPos, factorType);
				if (factor != null)
					setFactor(currPos, factorType, factor);
			}
		}
	}

	// ! copy a single factor (specified by factorType)
	public void mergeFactors(final Phrase copy, int factorType) {
		ASSERT.a(getSize() == copy.getSize());
		for (int currPos = 0; currPos < getSize(); currPos++)
			setFactor(currPos, factorType, copy.getFactor(currPos, factorType));
	}

	// ! copy all factors specified in factorVec and none others
	public void mergeFactors(final Phrase copy, final int[] factorVec) {
		assert (getSize() == copy.getSize());
		for (int currPos = 0; currPos < getSize(); currPos++) {
			for (int i : factorVec) {
				setFactor(currPos, i, copy.getFactor(currPos, i));
			}
		}
	}

	/**
	 * compare 2 phrases to ensure no factors are lost if the phrases are merged
	 * must run IsCompatible() to ensure incompatible factors aren't being overwritten
	 */
	public boolean isCompatible(final Phrase inputPhrase) {
		if (inputPhrase.getSize() != getSize()) {
			return false;
		}

		final int size = getSize();

		final int maxNumFactors = StaticData.instance().getMaxNumFactors(getDirection());
		for (int currPos = 0; currPos < size; currPos++) {
			for (int currFactor = 0; currFactor < maxNumFactors; currFactor++) {
				int factorType = (currFactor);
				final Factor thisFactor = getFactor(currPos, factorType), inputFactor = inputPhrase
						.getFactor(currPos, factorType);
				if (thisFactor != null && inputFactor != null && thisFactor != inputFactor)
					return false;
			}
		}
		return true;
	}

	public boolean isCompatible(final Phrase inputPhrase, int factorType) {
		if (inputPhrase.getSize() != getSize()) {
			return false;
		}
		for (int currPos = 0; currPos < getSize(); currPos++) {
			if (getFactor(currPos, factorType) != inputPhrase.getFactor(currPos, factorType))
				return false;
		}
		return true;
	}

	public boolean isCompatible(final Phrase inputPhrase, final int[] factorVec) {
		if (inputPhrase.getSize() != getSize()) {
			return false;
		}
		for (int currPos = 0; currPos < getSize(); currPos++) {
			for (int i : factorVec) {
				if (getFactor(currPos, i) != inputPhrase.getFactor(currPos, i))
					return false;
			}
		}
		return true;
	}

	// ! really means what language. Input = Source, Output = Target
	public final FactorDirection getDirection() {
		return m_direction;
	}

	// ! number of words
	public int getSize() {
		return m_words.size();
	}

	// ! word at a particular position
	public Word getWord(int pos) {
		return m_words.get(pos);
	}

	// ! particular factor at a particular position
	public Factor getFactor(int pos, int factorType) {
		final Word ptr = m_words.get(pos);
		return ptr.get(factorType);
	}

	public final void setFactor(int pos, int factorType, final Factor factor) {
		Word ptr = m_words.get(pos);
		ptr.set(factorType, factor);
	}

	public int getNumTerminals() {
		int ret = 0;

		for (int pos = 0; pos < getSize(); ++pos) {
			if (!getWord(pos).isNonTerminal())
				ret++;
		}
		return ret;
	}

	// ! whether the 2D vector is a substring of this phrase
	public boolean contains(final List<String[]> subPhraseVector, final List<Integer> inputFactor) {
		final int subSize = subPhraseVector.size(), thisSize = getSize();
		if (subSize > thisSize)
			return false;

		// try to match word-for-word
		for (int currStartPos = 0; currStartPos < (thisSize - subSize + 1); currStartPos++) {
			boolean match = true;

			for (int currFactorIndex = 0; currFactorIndex < inputFactor.size(); currFactorIndex++) {
				int factorType = inputFactor.get(currFactorIndex);
				for (int currSubPos = 0; currSubPos < subSize; currSubPos++) {
					int currThisPos = currSubPos + currStartPos;
					final String subStr = subPhraseVector.get(currSubPos)[currFactorIndex], thisStr = getFactor(
							currThisPos, factorType).getString();
					if (subStr != thisStr) {
						match = false;
						break;
					}
				}
				if (!match)
					break;
			}

			if (match)
				return true;
		}
		return false;
	}

	// ! create an empty word at the end of the phrase
	public Word addWord() {
		Word w = new Word();
		m_words.add(w);
		return w;
	}

	// ! create copy of input word at the end of the phrase
	public void addWord(final Word newWord) {
		m_words.add(new Word(newWord));
	}

	/** appends a phrase at the end of current phrase **/
	public void append(final Phrase endPhrase) {
		for (int i = 0; i < endPhrase.getSize(); i++) {
			addWord(endPhrase.getWord(i));
		}
	}

	public void prependWord(final Word newWord) {
		addWord();

		// shift
		for (int pos = getSize() - 1; pos >= 1; --pos) {
			final Word word = m_words.get(pos - 1);
			m_words.set(pos, word);
		}

		m_words.set(0, newWord);
	}

	public void clear() {
		m_words.clear();
	}

	public void removeWord(int pos) {
		ASSERT.a(pos < m_words.size());

		m_words.remove(pos);
	}

	// ! create new phrase class that is a substring of this phrase
	public Phrase getSubString(final WordsRange wordsRange) {
		Phrase retPhrase = new Phrase(m_direction);
		for (int currPos = wordsRange.getStartPos(); currPos <= wordsRange.getEndPos(); currPos++) {
			retPhrase.addWord(getWord(currPos));
		}

		return retPhrase;
	}

	// ! return a string rep of the phrase. Each factor is separated by the factor delimiter as
	// specified in StaticData class
	public String getStringRep(final int[] factorsToPrint) {

		StringBuilder strme = new StringBuilder();
		for (int pos = 0; pos < getSize(); pos++) {
			strme.append(getWord(pos).getString(factorsToPrint, (pos != getSize() - 1)));
		}

		return strme.toString();
	}



	public int compare(final Phrase other) {
		int thisSize = getSize(), compareSize = other.getSize();
		if (thisSize != compareSize) {
			return (thisSize < compareSize) ? -1 : 1;
		}

		for (int pos = 0; pos < thisSize; pos++) {
			final Word thisWord = getWord(pos), otherWord = other.getWord(pos);
			int ret = Word.compare(thisWord, otherWord);

			if (ret != 0)
				return ret;
		}

		return 0;
	}

	/**
	 * transitive comparison between 2 phrases
	 * used to insert & find phrase in dictionary
	 */
	public boolean less(final Phrase compare) {
		return compare(compare) < 0;
	}
	@Override
	public int hashCode(){
		return 19771010;//
	}
	@Override
	public boolean equals( Object compare) {
		return compare((Phrase)compare) == 0;
	}

	public int getArity() {
		return m_arity;
	}
	// TO_STRING();
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int pos = 0; pos < this.getSize(); pos++) {
			Word word = this.getWord(pos);
			sb.append(word.toString());
		}
		return sb.toString();
	}

}
