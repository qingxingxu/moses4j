package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;

import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK
 */
public class ReorderingConstraint {
	public final static int NOT_A_ZONE = 999999999;
	// friend std::ostream& operator<<(std::ostream& out, const ReorderingConstraint&
	// reorderingConstraint);

	// const size_t m_size; /**< number of words in sentence */
	protected int m_size;
	/** < number of words in sentence */
	protected boolean[] m_wall;
	/** < flag for each word if it is a wall */
	protected int[] m_localWall;
	/** < flag for each word if it is a local wall */
	protected List<List<Integer>> m_zone = new ArrayList<List<Integer>>();
	/** zones that limit reordering */
	protected boolean m_active;

	/** < flag indicating, if there are any active constraints */

	// ! create ReorderingConstraint of length size and initialise to zero
	public ReorderingConstraint() {
		m_wall = null;
		m_localWall = null;
		m_active = false;
	}

	// //! destructer
	// ~ReorderingConstraint()
	// {
	// if (m_wall != NULL) free(m_wall);
	// if (m_localWall != NULL) free(m_localWall);
	// }

	// ! allocate memory for memory for a sentence of a given size
	public void initializeWalls(int size) {
		m_size = size;
		m_wall = new boolean[size];
		m_localWall = new int[size];

		for (int pos = 0; pos < m_size; pos++) {
			m_wall[pos] = false;
			m_localWall[pos] = NOT_A_ZONE;
		}
	}

	// ! changes walls in zones into local walls
	public void finalizeWalls() {
		for (int z = 0; z < m_zone.size(); z++) {
			final int startZone = m_zone.get(z).get(0);
			final int endZone = m_zone.get(z).get(1);// note: wall after endZone is not local
			for (int pos = startZone; pos < endZone; pos++) {
				if (m_wall[pos]) {
					m_localWall[pos] = z;
					m_wall[pos] = false;
					VERBOSE.v(3, "SETTING local wall " + pos + "\n");
				}
				// enforce that local walls only apply to innermost zone
				else if (m_localWall[pos] != NOT_A_ZONE) {
					int assigned_z = m_localWall[pos];
					if ((m_zone.get(assigned_z).get(0) < startZone)
							|| (m_zone.get(assigned_z).get(1) > endZone)) {
						m_localWall[pos] = z;
					}
				}
			}
		}
	}

	// ! set value at a particular position
	public void setWall(int pos, boolean value) {
		VERBOSE.v(3, "SETTING reordering wall at position " + pos + "\n");
		m_wall[pos] = value;
		m_active = true;
	}

	// ! whether a word has been translated at a particular position
	public boolean getWall(int pos) {
		return m_wall[pos];
	}

	// ! whether a word has been translated at a particular position
	public boolean getLocalWall(int pos, int zone) {
		return (m_localWall[pos] == zone);
	}

	// ! set a zone
	public void setZone(int startPos, int endPos) {
		VERBOSE.v(3, "SETTING zone " + startPos + "-" + endPos + "\n");
		List<Integer> newZone = new ArrayList<Integer>();
		newZone.add(startPos);
		newZone.add(endPos);
		m_zone.add(newZone);
		m_active = true;
	}

	// ! returns the vector of zones
	public List<List<Integer>> getZones() {
		return m_zone;
	}

	// ! set the reordering walls based on punctuation in the sentence
	public void setMonotoneAtPunctuation(final Phrase sentence) {

		for (int i = 0; i < sentence.getSize(); i++) {
			final Word word = sentence.getWord(i);
			if (word.get(0).getString().equals(",") || word.get(0).getString().equals(".")
					|| word.get(0).getString().equals("!") || word.get(0).getString().equals("?")
					|| word.get(0).getString().equals(":") || word.get(0).getString().equals(";")
					|| word.get(0).getString().equals("\"")) {
				// set wall before and after punc, but not at sentence start, end
				if (i > 0 && i < m_size - 1)
					setWall(i, true);
				if (i > 1)
					setWall(i - 1, true);
			}
		}
	}

	// ! check if all constraints are fulfilled -> all find
	public boolean check(final WordsBitmap bitmap, int start, int end) {
		// nothing to be checked, we are done
		if (!isActive())
			return true;

		VERBOSE.v(3, "CHECK " + bitmap + " " + start + "-" + end);

		// check walls
		int firstGapPos = bitmap.getFirstGapPos();
		// filling first gap -> no wall violation possible
		if (firstGapPos != end) {
			// if there is a wall before the last word,
			// we created a gap while moving through wall
			// -> violation
			for (int pos = firstGapPos; pos < end; pos++) {
				if (getWall(pos)) {
					VERBOSE.v(3, " hitting wall " + pos + "\n");
					return false;
				}
			}
		}

		// monotone -> no violation possible
		int lastPos = bitmap.getLastPos();
		if ((lastPos == TypeDef.NOT_FOUND && start == 0)
				|| (firstGapPos > lastPos && firstGapPos == start)) {
			VERBOSE.v(3, " montone, fine.\n");
			return true;
		}

		// check zones
		for (int z = 0; z < m_zone.size(); z++) {
			final int startZone = m_zone.get(z).get(0);
			final int endZone = m_zone.get(z).get(1);
			// fine, if translation has not reached zone yet and phrase outside zone
			if (lastPos < startZone && (end < startZone || start > endZone)) {
				continue;
			}

			// already completely translated zone, no violations possible
			if (firstGapPos > endZone) {
				continue;
			}

			// some words are translated beyond the start
			// let's look closer if some are in the zone
			int numWordsInZoneTranslated = 0;
			if (lastPos >= startZone) {
				for (int pos = startZone; pos <= endZone; pos++) {
					if (bitmap.getValue(pos)) {
						numWordsInZoneTranslated++;
					}
				}
			}

			// all words in zone translated, no violation possible
			if (numWordsInZoneTranslated == endZone - startZone + 1) {
				continue;
			}

			// flag if this is an active zone
			boolean activeZone = (numWordsInZoneTranslated > 0);

			// fine, if zone completely untranslated and phrase outside zone
			if (!activeZone && (end < startZone || start > endZone)) {
				continue;
			}

			// violation, if phrase completely outside active zone
			if (activeZone && (end < startZone || start > endZone)) {
				VERBOSE.v(3, " outside active zone\n");
				return false;
			}

			// ok, this is what we know now:
			// * the phrase is in the zone (at least partially)
			// * either zone is already active, or it becomes active now

			// check, if we are setting us up for a dead end due to distortion limits
			if (start != firstGapPos
					&& endZone - firstGapPos >= StaticData.instance().getMaxDistortion()) {
				VERBOSE.v(3, " dead end due to distortion limit\n");
				return false;
			}

			// let us check on phrases that are partially outside

			// phrase overlaps at the beginning, always ok
			if (start <= startZone) {
				continue;
			}

			// phrase goes beyond end, has to fill zone completely
			if (end > endZone) {
				if (endZone - start + 1 < // num. words filled in by phrase
				endZone - startZone + 1 - numWordsInZoneTranslated) // num. untranslated
				{
					VERBOSE.v(3, " overlap end, but not completing\n");
					return false;
				} else {
					continue;
				}
			}

			// now we are down to phrases that are completely inside the zone
			// we have to check local walls
			boolean seenUntranslatedBeforeStartPos = false;
			for (int pos = startZone; pos < endZone && pos < end; pos++) {
				// be careful when there is a gap before phrase
				if (!bitmap.getValue(pos) // untranslated word
						&& pos < start) // before startPos
				{
					seenUntranslatedBeforeStartPos = true;
				}
				if (seenUntranslatedBeforeStartPos && getLocalWall(pos, z)) {
					VERBOSE.v(3, " local wall violation\n");
					return false;
				}
			}

			// passed all checks for this zone, on to the next one
		}

		// passed all checks, no violations
		VERBOSE.v(3, " fine.\n");
		return true;
	}

	// ! checks if reordering constraints will be enforced
	public boolean isActive() {
		return m_active;
	}

}
