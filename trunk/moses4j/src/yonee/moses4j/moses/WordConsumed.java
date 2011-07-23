package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK<<
 */
public class WordConsumed {
	// friend std::ostream& operator<<(std::ostream&, const WordConsumed&);

	protected WordsRange m_coverage;
	protected final Word m_sourceWord; // can be non-term headword, or term
	protected final WordConsumed m_prevWordsConsumed;

	// public WordConsumed(); // not implmented
	public WordConsumed(int startPos, int endPos, final Word sourceWord,
			final WordConsumed prevWordsConsumed) {
		m_coverage = new WordsRange(startPos, endPos);
		m_sourceWord = sourceWord;
		m_prevWordsConsumed = prevWordsConsumed;
	}

	public final WordsRange getWordsRange() {
		return m_coverage;
	}

	public final Word getSourceWord() {
		return m_sourceWord;
	}

	public WordsRange GetWordsRange() {
		return m_coverage;
	}

	public boolean isNonTerminal() {
		return m_sourceWord.isNonTerminal();
	}

	final WordConsumed getPrevWordsConsumed() {
		return m_prevWordsConsumed;
	}

	// ! transitive comparison used for adding objects into FactorCollection
	public final boolean less(final WordConsumed compare) {
		if (isNonTerminal() != compare.isNonTerminal())
			return true;
		else if (isNonTerminal() == compare.isNonTerminal())
			return m_coverage.less(compare.m_coverage);

		return false;
	}
}
