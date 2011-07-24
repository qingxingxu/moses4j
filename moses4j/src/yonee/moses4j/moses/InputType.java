package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import yonee.moses4j.moses.TypeDef.InputTypeEnum;

/**
 * 
 * @author YONEE
 * @OK<<
 */
public interface InputType {
	public class Impl {
		protected long m_translationId; // < contiguous Id
		protected boolean m_hasMetaData;
		protected long m_segId;
		protected ReorderingConstraint m_reorderingConstraint = new ReorderingConstraint();
		/** < limits on reordering specified either by "-mp" switch or xml tags */

		// used in -continue-partial-translation
		public List<Boolean> m_sourceCompleted = new ArrayList<Boolean>();
		public String m_initialTargetPhrase = new String();
		public int m_frontSpanCoveredLength;

		// how many words from the beginning are covered

		public Impl() {
			this(0);
		}
		
		public Impl(long translationId) {
			m_translationId = translationId;
			m_frontSpanCoveredLength = 0;
		}

		public final long getTranslationId() {
			return m_translationId;
		}

		public void setTranslationId(long translationId) {
			m_translationId = translationId;
		}

		// ! returns the number of words moved
		public int computeDistortionDistance(final WordsRange prev, final WordsRange current) {
			int dist = 0;
			if (prev.getNumWordsCovered() == 0) {
				dist = current.getStartPos();
			} else {
				dist = (int) prev.getEndPos() - (int) current.getStartPos() + 1;
			}
			return Math.abs(dist);
		}

		// ! In a word lattice, tells you if there's a path from node start to node end
		public boolean canIGetFromAToB(int start, int end) {
			return true;
		}

		// ! is there a path covering [range] (lattice only, otherwise true)
		public boolean isCoveragePossible(final WordsRange range) {
			return canIGetFromAToB(range.getStartPos(), range.getEndPos() + 1);
		}

		// ! In a word lattice, you can't always get from node A to node B
		public boolean isExtensionPossible(final WordsRange prev, final WordsRange current) {
			// return ComputeDistortionDistance(prev, current) < 100000;
			int t = prev.getEndPos() + 1; // 2
			int l = current.getEndPos() + 1; // l=1
			int r = l;
			if (l < t) {
				r = t;
			} else {
				l = t;
			} // r=2
			if (!canIGetFromAToB(l, r))
				return false;

			// there's another check here: a current span may end at a place that previous could get
			// to,
			// but it may not *START* at a place it can get to. We'll also have to check if we're
			// going
			// left or right

			r = current.getStartPos();
			l = prev.getEndPos() + 1;
			if (l == r)
				return true;
			if (prev.getEndPos() > current.getStartPos()) {
				r = prev.getStartPos();
				l = current.getEndPos() + 1;
				if (r == l)
					return true;
			}
			return canIGetFromAToB(l, r);
		}

		public ReorderingConstraint getReorderingConstraint() {
			return m_reorderingConstraint;
		}
	}

	public Impl i();

	public InputTypeEnum getType();

	// ! number of words in this sentence/confusion network
	public int getSize();

	// ! populate this InputType with data from in stream
	public int read(BufferedReader in, final int[] factorOrder) throws IOException;

	// ! Output debugging info to stream out
	public void print(PrintStream out);

	// ! create trans options specific to this InputType
	public TranslationOptionCollection createTranslationOptionCollection();

	// ! return substring. Only valid for Sentence class. TODO - get rid of this fn
	public Phrase getSubString(final WordsRange wordsRange);

	// ! return substring at a particular position. Only valid for Sentence class. TODO - get rid of
	// this fn
	public Word getWord(int pos);

	public List<Word> getLabelList(int startPos, int endPos);

}
