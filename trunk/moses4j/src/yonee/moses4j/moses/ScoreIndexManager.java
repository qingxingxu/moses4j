package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yonee.utils.ASSERT;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class ScoreIndexManager {

	private List<Integer> m_begins = new ArrayList<Integer>();
	private List<Integer> m_ends = new ArrayList<Integer>();
	private List<ScoreProducer> m_producers = new ArrayList<ScoreProducer>();
	/** < all the score producers in this run */
	private List<StatefulFeatureFunction> m_stateful = new ArrayList<StatefulFeatureFunction>();
	/** < all the score producers in this run */
	private List<StatelessFeatureFunction> m_stateless = new ArrayList<StatelessFeatureFunction>();
	/** < all the score producers in this run */
	private List<String> m_featureNames = new ArrayList<String>();
	private List<String> m_featureShortNames = new ArrayList<String>();
	private int m_last;

	public ScoreIndexManager() {
		m_last = 0;
	}

	// ! new score producer to manage. Producers must be inserted in the order
	// they are created
	public void addScoreProducer(ScoreProducer sp) {
		// Producers must be inserted in the order they are created
		((ScoreProducer) (sp)).createScoreBookkeepingID();
		ASSERT.a(m_begins.size() == (sp.getScoreBookkeepingID()));

		m_producers.add(sp);
		if (sp.isStateless()) {
			final StatelessFeatureFunction ff = (StatelessFeatureFunction) (sp);
			if (!ff.computeValueInTranslationOption())
				m_stateless.add(ff);
		} else {
			m_stateful.add((StatefulFeatureFunction) (sp));
		}

		m_begins.add(m_last);
		int numScoreCompsProduced = sp.getNumScoreComponents();
		ASSERT.a(numScoreCompsProduced > 0);
		m_last += numScoreCompsProduced;
		m_ends.add(m_last);

	}

	public void initFeatureNames() {
		m_featureNames.clear();
		m_featureShortNames.clear();
		int cur_i = 0;
		int cur_scoreType = 0;
		while (cur_i < m_last) {
			int nis_idx = 0;
			boolean add_idx = (m_producers.get(cur_scoreType).getNumInputScores() > 1);
			while (nis_idx < m_producers.get(cur_scoreType).getNumInputScores()) {
				StringBuilder os = new StringBuilder();
				os.append(m_producers.get(cur_scoreType).getScoreProducerDescription());
				if (add_idx)
					os.append('_').append((nis_idx + 1));
				m_featureNames.add(os.toString());
				nis_idx++;
				cur_i++;
			}

			int ind = 1;
			add_idx = (m_ends.get(cur_scoreType) - cur_i > 1);
			while (cur_i < m_ends.get(cur_scoreType)) {
				StringBuilder os = new StringBuilder();
				os.append(m_producers.get(cur_scoreType).getScoreProducerDescription());
				if (add_idx)
					os.append('_').append(ind);
				m_featureNames.add(os.toString());
				m_featureShortNames.add(m_producers.get(cur_scoreType)
						.getScoreProducerWeightShortName());
				++cur_i;
				++ind;
			}
			cur_scoreType++;
		}
	}

	// ! starting score index for a particular score producer with
	// scoreBookkeepingID
	public int getBeginIndex(int scoreBookkeepingID) {
		return m_begins.get(scoreBookkeepingID);
	}

	// ! end score index for a particular score producer with scoreBookkeepingID
	public int getEndIndex(int scoreBookkeepingID) {
		return m_ends.get(scoreBookkeepingID);
	}

	// ! sum of all score components from every score producer
	public int getTotalNumberOfScores() {
		return m_last;
	}

	// ! print unweighted scores of each ScoreManager to stream os
	public void printLabeledScores(Writer os, final ScoreComponentCollection scores) {
		// List<Float> weights = new ArrayList<Float>(scores.m_scores.size(),
		// 1.0f);
		// printLabeledWeightedScores(os, scores, weights);

	}

	// ! print weighted scores of each ScoreManager to stream os
	public void printLabeledWeightedScores(PrintStream os, final ScoreComponentCollection scc,
			final List<Float> weights) {

	}

	// #ifdef HAVE_PROTOBUF
	// void SerializeFeatureNamesToPB(hgmert::Hypergraph* hg) const;
	// #endif
	public void initWeightVectorFromFile(final String fnam, List<Float> m_allWeights) {
		ASSERT.a(m_allWeights.size() == m_featureNames.size());
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fnam));

			// ASSERT.a(in.good());
			// char buf[] = new char[2000];
			Map<String, Double> name2val = new HashMap<String, Double>();
			String line = null;
			while ((line = br.readLine()) != null) {

				if (line.trim().length() == 0)
					continue;
				if (line.charAt(0) == '#')
					continue;
				String s[] = line.split(" ");
				ASSERT.a(s.length != 2);

				String fname = s[0];// YONEE
				double val = Double.valueOf(s[1]);

				Double v = name2val.get(fname);
				ASSERT.a(v != null);
				name2val.put(fname, val);
			}
			ASSERT.a(m_allWeights.size() == m_featureNames.size());
			for (int i = 0; i < m_featureNames.size(); ++i) {
				// Map<string, double>::iterator iter =
				// name2val.find(m_featureNames[i]);
				Double v = name2val.get(m_featureNames.get(i));
				if (v == null) {
					System.err.println("No weight found found for feature: "
							+ m_featureNames.get(i));
					System.exit(0);
				}
				m_allWeights.set(i, v.floatValue());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					//
				}
			}
		}
	}

	public List<ScoreProducer> getFeatureFunctions() {
		return m_producers;
	}

	public List<StatefulFeatureFunction> getStatefulFeatureFunctions() {
		return m_stateful;
	}

	public List<StatelessFeatureFunction> getStatelessFeatureFunctions() {
		return m_stateless;
	}

	public String toString() {
		StringBuilder os = new StringBuilder();
		for (String s : m_featureNames) {
			os.append(s).append("\n");
		}
		os.append("Stateless: ").append(m_stateless.size()).append("\tStateful: ").append(
				m_stateful.size()).append("\n");
		return os.toString();
	}

}
