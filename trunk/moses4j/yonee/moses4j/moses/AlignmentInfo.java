package yonee.moses4j.moses;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import yonee.utils.Pair;

/**
 * 
 * @author YONEE
 * @OK
 * 
 */
public class AlignmentInfo {

	private static final long serialVersionUID = 1L;
	private List<Pair<Integer, Integer>> m_collection = new LinkedList<Pair<Integer, Integer>>();

	public List<Pair<Integer, Integer>> c() {
		return m_collection;
	}

	public void addAlignment(List<Pair<Integer, Integer>> alignmentPairs) {
		m_collection = alignmentPairs;
		Collections.sort(m_collection, new Comparator<Pair<Integer, Integer>>() {
			public int compare(Pair<Integer, Integer> arg0, Pair<Integer, Integer> arg1) {
				return arg0.first - arg1.second;
			}
		});
	}

	public String toString() {
		StringBuilder out = new StringBuilder();
		for (Pair<Integer, Integer> iter : this.m_collection) {
			out.append(iter.first).append("-").append(iter.second).append(" ");
		}
		return out.toString();
	}

}
