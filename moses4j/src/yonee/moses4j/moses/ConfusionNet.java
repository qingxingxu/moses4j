package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.utils.ASSERT;
import yonee.utils.CollectionUtils;
import yonee.utils.Pair;
import yonee.utils.StringReader;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @REST<<
 * @RISK
 *       析构函数finalize实现
 */
public class ConfusionNet implements InputType {

	//
	class CNStats {
		public int created, destr, read, colls, words;

		public CNStats() {

		}

		public void createOne() {
			++created;
		}

		public void destroyOne() {
			++destr;
		}

		public void collect(final ConfusionNet cn) {
			++read;
			colls += cn.getSize();
			for (int i = 0; i < cn.getSize(); ++i)
				words += cn.get(i).size();
		}

		void print(PrintStream out) {

			if (created > 0) {
				out.append("confusion net statistics:\n").append("created:\t" + created + "\n")
						.append(" destroyed:\t" + destr + "\n").append(
								" succ. read:\t" + read + "\n")
						.append(" columns:\t" + colls + "\n").append(" words:\t" + words + "\n")
						.append(" avg. word/column:\t" + words / (1.0 * colls) + "\n").append(
								" avg. cols/sent:\t" + colls / (1.0 * read) + "\n").append("\n\n");
			}
		}

	};

	public CNStats stats = new CNStats();

	// ---------------------

	public class Column extends ArrayList<Pair<Word, float[]>> {
		private static final long serialVersionUID = 1L;

	}

	
	//
	InputType.Impl base = new InputType.Impl();

	public InputType.Impl i() {
		return this.base;
	}
	//
	
	protected List<Column> data = new ArrayList<Column>();

	protected boolean readFormat0(BufferedReader in, final int[] factorOrder)
			throws IOException {
		clear();
		String line;

		int numLinkParams = StaticData.instance().getNumLinkParams();
		int numLinkWeights = StaticData.instance().getNumInputScores();
		boolean addRealWordCount = ((numLinkParams + 1) == numLinkWeights);

		while ((line = in.readLine()) != null) {
			StringReader is = new StringReader(line);
			String word;

			Column col = new Column();
			while ((word = is.getString()) != null) {
				Word w = new Word();
				string2Word(word, w, factorOrder);
				float[] probs = new float[numLinkWeights];
				for (int i = 0; i < numLinkParams; i++) {
					Double prob;
					if ((prob = is.getDouble()) == null) {
						TRACE
								.err("ERROR: unable to parse CN input - bad link probability, or wrong number of scores\n");
						return false;
					}
					if (prob < 0.0) {
						VERBOSE.v(1, "WARN: negative prob: " + prob + " ->set to 0.0\n");
						prob = 0.0;
					} else if (prob > 1.0) {
						VERBOSE.v(1, "WARN: prob > 1.0 : " + prob + " -> set to 1.0\n");
						prob = 1.0;
					}
					probs[i] = (Math.max((float) (Math.log(prob)), TypeDef.LOWEST_SCORE));

				}
				// store 'real' word count in last feature if we have one more weight than we do arc
				// scores and not epsilon
				if (addRealWordCount && !word.equals(TypeDef.EPSILON) && "".equals(word))
					probs[numLinkParams] = -1.0f;
				col.add(new Pair<Word, float[]>(w, probs));
			}
			if (col.size() > 0) {
				data.add(col);
				Util.shrinkToFit(data.get(data.size() - 1));
			} else
				break;
		}
		return !data.isEmpty();
	}

	protected boolean readFormat1(BufferedReader in, final int[] factorOrder)
			throws IOException {
		clear();
		String line;
		if ((line = in.readLine()) == null)
			return false;

		Integer s = 0;
		if ((line = in.readLine()) != null)
			s = Integer.valueOf(line);
		else
			return false;
		CollectionUtils.resize(data, s,null);

		for (int i = 0; i < data.size(); ++i) {
			if ((line = in.readLine()) == null)
				return false;

			StringReader is = new StringReader(line);
			if ((s = is.getInteger()) == null)
				return false;
			String word;
			Double prob;
			CollectionUtils.resize(data.get(i), s,null);
			for (int j = 0; j < s; ++j)
				if ((word = is.getString()) != null) {
					if ((prob = is.getDouble()) != null) {

						// TODO: we are only reading one prob from this input format, should read
						// many... but this function is unused anyway. -JS
						data.get(i).get(j).second = new float[1];
						data.get(i).get(j).second[0] = (float) Math.log(prob);

						if (data.get(i).get(j).second[0] < 0) {
							VERBOSE.v(1, "WARN: neg costs: " + data.get(i).get(j).second[0]
									+ " -> set to 0\n");
							data.get(i).get(j).second[0] = 0.0f;
						}
						string2Word(word, data.get(i).get(j).first, factorOrder);
					}
				} else
					return false;
		}
		return !data.isEmpty();
	}

	protected void string2Word(final String s, Word w, final int[] factorOrder) {
		String[] factorStrVector = Util.tokenize(s, "|");
		for (int i = 0; i < factorOrder.length; ++i)
			w.setFactor(factorOrder[i], FactorCollection.instance().addFactor(
					FactorDirection.Input, factorOrder[i], factorStrVector[i]));
	}

	public ConfusionNet() {
		stats.createOne();
	}

	public void finalize() throws Throwable {
		stats.destroyOne();
		super.finalize();
	}

	public ConfusionNet(Sentence s) {
		CollectionUtils.resize(data, s.getSize(), Column.class);

		for (int i = 0; i < s.getSize(); ++i)
			data.get(i).add(new Pair<Word, float[]>(s.getWord(i), null));
	}

	public InputTypeEnum getType() {
		return InputTypeEnum.ConfusionNetworkInput;
	}

	public final Column getColumn(int i) {
		assert (i < data.size());
		return data.get(i);
	}

	public final Column get(int i) {

		return getColumn(i);
	}

	public int getColumnIncrement(int i, int j) {

		return 1;
	}

	public boolean empty() {
		return data.isEmpty();
	}

	public int getSize() {
		return data.size();
	}

	public void clear() {
		data.clear();
	}

	public boolean readF(BufferedReader is, final int[]factorOrder) throws IOException {
		return readF(is, factorOrder, 0);
	}

	public boolean readF(BufferedReader in, final int[] factorOrder, int format)
			throws IOException {
		VERBOSE.v(1, "read confusion net with format " + format + "\n");
		switch (format) {
		case 0:
			return readFormat0(in, factorOrder);
		case 1:
			return readFormat1(in, factorOrder);
		default:
			StringBuilder strme = new StringBuilder();
			strme.append("ERROR: unknown format '").append(format)
					.append("' in ConfusionNet::Read");
			UserMessage.add(strme.toString());
		}
		return false;
	}

	public void print(PrintStream out) {
		out.print("conf net: " + data.size() + "\n");
		for (int i = 0; i < data.size(); ++i) {
			out.print(i + " -- ");
			for (int j = 0; j < data.get(i).size(); ++j) {
				out.print("(" + data.get(i).get(j).first.toString() + ", ");
				for (float f : data.get(i).get(j).second) {
					out.print(", " + f);
				}

				out.print(") ");
			}
			out.print("\n");
		}
		out.print("\n\n");
	}

	public int read(BufferedReader in, final int[] factorOrder) throws IOException {
		boolean rv = readF(in, factorOrder, 0);
		if (rv)
			stats.collect(this);
		return rv ? 1 : 0;
	}

	public Phrase getSubString(final WordsRange wr) {
		TRACE.err("ERROR: call to ConfusionNet::GetSubString\n");
		System.exit(0);
		return null;
	}

	public String getStringRep(final List<Integer> factorsToPrint) {
		TRACE.err("ERROR: call to ConfusionNet::GeStringRep\n");
		return "";
	}

	public final Word getWord(int pos) {
		TRACE.err("ERROR: call to ConfusionNet::GetFactorArray\n");
		System.exit(0);
		return null;
	}

	public TranslationOptionCollection createTranslationOptionCollection() {
		int maxNoTransOptPerCoverage = StaticData.instance().getMaxNoTransOptPerCoverage();
		float translationOptionThreshold = StaticData.instance().getTranslationOptionThreshold();
		TranslationOptionCollection rv = new TranslationOptionCollectionConfusionNet(this,
				maxNoTransOptPerCoverage, translationOptionThreshold);
		ASSERT.a(rv != null);
		return rv;
	}

	public  List<Word> getLabelList(int startPos, int endPos) {
		ASSERT.a(false);
		return (new ArrayList<Word>());
	}

	
}
