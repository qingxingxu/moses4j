package yonee.moses4j.moses;

import java.util.BitSet;
import java.util.List;
/**
 * 
 * @author YONEE
 * @OK not verify
 * 
 */
public class FactorMask extends BitSet {

	private static final long serialVersionUID = 1L;

	// ! construct object from list of FactorType.
	public FactorMask(final List<Integer> factors) {
		for (int i : factors) {
			this.set(i);
		}
	}

	public FactorMask(int[] factors) {
		for (int i : factors) {
			this.set(i);
		}
	}

	// ! default constructor
	public FactorMask() {
		super();
	}

	// ! copy constructor
	public FactorMask(final BitSet rhs) {
		// : std::bitset<MAX_NUM_FACTORS>(rhs) { }
		super(TypeDef.MAX_NUM_FACTORS);
		this.or(rhs);
	}

	// TO_STRING();

}
