package yonee.utils;

/**
 * 
 * @author YONEE
 * @RISK
 *       读取流的间隔字符？
 */
public class StringReader {
	private String data;
	private int pos = 0;

	public StringReader(String s) {
		data = s;
		pos = 0;
	}

	public String getString() {
		StringBuilder s = new StringBuilder(20);
		for (; pos < data.length(); pos++) {
			if (data.charAt(pos) == ' ') {
				pos++;
				return s.toString();
			} else {
				s.append(data.charAt(pos));
			}

		}
		if (s.length() == 0) {
			return null;
		} else {
			return s.toString();
		}
	}

	public Double getDouble() {
		String s = getString();
		if (s != null) {
			Double.valueOf(s);
		}
		return null;
	}

	public Integer getInteger() {
		String s = getString();
		if (s != null) {
			Integer.valueOf(s);
		}
		return null;
	}
}
