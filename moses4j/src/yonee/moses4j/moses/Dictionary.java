package yonee.moses4j.moses;

import yonee.moses4j.moses.TypeDef.DecodeType;

/**
 * 
 * @author YONEE
 * @OK
 */
public interface Dictionary {
	public class Impl {
		// /////////////////////////// Dictionary implement
		int m_numScoreComponent;
		FactorMask m_inputFactors = new FactorMask();
		FactorMask m_outputFactors = new FactorMask();

		// ! returns output factor types as specified by the ini file
		public FactorMask getOutputFactorMask() {
			return m_outputFactors;
		}

		// ! returns input factor types as specified by the ini file
		public FactorMask getInputFactorMask() {
			return m_inputFactors;
		}

		public void cleanUp() {

		}
	}


	public FactorMask getOutputFactorMask();

	public FactorMask getInputFactorMask();

	public DecodeType getDecodeType();

	public void cleanUp();
}
