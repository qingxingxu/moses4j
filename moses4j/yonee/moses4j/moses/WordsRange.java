package yonee.moses4j.moses;

import yonee.utils.ASSERT;

/**
 * 
 * @author YONEE
 * @OK
 * 
 */
public class WordsRange {

	int m_startPos, m_endPos;

	public WordsRange() {
		this(0, 0);
	}

	public WordsRange(int startPos, int endPos) {
		m_startPos = startPos;
		m_endPos = endPos;
	}

	public WordsRange(final WordsRange copy) {
		m_startPos = copy.getStartPos();
		m_endPos = copy.getEndPos();
	}

	public final int getStartPos() {
		return m_startPos;
	}

	public final int getEndPos() {
		return m_endPos;
	}

	// ! count of words translated
	public final int getNumWordsCovered() {
		return (m_startPos == TypeDef.NOT_FOUND) ? 0 : m_endPos - m_startPos + 1;
	}

	// ! transitive comparison
	public final boolean less(final WordsRange x) {
		return (m_startPos < x.m_startPos || (m_startPos == x.m_startPos && m_endPos < x.m_endPos));
	}

	// equality operator
	public final boolean equals(final WordsRange x) {
		return (m_startPos == x.m_startPos && m_endPos == x.m_endPos);
	}

	// Whether two word ranges overlap or not
	public final boolean overlap(final WordsRange x) {

		if (x.m_endPos < m_startPos || x.m_startPos > m_endPos)
			return false;

		return true;
	}

	public final int getNumWordsBetween(final WordsRange x) {
		ASSERT.a(!overlap(x));
		if (x.m_endPos < m_startPos) {
			return m_startPos - x.m_endPos;
		}

		return x.m_startPos - m_endPos;
	}

	// TO_STRING();
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("[").append(this.m_startPos).append("..").append(this.m_endPos).append("]");
		return out.toString();
	}
}
