package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import yonee.utils.ASSERT;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       finalize
 */
public class DecodeGraph {

	protected List<DecodeStep> m_steps = new ArrayList<DecodeStep>();
	protected int m_position;
	protected int m_maxChartSpan;

	/**
	 * position: The position of this graph within the decode sequence.
	 **/
	public DecodeGraph(int position) {
	}

	// for chart decoding
	public DecodeGraph(int position, int maxChartSpan) {
		m_position = position;
		m_maxChartSpan = maxChartSpan;
	}

	// ! iterators
	public Collection<DecodeStep> c() {
		return this.m_steps;
	}

	public void finalize() throws Throwable {
		m_steps.clear();
		super.finalize();
	}

	// ! Add another decode step to the graph
	public void add(DecodeStep decodeStep) {
		m_steps.add(decodeStep);
	}

	public int getSize() {
		return m_steps.size();
	}

	public int getMaxChartSpan() {
		ASSERT.a(m_maxChartSpan != TypeDef.NOT_FOUND);
		return m_maxChartSpan;
	}

	public int getPosition() {
		return m_position;
	}

}
