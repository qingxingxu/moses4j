package yonee.moses4j.moses;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class WordTest {
	@Test
	public void testMap() {
		Map<Word, String> m = new HashMap<Word,String>();
		m.put(new Word(), "1");
		m.put( new Word(), "2");
		System.out.println(m.get(new Word()));
		
		
	}
}
