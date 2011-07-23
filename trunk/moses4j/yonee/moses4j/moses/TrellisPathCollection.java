package yonee.moses4j.moses;

import java.util.LinkedList;


/**
 * 
 * @author YONEE
 * @OK
 * 
 */
public class TrellisPathCollection extends LinkedList<TrellisPath> {

	private static final long serialVersionUID = 1L;

	/**
	 * 清除保留的数据
	 * @param newSize
	 */
	public void prune(int newSize) {
		int currSize = size();

		if (currSize <= newSize)
			return; // don't need to prune

		this.removeRange(newSize, this.size());

	}
}
