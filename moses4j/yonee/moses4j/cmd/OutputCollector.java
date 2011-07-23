package yonee.moses4j.cmd;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author YONEE
 * @OK
 */
public class OutputCollector {

	private Map<Integer, String> m_outputs = new HashMap<Integer,String>();
	private Map<Integer, String> m_debugs = new HashMap<Integer,String>();
	private int m_nextOutput;
	private Writer m_outStream;
	private Writer m_debugStream;

	// #ifdef WITH_THREADS
	// boost::mutex m_mutex;
	// #endif

	public OutputCollector() {
		this(new PrintWriter(System.out), new PrintWriter(System.err));
	}

	public OutputCollector(Writer outStream) {
		this(outStream, new PrintWriter(System.err));
	}

	public OutputCollector(Writer outStream, Writer debugStream) {
		m_nextOutput = 0;
		m_outStream = outStream;
		m_debugStream = debugStream;
	}

	/**
	 * Write or cache the output, as appropriate.
	 * 
	 * @throws IOException
	 **/
	public void write(int sourceId, final String output) throws IOException {
		write(sourceId, output, "");
	}

	public void write(int sourceId, final String output, final String debug) throws IOException {
		if (sourceId == m_nextOutput) {
			// This is the one we were expecting
			m_outStream.append(output).flush();
			m_debugStream.append(debug).flush();
			++m_nextOutput;
			// see if there's any more

			String iter = null;
			while ((iter = m_outputs.get(m_nextOutput)) != null) {
				m_outStream.append(iter);
				m_outputs.remove(m_nextOutput);
				++m_nextOutput;
				String iterDebug = m_debugs.get(m_nextOutput);
				if (iterDebug != null) {
					m_debugStream.append(iterDebug);
					m_debugs.remove(m_nextOutput);
				}
			}
		} else {
			// save for later
			m_outputs.put(sourceId, output);
			m_debugs.put(sourceId, debug);
		}
	}

}
