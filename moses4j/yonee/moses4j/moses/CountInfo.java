package yonee.moses4j.moses;

/**
 * 
 * @author YONEE
 * @OK
 */
public class CountInfo {

	public CountInfo() {
	}

	public CountInfo(float countSource, float countTarget)

	{
		m_countSource = countSource;
		m_countTarget = countTarget;
	}

	public float m_countSource;
	public float m_countTarget;
}
