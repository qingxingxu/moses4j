package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       nthElement compare
 */
public class TargetPhraseCollection extends ArrayList<TargetPhrase> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void nthElement(int tableLimit) {
		//int iterMiddle = (tableLimit == 0 || size() < tableLimit) ? size() - 1 : tableLimit;
		Collections.sort(this,  new Comparator<TargetPhrase>() {
			public int compare(TargetPhrase a, TargetPhrase b) {
				return a.getFutureScore() < b.getFutureScore() ? -1:1;
			}
		});
	}
	

	public void prune(boolean adhereTableLimit, int tableLimit) {
		nthElement(tableLimit);
		if (adhereTableLimit && size() > tableLimit) {
			this.removeRange(tableLimit, size());
		}
	}

}
