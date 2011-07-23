package yonee.moses4j.moses;

import java.util.List;

import yonee.utils.ASSERT;
import yonee.utils.ArrayUtils;

/**
 * 
 * @author YONEE
 * @OK<<
 */
public class WordsBitmap {
	// friend std::ostream& operator<<(std::ostream& out, const WordsBitmap& wordsBitmap);

	protected final int m_size;
	/** < number of words in sentence */
	protected boolean m_bitmap[];

	/** < ticks of words that have been done */

	// protected WordsBitmap(); // not implemented

	// ! set all elements to false
	protected void initialize() {
		for (int pos = 0; pos < m_size; pos++) {
			m_bitmap[pos] = false;
		}
	}

	// sets elements by vector
	protected void initialize(List<Boolean> vector) {
		int vector_size = vector.size();
		for (int pos = 0; pos < m_size; pos++) {
			if (pos < vector_size && vector.get(pos) == true)
				m_bitmap[pos] = true;
			else
				m_bitmap[pos] = false;
		}
	}

	// ! create WordsBitmap of length size and initialise with vector
	public WordsBitmap(int size, List<Boolean> initialize_vector) {
		m_size = size;
		m_bitmap = new boolean[size];
		initialize(initialize_vector);
	}

	// ! create WordsBitmap of length size and initialise
	public WordsBitmap(int size) {
		m_size = size;
		m_bitmap = new boolean[size];
		initialize();
	}
	

	// ! deep copy
	public WordsBitmap(final WordsBitmap copy) {
		m_size = copy.m_size;
		m_bitmap = new boolean[m_size];
		for (int pos = 0; pos < copy.m_size; pos++) {
			m_bitmap[pos] = copy.getValue(pos);
		}
	}

	// ! count of words translated
	public int getNumWordsCovered() {
		int count = 0;
		for (int pos = 0; pos < m_size; pos++) {
			if (m_bitmap[pos])
				count++;
		}
		return count;
	}

	// ! position of 1st word not yet translated, or NOT_FOUND if everything already translated
	public int getFirstGapPos() {
		for (int pos = 0; pos < m_size; pos++) {
			if (!m_bitmap[pos]) {
				return pos;
			}
		}
		// no starting pos
		return TypeDef.NOT_FOUND;
	}

	// ! position of last word not yet translated, or NOT_FOUND if everything already translated
	public int getLastGapPos() {
		for (int pos = (int) m_size - 1; pos >= 0; pos--) {
			if (!m_bitmap[pos]) {
				return pos;
			}
		}
		// no starting pos
		return TypeDef.NOT_FOUND;
	}

	// ! position of last translated word
	public int getLastPos() {
		for (int pos = (int) m_size - 1; pos >= 0; pos--) {
			if (m_bitmap[pos]) {
				return pos;
			}
		}
		// no starting pos
		return TypeDef.NOT_FOUND;
	}

	// ! whether a word has been translated at a particular position
	public boolean getValue(int pos) {
		return m_bitmap[pos];
	}

	// ! set value at a particular position
	public void setValue(int pos, boolean value) {
		m_bitmap[pos] = value;
	}

	// ! set value between 2 positions, inclusive
	public void setValue(int startPos, int endPos, boolean value) {
		for (int pos = startPos; pos <= endPos; pos++) {
			m_bitmap[pos] = value;
		}
	}

	// ! whether every word has been translated
	public boolean isComplete() {
		return getSize() == getNumWordsCovered();
	}

	// ! whether the wordrange overlaps with any translated word in this bitmap
	public boolean overlap(final WordsRange compare) {
		for (int pos = compare.getStartPos(); pos <= compare.getEndPos(); pos++) {
			if (m_bitmap[pos])
				return true;
		}
		return false;
	}

	// ! number of elements
	public int getSize() {
		return m_size;
	}

	// ! transitive comparison of WordsBitmap
	public final int compare(final WordsBitmap compare) {
		// -1 = less than
		// +1 = more than
		// 0 = same

		int thisSize = getSize(), compareSize = compare.getSize();

		if (thisSize != compareSize) {
			return (thisSize < compareSize) ? -1 : 1;
		}

		return ArrayUtils.compare(m_bitmap, compare.m_bitmap, thisSize);
	}

	public boolean less(final WordsBitmap compare) {
		return compare(compare) < 0;
	}

	public final int getEdgeToTheLeftOf(int l) {
		if (l == 0)
			return l;
		while (l != 0 && !m_bitmap[l - 1]) {
			--l;
		}
		return l;
	}

	public final int getEdgeToTheRightOf(int r) {
		if (r + 1 == m_size)
			return r;
		while (r + 1 < m_size && !m_bitmap[r + 1]) {
			++r;
		}
		return r;
	}

	// ! TODO - ??? no idea
	public int getFutureCosts(int lastPos) {
		int sum = 0;
		boolean aim1 = false, ai = false, aip1 = m_bitmap[0];

		for (int i = 0; i < m_size; ++i) {
			aim1 = ai;
			ai = aip1;
			aip1 = (i + 1 == m_size || m_bitmap[i + 1]);

			// #ifndef NDEBUG
			// if( i>0 ) assert( aim1==(i==0||m_bitmap[i-1]==1));
			// //assert( ai==a[i] );
			// if( i+1<m_size ) assert( aip1==m_bitmap[i+1]);
			// #endif
			if ((i == 0 || aim1) && ai == false) {
				sum += Math.abs(lastPos - (int) (i) + 1);
				// sum+=getJumpCosts(lastPos,i,maxJumpWidth);
			}
			// if(sum>1e5) return sum;
			if (i > 0 && ai == false && (i + 1 == m_size || aip1))
				lastPos = (int) (i + 1);
		}

		// sum+=getJumpCosts(lastPos,as,maxJumpWidth);
		sum += Math.abs(lastPos - (int) (m_size) + 1); // getCosts(lastPos,as);
		assert (sum >= 0);

		// TRACE_ERR(sum<<"\n");
		return sum;
	}

	// ! converts bitmap into an integer ID: it consists of two parts: the first 16 bit are the
	// pattern between the first gap and the last word-1, the second 16 bit are the number of filled
	// positions. enforces a sentence length limit of 65535 and a max distortion of 16
	public long getID() {
		assert (m_size < (1 << 16));

		int start = getFirstGapPos();
		if (start == TypeDef.NOT_FOUND)
			start = m_size; // nothing left

		int end = getLastPos();
		if (end == TypeDef.NOT_FOUND)
			end = 0; // nothing translated yet

		ASSERT.a(end < start || end - start <= 16);
		long id = 0;
		for (int pos = end; pos > start; pos--) {
			id = id * 2 + (getValue(pos) ? 1 : 0);
		}
		return id + (1 << 16) * start;
	}

	// ! converts bitmap into an integer ID, with an additional span covered
	public long getIDPlus(int startPos, int endPos) {
		ASSERT.a(m_size < (1 << 16));

		int start = getFirstGapPos();
		if (start == TypeDef.NOT_FOUND)
			start = m_size; // nothing left

		int end = getLastPos();
		if (end == TypeDef.NOT_FOUND)
			end = 0; // nothing translated yet

		if (start == startPos)
			start = endPos + 1;
		if (end < endPos)
			end = endPos;

		ASSERT.a(end < start || end - start <= 16);
		long id = 0;
		for (int pos = end; pos > start; pos--) {
			id = id * 2;
			if (getValue(pos) || (startPos <= pos && pos <= endPos))
				id++;
		}
		return id + (1 << 16) * start;
	}

	// TO_STRING(); {
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < m_size; i++) {
			sb.append((getValue(i) ? 1 : 0));
		}
		return sb.toString();
	}

}
