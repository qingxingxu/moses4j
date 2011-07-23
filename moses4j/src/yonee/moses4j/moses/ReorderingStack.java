package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class ReorderingStack {

	private List<WordsRange> m_stack = new ArrayList<WordsRange>();

	private boolean less(List<WordsRange> a, List<WordsRange> b) {
		int size = a.size() < b.size() ? a.size() : b.size();
		for (int i = 0; i < size; i++) {
			if (a.get(i).less(b.get(i))) {// 小于
				return true;
			} else if (b.get(i).less(a.get(i))) {// 大于
				return false;
			}
		}
		return false;
	}

	public int compare(final ReorderingStack o) {
		final ReorderingStack other = (ReorderingStack) (o);
		if (less(m_stack, other.m_stack)) {
			return 1;
		} else if (less(other.m_stack, m_stack)) {
			return -1;
		}
		return 0;
	}

	public int shiftReduce(WordsRange input_span) {
		int distance; // value to return: the initial distance between this and previous span

		// stack is empty
		if (m_stack.isEmpty()) {
			m_stack.add(input_span);
			return input_span.getStartPos() + 1; // - (-1)
		}

		// stack is non-empty
		WordsRange prev_span = m_stack.get(m_stack.size() - 1); // access last element added

		// calculate the distance we are returning
		if (input_span.getStartPos() > prev_span.getStartPos()) {
			distance = input_span.getStartPos() - prev_span.getEndPos();
		} else {
			distance = input_span.getEndPos() - prev_span.getStartPos();
		}

		if (distance == 1) { // monotone
			m_stack.remove(m_stack.size() - 1);
			WordsRange new_span = new WordsRange(prev_span.getStartPos(), input_span.getEndPos());
			reduce(new_span);
		} else if (distance == -1) { // swap
			m_stack.remove(m_stack.size() - 1);
			WordsRange new_span = new WordsRange(input_span.getStartPos(), prev_span.getEndPos());
			reduce(new_span);
		} else { // discontinuous
			m_stack.add(input_span);
		}

		return distance;
	}

	private void reduce(WordsRange current) {
		boolean cont_loop = true;

		while (cont_loop && m_stack.size() > 0) {

			WordsRange previous = m_stack.get(m_stack.size() - 1);

			if (current.getStartPos() - previous.getEndPos() == 1) { // mono&merge
				m_stack.remove(m_stack.size() - 1);
				WordsRange t = new WordsRange(previous.getStartPos(), current.getEndPos());
				current = t;
			} else if (previous.getStartPos() - current.getEndPos() == 1) { // swap&merge
				m_stack.remove(m_stack.size() - 1);
				WordsRange t = new WordsRange(current.getStartPos(), previous.getEndPos());
				current = t;
			} else { // discontinuous, no more merging
				cont_loop = false;
			}
		} // finished reducing, exit

		// add to stack
		m_stack.add(current);
	}

}
