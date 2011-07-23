package yonee.moses4j.moses;

import java.io.PrintStream;
import java.util.List;

/**
 * 
 * @author YONEE
 * @OK
 */
public abstract class LexicalReorderingTable {

	//
	/*
	 * local helper functions
	 */
	

	static void auxAppend(List<Integer> head, int[] tail) {
		for (int i = 0; i < tail.length; i++) {
			head.add(tail[i]);
		}
	}

	//

	
	public LexicalReorderingTable(final int[] f_factors, final int[] e_factors,
			final int[] c_factors) {
		m_FactorsF = f_factors;
		m_FactorsE = e_factors;
		m_FactorsC = c_factors;
	}

	public static LexicalReorderingTable loadAvailable(final String filePath,
			final int[] f_factors, final int[] e_factors,
			final int[] c_factors) {
		// decide use Tree or Memory table
		if (Util.fileExists(filePath + ".binlexr.idx")) {
			// there exists a binary version use that
			return new LexicalReorderingTableTree(filePath, f_factors, e_factors, c_factors);
		} else {
			// use plain memory
			return new LexicalReorderingTableMemory(filePath, f_factors, e_factors, c_factors);
		}
	}

	public abstract List<Float> getScore(final Phrase f, final Phrase e, final Phrase c);

	public void initializeForInput(final InputType i) {
		/* override for on-demand loading */
	};

	public void InitializeForInputPhrase(final Phrase phrase) {
	};

	/*
	 * int GetNumScoreComponents() final {
	 * return m_NumScores;
	 * }
	 */
	public int[] getFFactorMask() {
		return m_FactorsF;
	}

	public int[] getEFactorMask() {
		return m_FactorsE;
	}

	public int[] getCFactorMask() {
		return m_FactorsC;
	}

	public void DbgDump(PrintStream out) {
		out.print("Overwrite in subclass...\n");
	};

	protected int[] m_FactorsF ;
	protected int[] m_FactorsE;
	protected int[] m_FactorsC ;
}
