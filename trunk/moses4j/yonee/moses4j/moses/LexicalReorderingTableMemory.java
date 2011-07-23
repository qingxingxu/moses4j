package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import yonee.utils.TRACE;

/**
 * 
 * @author YONEE
 * @OK
 */
public class LexicalReorderingTableMemory extends LexicalReorderingTable {

	private Map<String, List<Float>> m_Table = new HashMap<String, List<Float>>();

	public LexicalReorderingTableMemory(int[] fFactors, int[] eFactors, int[] cFactors) {
		super(fFactors, eFactors, cFactors);

	}

	public LexicalReorderingTableMemory(String filePath, int[] fFactors, int[] eFactors,
			int[] cFactors) {
		super(fFactors, eFactors, cFactors);
		try {
			loadFromFile(filePath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Float> getScore(Phrase f, Phrase e, Phrase c) {
		List<Float> r = null;
		String key;
		if (0 == c.getSize()) {
			key = makeKey(f, e, c);
			r = m_Table.get(key);
			if (r != null) {
				return r;
			}
		} else {
			// right try from large to smaller context
			for (int i = 0; i <= c.getSize(); ++i) {
				Phrase sub_c = new Phrase(c.getSubString(new WordsRange(i, c.getSize() - 1)));
				key = makeKey(f, e, sub_c);
				r = m_Table.get(key);
				if (r != null) {
					return r;
				}
			}
		}
		return new ArrayList<Float>();
	}

	public void dbgDump(PrintStream out) {

		for (Entry<String, List<Float>> i : m_Table.entrySet()) {

			out.print(" key: '" + i.getKey() + "' score: ");
			out.print("(num scores: " + i.getValue().size() + ")");
			for (Float j : i.getValue()) {
				out.print(j + " ");
			}
			out.println();
		}
	}

	private String makeKey(final Phrase f, final Phrase e, final Phrase c) {
		return makeKey(f.getStringRep(m_FactorsF).trim(), e.getStringRep(m_FactorsE).trim(), c
				.getStringRep(m_FactorsC).trim());
	}

	private String makeKey(final String f, final String e, final String c) {
		String key = "";
		if (!f.isEmpty()) {
			key += f;
		}
		if (m_FactorsE != null) {
			if (!key.isEmpty()) {
				key += "|||";
			}
			key += e;
		}
		if (m_FactorsC != null) {
			if (!key.isEmpty()) {
				key += "|||";
			}
			key += c;
		}
		return key;
	}

	private void loadFromFile(final String filePath) throws IOException {
		String fileName = filePath;
		if (!Util.fileExists(fileName) && Util.fileExists(fileName + ".gz")) {
			fileName += ".gz";
		}
		BufferedReader file = new BufferedReader(new FileReader(fileName));
		String line = "";
		int numScores = -1;
		System.err.print("Loading table into memory...");
		while ((line = file.readLine()) != null) {
			String[] tokens = Util.tokenizeMultiCharSeparator(line, "|||");
			int t = 0;
			String f = "", e = "", c = "";

			if (m_FactorsF != null) {
				// there should be something for f
				f = tokens[t].trim();
				++t;
			}
			if (m_FactorsE != null) {
				// there should be something for e
				e = tokens[t].trim();
				++t;
			}
			if (m_FactorsC != null) {
				// there should be something for c
				c = tokens[t].trim();
				++t;
			}
			// last token are the probs
			List<Float> p = Scan.toFloatList(Util.tokenize(tokens[t]));
			// sanity check: all lines must have equall number of probs
			if (-1 == numScores) {
				numScores = (int) p.size(); // set in first line
			}
			if ((int) p.size() != numScores) {
				TRACE.err("found inconsistent number of probabilities... found " + p.size()
						+ " expected " + numScores + "\n");
				System.exit(0);
			}
			Util.transformScore(p);
			Util.floorScore(p);

			m_Table.put(makeKey(f, e, c), p);
		}
		System.err.print("done.\n");
	}
}
