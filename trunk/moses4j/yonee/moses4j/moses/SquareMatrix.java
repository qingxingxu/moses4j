package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK<<
 */
public class SquareMatrix {

	// friend std::ostream& operator<<(std::ostream &out, const SquareMatrix &matrix);

	protected final int m_size;
	/** < length of the square (sentence length) */
	protected float[] m_array;

	/** < two-dimensional array to store floats */

	//protected SquareMatrix() 

	// protected SquareMatrix(final SquareMatrix &copy); // not implemented

	public SquareMatrix(int size) {
		m_size = size;
		m_array = new float[size * size];

	}

	/** Returns length of the square: typically the sentence length */
	public final int getSize() {
		return m_size;
	}

	/** Get a future cost score for a span */
	public final float getScore(int startPos, int endPos) {
		return m_array[startPos * m_size + endPos];
	}

	/** Set a future cost score for a span */
	public final void setScore(int startPos, int endPos, float value) {
		m_array[startPos * m_size + endPos] = value;
	}

	public float calcFutureScore(WordsBitmap bitmap) {
		final int notInGap = Integer.MAX_VALUE;
		int startGap = notInGap;
		float futureScore = 0.0f;
		for (int currPos = 0; currPos < bitmap.getSize(); currPos++) {
			// start of a new gap?
			if (bitmap.getValue(currPos) == false && startGap == notInGap) {
				startGap = currPos;
			}
			// end of a gap?
			else if (bitmap.getValue(currPos) == true && startGap != notInGap) {
				futureScore += getScore(startGap, currPos - 1);
				startGap = notInGap;
			}
		}
		// coverage ending with gap?
		if (startGap != notInGap) {
			futureScore += getScore(startGap, bitmap.getSize() - 1);
		}

		return futureScore;
	}

	public float calcFutureScore(WordsBitmap bitmap, int startPos, int endPos) {
		final int notInGap = Integer.MAX_VALUE;
		float futureScore = 0.0f;
		int startGap = bitmap.getFirstGapPos();
		if (startGap == TypeDef.NOT_FOUND)
			return futureScore; // everything filled

		// start loop at first gap
		int startLoop = startGap + 1;
		if (startPos == startGap) // unless covered by phrase
		{
			startGap = notInGap;
			startLoop = endPos + 1; // -> postpone start
		}

		int lastCovered = bitmap.getLastPos();
		if (endPos > lastCovered || lastCovered == TypeDef.NOT_FOUND)
			lastCovered = endPos;

		for (int currPos = startLoop; currPos <= lastCovered; currPos++) {
			// start of a new gap?
			if (startGap == notInGap && bitmap.getValue(currPos) == false
					&& (currPos < startPos || currPos > endPos)) {
				startGap = currPos;
			}
			// end of a gap?
			else if (startGap != notInGap
					&& (bitmap.getValue(currPos) == true || (startPos <= currPos && currPos <= endPos))) {
				futureScore += getScore(startGap, currPos - 1);
				startGap = notInGap;
			}
		}
		// coverage ending with gap?
		if (lastCovered != bitmap.getSize() - 1) {
			futureScore += getScore(lastCovered + 1, bitmap.getSize() - 1);
		}

		return futureScore;
	}

	// TO_STRING();

}
