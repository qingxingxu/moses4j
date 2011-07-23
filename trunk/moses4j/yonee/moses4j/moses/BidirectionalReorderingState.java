package yonee.moses4j.moses;


import yonee.moses4j.moses.LexicalReorderingConfiguration.Direction;

/**
 * 
 * @author YONEE
 * @OK
 */
public class BidirectionalReorderingState extends LexicalReorderingState implements FFState {

	private LexicalReorderingState m_backward;
	private LexicalReorderingState m_forward;

	public BidirectionalReorderingState(final LexicalReorderingConfiguration config,
			final LexicalReorderingState bw, final LexicalReorderingState fw, int offset) {
		super(config, Direction.Bidirectional, offset);
		m_backward = bw;
		m_forward = fw;
	}

	public int compare(final FFState o) {
		if (o == this)
			return 0;

		final BidirectionalReorderingState other = (BidirectionalReorderingState) (o);
		if (m_backward.compare(other.m_backward) < 0)
			return -1;
		else if (m_backward.compare(other.m_backward) > 0)
			return 1;
		else
			return m_forward.compare(other.m_forward);
	}

	public LexicalReorderingState expand(final TranslationOption topt, float[] scores) {
		LexicalReorderingState newbwd = m_backward.expand(topt, scores);
		LexicalReorderingState newfwd = m_forward.expand(topt, scores);
		return new BidirectionalReorderingState(m_configuration, newbwd, newfwd, m_offset);
	}

}
