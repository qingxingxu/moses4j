package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK
 */
public class RecombinationInfo {

	public RecombinationInfo() {
	} // for std::vector

	public RecombinationInfo(int srcWords, float gProb, float bProb) {
		numSourceWords = srcWords;
		betterProb = gProb;
		worseProb = bProb;
	}

	public int numSourceWords;
	public float betterProb, worseProb;

}
