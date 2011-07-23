package yonee.moses4j.moses;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import java.util.Set;

import yonee.moses4j.moses.TypeDef.FactorDirection;

/**
 * 
 * @author YONEE
 * @OK not verify
 * 
 */
public class FactorCollection {
	protected static FactorCollection s_instance = new FactorCollection();

	protected int m_factorId;
	/** < unique, contiguous ids, starting from 0, for each factor */
	protected Set<Factor> m_collection = new HashSet<Factor>();
	/** < collection of all factors */
	protected Set<String> m_factorStringCollection = new HashSet<String>();;

	/** < collection of unique string used by factors */
	protected FactorCollection() {
		m_factorId = 0;
	}

	public static FactorCollection instance() {
		return s_instance;
	}

	// ! Test to see whether a factor exists
	public boolean exists(FactorDirection direction, int factorType, final String factorString) {
		/*
		 * #ifdef WITH_THREADS boost::shared_lock<boost::shared_mutex>
		 * lock(m_accessLock); #endif
		 */
		// find string id
		m_factorStringCollection.add(factorString);
		return m_collection.contains(new Factor(direction, factorType, factorString));

	}

	/**
	 * returns a factor with the same direction, factorType and factorString. If
	 * a factor already exist in the collection, return the existing factor, if
	 * not create a new 1 C++ set特性：插入如果存在返回second为false; first为值
	 */
	public Factor addFactor(FactorDirection direction, int factorType, final String factorString) {
		// #ifdef WITH_THREADS
		// boost::upgrade_lock<boost::shared_mutex> lock(m_accessLock);
		// boost::upgrade_to_unique_lock<boost::shared_mutex> uniqueLock(lock);
		// #endif
		// find string id
		m_factorStringCollection.add(factorString);
		Factor factor = new Factor(direction, factorType, factorString, m_factorId);
		boolean exists = m_collection.add(factor);

		if (exists)
			++m_factorId; // new factor, make sure next new factor has diffrernt
		// id

		return factor;
	}

	// ! Load list of factors. Deprecated
	public void loadVocab(FactorDirection direction, int factorType, final String filePath)
			throws IOException {

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath));
			String line;

			while ((line = br.readLine()) != null) {
				String[] token = Util.tokenize(line);
				if (token.length < 2) {
					continue;
				}
				// looks like good line
				addFactor(direction, factorType, token[1]);
			}
		} finally {
			if (br != null) {
				br.close();
			}
		}

	}

}
