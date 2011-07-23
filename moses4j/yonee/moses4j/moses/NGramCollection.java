package yonee.moses4j.moses;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author YONEE
 * @OK
 */
public class NGramCollection {

	// typedef std::map<const Factor*, NGramNode*> Collection;
	protected Map<Factor, NGramNode> m_collection = new HashMap<Factor, NGramNode>();

	protected void add(final Factor factor, final NGramNode ngramNode) {
		// NO CODE
	}

	public NGramCollection() {
	}

	public NGramNode getOrCreateNGram(final Factor factor) {
		NGramNode ngn = m_collection.get(factor);
		if (ngn == null) {
			ngn = new NGramNode();
			m_collection.put(factor, ngn);
		}
		return ngn;

	}

	public NGramNode getNGram(final Factor factor) {
		return m_collection.get(factor);

	}

}
