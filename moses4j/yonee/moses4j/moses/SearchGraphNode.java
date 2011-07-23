package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK
 */
public class SearchGraphNode {

	public SearchGraphNode(final Hypothesis theHypo, Hypothesis theRecombinationHypo,
			int theForward, double theFscore) {
		hypo = theHypo;
		recombinationHypo = theRecombinationHypo;
		forward = theForward;
		fscore = theFscore;
	}

	public Hypothesis hypo;
	public int forward;
	public double fscore;
	public Hypothesis recombinationHypo;

}
