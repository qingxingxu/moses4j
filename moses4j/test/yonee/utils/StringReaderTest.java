package yonee.utils;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

public class StringReaderTest {

	@Test
	public void testGetString() throws IOException {
		String except0[] = new String[] { "a", "bb", "ccc", "dddd" };
		Integer except1[] = new Integer[] { 0, 11, 22, 333, 4444 };
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader("e:\\a.txt"));
			String line = null;
			for (int i = 0; (line = in.readLine()) != null; i++) {
				StringReader sr = new StringReader(line);

				if (i == 0) {
					String s = null;
					for (int j = 0; (s = sr.getString()) != null; j++) {
						Assert.assertEquals(except0[j], s);
					}
				} else if (i == 1) {
					Integer s = null;
					for (int j = 0; (s = sr.getInteger()) != null; j++) {
						Assert.assertEquals(except1[j], s);
					}
				}
			}
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					Assert.fail(e.getMessage());
				}
		}
	}
}
