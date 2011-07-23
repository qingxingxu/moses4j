package yonee.moses4j.moses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class PhraseDictionaryNode {

	private static final long serialVersionUID = 1L;

	protected Map<Word, PhraseDictionaryNode> m_map = new HashMap<Word, PhraseDictionaryNode>();

	public Set<Map.Entry<Word, PhraseDictionaryNode>> entrySet() {
		return m_map.entrySet();
	}

	protected TargetPhraseCollection m_targetPhraseCollection;

	protected PhraseDictionaryNode() {
		m_targetPhraseCollection = null;
	}

	public void sort(int tableLimit) {
		// recusively sort
		for (Map.Entry<Word, PhraseDictionaryNode> pdn : m_map.entrySet()) {
			pdn.getValue().sort(tableLimit);
		}
		// sort TargetPhraseCollection in this node
		if (m_targetPhraseCollection != null)
			m_targetPhraseCollection.nthElement(tableLimit);
	}

	public PhraseDictionaryNode getOrCreateChild(final Word word) {
		PhraseDictionaryNode pdn = m_map.get(word);
		if (pdn != null) {
			return pdn;
		}
		// can't find node. create a new 1
		pdn = new PhraseDictionaryNode();
		m_map.put(word, pdn);
		return pdn;

	}

	public PhraseDictionaryNode getChild(final Word word) {
		PhraseDictionaryNode pdn = m_map.get(word);
		if (pdn != null) {
			return pdn;
		}
		// don't return anything
		return null;
	}

	public TargetPhraseCollection getTargetPhraseCollection() {
		return m_targetPhraseCollection;
	}

	public TargetPhraseCollection createTargetPhraseCollection() {
		if (m_targetPhraseCollection == null)
			m_targetPhraseCollection = new TargetPhraseCollection();
		return m_targetPhraseCollection;
	}

	// for mert
	public void setWeightTransModel(final PhraseDictionaryMemory phraseDictionary,
			List<Float> weightT) {
		// recursively set weights
		for (Map.Entry<Word, PhraseDictionaryNode> e : m_map.entrySet()) {
			e.getValue().setWeightTransModel(phraseDictionary, weightT);
		}

		// set wieghts for this target phrase
		if (m_targetPhraseCollection == null)
			return;

		for (TargetPhrase targetPhrase : m_targetPhraseCollection) {
			targetPhrase.setWeights(phraseDictionary.getFeature(), weightT);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<Word, PhraseDictionaryNode> e : m_map.entrySet()) {
			sb.append( e.getKey() ).append("->").append(e.getValue().toString());
		}
		sb.append("\n");
		return sb.toString();
	}

}
