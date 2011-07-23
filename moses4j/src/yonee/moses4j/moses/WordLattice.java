package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import yonee.moses4j.pcn.PCNTools;
import yonee.utils.ASSERT;
import yonee.utils.CollectionUtils;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class WordLattice extends ConfusionNet {

	private int[][] next_nodes;
	private int[][] distances;

	public WordLattice() {

	}

	public int getColumnIncrement(int i, int j) {
		return next_nodes[i][j];
	}

	public void print(PrintStream out) {
		out.print("word lattice: " + data.size() + "\n");
		for (int i = 0; i < data.size(); ++i) {
			out.print(i + " -- ");
			for (int j = 0; j < data.get(i).size(); ++j) {
				out.print("(" + data.get(i).get(j).first.toString() + ", ");
				for (float scoreIterator : data.get(i).get(j).second) {
					out.print(scoreIterator + ", ");
				}
				out.println(getColumnIncrement(i, j) + ") ");
			}

			out.print("\n");
		}
		out.print("\n\n");
	}

	/**
	 * Get shortest path between two nodes
	 */
	public int computeDistortionDistance(final WordsRange prev, final WordsRange current) {
		int result;

		if (prev.getStartPos() == TypeDef.NOT_FOUND && current.getStartPos() == 0) {
			result = 0;

			VERBOSE.v(4, "Word lattice distortion: monotonic initial step\n");
		} else if (prev.getEndPos() + 1 == current.getStartPos()) {
			result = 0;

			VERBOSE.v(4, "Word lattice distortion: monotonic step from " + prev.getEndPos()
					+ " to " + current.getStartPos() + "\n");
		} else if (prev.getStartPos() == TypeDef.NOT_FOUND) {
			result = distances[0][current.getStartPos()];

			VERBOSE.v(4, "Word lattice distortion: initial step from 0 to " + current.getStartPos()
					+ " of length " + result + "\n");
			if (result < 0 || result > 99999) {
				TRACE.err("prev: " + prev + "\ncurrent: " + current + "\n");
				TRACE.err("A: got a weird distance from 0 to " + (current.getStartPos() + 1)
						+ " of " + result + "\n");
			}
		} else if (prev.getEndPos() > current.getStartPos()) {
			result = distances[current.getStartPos()][prev.getEndPos() + 1];

			VERBOSE.v(4, "Word lattice distortion: backward step from " + (prev.getEndPos() + 1)
					+ " to " + current.getStartPos() + " of length " + result + "\n");
			if (result < 0 || result > 99999) {
				TRACE.err("prev: " + prev + "\ncurrent: " + current + "\n");
				TRACE.err("B: got a weird distance from " + current.getStartPos() + " to "
						+ prev.getEndPos() + 1 + " of " + result + "\n");
			}
		} else {
			result = distances[prev.getEndPos() + 1][current.getStartPos()];

			VERBOSE.v(4, "Word lattice distortion: forward step from " + (prev.getEndPos() + 1)
					+ " to " + current.getStartPos() + " of length " + result + "\n");
			if (result < 0 || result > 99999) {
				TRACE.err("prev: " + prev + "\ncurrent: " + current + "\n");
				TRACE.err("C: got a weird distance from " + prev.getEndPos() + 1 + " to "
						+ current.getStartPos() + " of " + result + "\n");
			}
		}

		return result;
	}

	// is it possible to get from the edge of the previous word range to the current word range
	public boolean canIGetFromAToB(int start, int end) {
		// std::cerr + "CanIgetFromAToB(" + start + "," + end + ")=" + distances[start][end] +
		// std::endl;
		return distances[start][end] < 100000;
	}

	public int read(BufferedReader in, final int[] factorOrder) throws IOException {
		clear();
		String line;
		if ((line = in.readLine()) == null)
			return 0;
		Map<String, String> meta = Util.processAndStripSGML(line);
		if (meta.get("id") != null) {
			this.base.setTranslationId(Long.valueOf(meta.get("id")));
		}
		int numLinkParams = StaticData.instance().getNumLinkParams();
		int numLinkWeights = StaticData.instance().getNumInputScores();
		int maxSizePhrase = StaticData.instance().getMaxPhraseLength();

		// when we have one more weight than params, we add a word count feature
		boolean addRealWordCount = ((numLinkParams + 1) == numLinkWeights);

		PCNTools.CN cn = PCNTools.parsePCN(line);
		// data.resize(cn.size());
		CollectionUtils.resize(data, cn.size(), null);
		// next_nodes.resize(cn.size());
		for (int i = 0; i < cn.size(); ++i) {
			PCNTools.CNCol col = cn.get(i);
			if (col.isEmpty())
				return 0;
			CollectionUtils.resize(data.get(i), col.size(), null);
			// data.get(i).resize(col.size());
			// CollectionUtils.resize(next_nodes[i],col.size(), null);
			next_nodes[i] = new int[col.size()];
			// next_nodes[i].resize(col.size());
			for (int j = 0; j < col.size(); ++j) {
				PCNTools.CNAlt alt = col.get(j);

				// check for correct number of link parameters
				if (alt.first.second.size() != numLinkParams) {
					TRACE.err("ERROR: need " + numLinkParams + " link parameters, found "
							+ alt.first.second.size() + " while reading column " + i + " from "
							+ line + "\n");
					return 0;
				}

				// check each element for bounds

				data.get(i).get(j).second = new float[alt.first.second.size() + 1];

				int k = 0;
				for (float probsIterator : alt.first.second) {
					if (probsIterator < 0.0f) {
						TRACE.err("WARN: neg probability: " + probsIterator + "\n");
						// *probsIterator = 0.0f;
					}
					if (probsIterator > 1.0f) {
						TRACE.err("WARN: probability > 1: " + probsIterator + "\n");
						// *probsIterator = 1.0f;
					}
					data.get(i).get(j).second[k++] = (Math.max((float) (Math.log(probsIterator)),
							TypeDef.LOWEST_SCORE));
				}
				// store 'real' word count in last feature if we have one more weight than we do arc
				// scores and not epsilon
				if (addRealWordCount) {
					// only add count if not epsilon
					float value = (alt.first.first == "" || alt.first.first == TypeDef.EPSILON) ? 0.0f
							: -1.0f;
					data.get(i).get(j).second[k++] = (value);
				}
				string2Word(alt.first.first, data.get(i).get(j).first, factorOrder);
				next_nodes[i][j] = alt.second;

				if (next_nodes[i][j] > maxSizePhrase) {
					TRACE.err("ERROR: Jump length " + next_nodes[i][j]
							+ " in word lattice exceeds maximum phrase length " + maxSizePhrase
							+ ".\n");
					TRACE.err("ERROR: Increase max-phrase-length to process this lattice.\n");
					return 0;
				}
			}
		}
		if (!cn.isEmpty()) {

			boolean[][] edges = getAsEdgeMatrix();
			// floyd_warshall(edges,distances);

			if (VERBOSE.v(2)) {
				TRACE.err("Shortest paths:\n");
				for (int i = 0; i < edges.length; ++i) {
					for (int j = 0; j < edges[i].length; ++j) {
						int d = distances[i][j];
						if (d > 99999) {
							d = -1;
						}
						TRACE.err("\t" + d);
					}
					TRACE.err("\n");
				}
			}
		}
		return !cn.isEmpty() ? 1 : 0;
	}

	/**
	 * Convert internal representation into an edge matrix
	 * 
	 * edges[1][2] means there is an edge from 1 to 2
	 */
	public boolean[][] getAsEdgeMatrix() {

		boolean[][] edges = new boolean[data.size() + 1][data.size() + 1];

		for (int i = 0; i < data.size(); ++i) {
			for (int j = 0; j < data.get(i).size(); ++j) {
				edges[i][i + next_nodes[i][j]] = true;
			}
		}
		return edges;
	}

	public final List<Word> getLabelList(int startPos, int endPos) {
		ASSERT.a(false);
		return (new ArrayList<Word>());
	}

}
