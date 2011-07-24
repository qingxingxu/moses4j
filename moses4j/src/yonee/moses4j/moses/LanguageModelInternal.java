package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.ASSERT;
import yonee.utils.Ref;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @NOT
 */
public class LanguageModelInternal extends LanguageModelSingleFactor {

	protected NGramNode[] m_lmIdLookup ;//= new ArrayList<NGramNode>();
	protected NGramCollection m_map = new NGramCollection();

	protected final NGramNode getLmID(final Factor factor) {
		int factorId = factor.getId();
		return (factorId >= m_lmIdLookup.length) ? null : m_lmIdLookup[factorId];
	};

	protected float getValue(final Factor factor0, Ref<Object> finalState) {
		float prob;
		final NGramNode nGram = getLmID(factor0);
		if (nGram == null) {
			if (finalState != null)
				finalState.v = null;
			prob = Float.NEGATIVE_INFINITY;
		} else {
			if (finalState != null)
				finalState.v = nGram;
			prob = nGram.getScore();
		}
		return Util.floorScore(prob);
	}

	protected float getValue(final Factor factor0, final Factor factor1, Ref<Object> finalState) {
		float score;
		NGramNode[] nGram = new NGramNode[2];

		nGram[1] = getLmID(factor1);
		if (nGram[1] == null) {
			if (finalState != null)
				finalState.v = null;
			score = Float.NEGATIVE_INFINITY;
		} else {
			nGram[0] = nGram[1].getNGram(factor0);
			if (nGram[0] == null) { // something unigram
				if (finalState != null)
					finalState.v = nGram[1];

				nGram[0] = getLmID(factor0);
				if (nGram[0] == null) { // stops at unigram
					score = nGram[1].getScore();
				} else { // unigram unigram
					score = nGram[1].getScore() + nGram[0].getLogBackOff();
				}
			} else { // bigram
				if (finalState != null)
					finalState.v = nGram[0];
				score = nGram[0].getScore();
			}
		}

		return Util.floorScore(score);
	}

	protected float getValue(final Factor factor0, final Factor factor1, final Factor factor2,
			Ref<Object> finalState) {
		float score;
		NGramNode[] nGram = new NGramNode[3];

		nGram[2] = getLmID(factor2);
		if (nGram[2] == null) {
			if (finalState != null)
				finalState.v = null;
			score = Float.NEGATIVE_INFINITY;
		} else {
			nGram[1] = nGram[2].getNGram(factor1);
			if (nGram[1] == null) { // something unigram
				if (finalState != null)
					finalState.v = nGram[2];

				nGram[1] = getLmID(factor1);
				if (nGram[1] == null) { // stops at unigram
					score = nGram[2].getScore();
				} else {
					nGram[0] = nGram[1].getNGram(factor0);
					if (nGram[0] == null) { // unigram unigram
						score = nGram[2].getScore() + nGram[1].getLogBackOff();
					} else { // unigram bigram
						score = nGram[2].getScore() + nGram[1].getLogBackOff()
								+ nGram[0].getLogBackOff();
					}
				}
			} else { // trigram, or something bigram
				nGram[0] = nGram[1].getNGram(factor0);
				if (nGram[0] != null) { // trigram
					if (finalState != null)
						finalState.v = nGram[0];
					score = nGram[0].getScore();
				} else {
					if (finalState != null)
						finalState.v = nGram[1];

					score = nGram[1].getScore();
					nGram[1] = nGram[1].getRootNGram();
					nGram[0] = nGram[1].getNGram(factor0);
					if (nGram[0] == null) { // just bigram
						// do nothing
					} else {
						score += nGram[0].getLogBackOff();
					}

				}
				// else do nothing. just use 1st bigram
			}
		}
		return Util.floorScore(score);
	}

	public float getValue(final List<Word> contextFactor, Ref<Object> finalState// null
			, Ref<Integer> len) {// null
		final int ngram = contextFactor.size();
		switch (ngram) {
		case 1:
			return getValue(contextFactor.get(0).get(m_factorType), finalState);
		case 2:
			return getValue(contextFactor.get(0).get(m_factorType), contextFactor.get(1).get(
					m_factorType), finalState);
		case 3:
			return getValue(contextFactor.get(0).get(m_factorType), contextFactor.get(1).get(
					m_factorType), contextFactor.get(2).get(m_factorType), finalState);
		}

		ASSERT.a(false);
		return 0;
	}

	public LanguageModelInternal(boolean registerScore, ScoreIndexManager scoreIndexManager) {
		super(registerScore, scoreIndexManager);
	}

	public boolean load(final String filePath, int factorType, float weight, int nGramOrder)
			throws IOException {
		ASSERT.a(nGramOrder <= 3);
		if (nGramOrder > 3) {
			UserMessage.add("Can only do up to trigram. Aborting");
			System.exit(0);
		}

		VERBOSE.v(1, "Loading Internal LM: " + filePath + "\n");

		FactorCollection factorCollection = FactorCollection.instance();

		m_filePath = filePath;
		m_factorType = factorType;
		m_weight = weight;
		m_nGramOrder = nGramOrder;

		// make sure start & end tags in factor collection
		m_sentenceStart = factorCollection.addFactor(FactorDirection.Output, m_factorType,
				TypeDef.BOS_);
		m_sentenceStartArray.set(m_factorType, m_sentenceStart);

		m_sentenceEnd = factorCollection.addFactor(FactorDirection.Output, m_factorType,
				TypeDef.EOS_);
		m_sentenceEndArray.set(m_factorType, m_sentenceEnd);

		// read in file
		VERBOSE.v(1, filePath + "\n");
		BufferedReader br = null;
		if (filePath.endsWith(".gz")) {
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(
					filePath))));
		} else {
			br = new BufferedReader(new FileReader(filePath));
		}

		// to create lookup vector later on
		int maxFactorId = 0;
		Map<Integer, NGramNode> lmIdMap = new HashMap<Integer, NGramNode>();

		String line;
		int lineNo = 0;

		while ((line = br.readLine()) != null) {
			lineNo++;

			if (line.length() != 0 && line.substring(0, 1) != "\\") {
				String[] tokens = Util.tokenize(line, "\t");
				if (tokens.length >= 2) {
					// split unigram/bigram trigrams
					String[] factorStr = Util.tokenize(tokens[1], " ");

					// create / traverse down tree
					NGramCollection ngramColl = m_map;
					NGramNode nGram = null;
					Factor factor = null;
					for (int currFactor = (int) factorStr.length - 1; currFactor >= 0; currFactor--) {
						factor = factorCollection.addFactor(FactorDirection.Output, m_factorType,
								factorStr[currFactor]);
						nGram = ngramColl.getOrCreateNGram(factor);

						ngramColl = nGram.getNGramColl();

					}

					NGramNode rootNGram = m_map.getNGram(factor);
					nGram.setRootNGram(rootNGram);

					// create vector of factors used in this LM
					int factorId = factor.getId();
					maxFactorId = (factorId > maxFactorId) ? factorId : maxFactorId;
					lmIdMap.put(factorId, rootNGram);
					// factorCollection.SetFactorLmId(factor, rootNGram);

					float score = Util.transformLMScore(Float.valueOf(tokens[0]));
					nGram.setScore(score);
					if (tokens.length == 3) {
						float logBackOff = Util.transformLMScore(Float.valueOf(tokens[2]));
						nGram.setLogBackOff(logBackOff);
					} else {
						nGram.setLogBackOff(0);
					}
				}
			}
		}

		br.close();

		m_lmIdLookup = new NGramNode[maxFactorId + 1];

		for (Map.Entry<Integer, NGramNode> e : lmIdMap.entrySet()) {
			m_lmIdLookup[e.getKey()] =  e.getValue();
		}

		return true;
	}

}
