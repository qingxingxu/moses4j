package yonee.moses4j.moses;

import yonee.moses4j.moses.TypeDef.FactorDirection;

/**
 * 
 * @author YONEE
 * @OK<<
 * 
 */
public class Factor {
	// friend std::ostream& operator<<(std::ostream&, const Factor&);

	// FactorDirection m_direction;
	// FactorType m_factorType;
	protected final String m_ptrString;
	protected final int m_id;

	// ! protected constructor. only friend class, FactorCollection, is allowed to create Factor
	// objects
	protected Factor(FactorDirection direction, int factorType, final String factorString, int id) {
		m_ptrString = factorString;
		m_id = id;
	}

	// ! no id set. do not used to create new factors, only used for seeing if factor exists
	protected Factor(FactorDirection direction, int factorType, final String factorString) {
		m_ptrString = factorString;
		m_id = TypeDef.NOT_FOUND;
	}

	// ! returns whether this factor is part of the source ('Input') or target ('Output') language
	// inline FactorDirection GetFactorDirection() const
	// {
	// return m_direction;
	// }
	// ! index, FactorType. For example, 0=surface, 1=POS. The actual mapping is user defined
	// inline FactorType GetFactorType() const
	// {
	// return m_factorType;
	// }
	// ! original string representation of the factor
	public final String getString() {
		return m_ptrString;
	}

	// ! contiguous ID
	public final int getId() {
		return m_id;
	}

	/*
	 * //! Alternative comparison between factors. Not yet used
	 * inline unsigned int GetHash() const
	 * {
	 * unsigned int h=quick_hash((final char*)&m_direction, sizeof(FactorDirection), 0xc7e7f2fd);
	 * h=quick_hash((final char*)&m_factorType, sizeof(FactorType), h);
	 * h=quick_hash((final char*)&m_ptrString, sizeof(final String *), h);
	 * return h;
	 * }
	 */

	/**
	 * transitive comparison between 2 factors.
	 * -1 = less than
	 * +1 = more than
	 * 0 = same
	 * Used by operator< & operator==, as well as other classes
	 */
	public final int compare(final Factor compare) {
		return m_ptrString.compareTo(compare.m_ptrString);

	}

	// ! transitive comparison used for adding objects into FactorCollection
	public final boolean less(final Factor compare) {
		return compare(compare) < 0;
	}

	// quick equality comparison. Not used
	public boolean equals(final Factor compare) {
		//return this == compare;
		return compare(compare) == 0;
	}
	//TO_STRING
	public String toString() {
		return this.getString();
	}

}
