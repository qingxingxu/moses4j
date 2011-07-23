package yonee.moses4j.pcn;

import java.util.ArrayList;
import java.util.List;

import yonee.utils.Pair;

public class PCNTools {
	public static class CNAlt extends Pair<Pair<String, List<Float>>, Integer> {

		public CNAlt(Pair<String, List<Float>> first, Integer second) {
			super(first, second);
		}

	}

	public static class CNCol extends ArrayList<CNAlt> {

		private static final long serialVersionUID = 1L;

	}

	public static class CN extends ArrayList<CNCol> {

		private static final long serialVersionUID = 1L;

	}

	public static final String chars = "'\\";
	public static final char quote = chars.charAt(0);
	public static final char slash = chars.charAt(1);

	// safe get
	public static final char get(final String in, int c) {
		if (c < 0 || c >= (int) in.length())
			return 0;
		else
			return in.charAt((int) c);
	}

	// consume whitespace
	public static final void eatws(final String in, int c) {
		while (get(in, c) == ' ') {
			c++;
		}
	}

	// from 'foo' return foo
	public static String getEscapedString(final String in, int c) {
		eatws(in, c);
		if (get(in, c++) != quote)
			return "ERROR";
		String res = "";
		char cur = 0;
		do {
			cur = get(in, c++);
			if (cur == slash) {
				res += get(in, c++);
			} else if (cur != quote) {
				res += cur;
			}
		} while (get(in, c) != quote && (c < (int) in.length()));
		c++;
		eatws(in, c);
		return res;
	}

	// basically atof
	public static float getFloat(final String in, int c) {
		String tmp = "";
		eatws(in, c);
		while (c < (int) in.length() && get(in, c) != ' ' && get(in, c) != ')' && get(in, c) != ',') {
			tmp += get(in, c++);
		}
		eatws(in, c);
		return Float.valueOf(tmp);
	}

	// basically atof
	public static int getInt(final String in, int c) {
		String tmp = "";
		eatws(in, c);
		while (c < (int) in.length() && get(in, c) != ' ' && get(in, c) != ')' && get(in, c) != ',') {
			tmp += get(in, c++);
		}
		eatws(in, c);
		return Integer.valueOf(tmp);
	}

	// parse ('foo', 0.23)
	public static CNAlt getCNAlt(final String in, int c) {
		if (get(in, c++) != '(') {
			System.err.print("PCN/PLF parse error: expected ( at start of cn alt block\n");
			return new CNAlt(null, null);
		} // throw "expected (";
		String word = getEscapedString(in, c);
		if (get(in, c++) != ',') {
			System.err.print("PCN/PLF parse error: expected , after string\n");
			return new CNAlt(null, null);
		} // throw "expected , after string";
		int cnNext = 1;
		List<Float> probs = new ArrayList<Float>();
		probs.add(getFloat(in, c));
		while (get(in, c) == ',') {
			c++;
			float val = getFloat(in, c);
			probs.add(val);
		}
		// if we read more than one prob, this was a lattice, last item was column increment
		if (probs.size() > 1) {
			cnNext = (probs.get(probs.size() - 1)).intValue();
			probs.remove(probs.size() - 1);
			if (cnNext < 1) {
				;
				System.err
						.print("PCN/PLF parse error: bad link length at last element of cn alt block\n");
				return new CNAlt(null, null);
			} // throw "bad link length"
		}
		if (get(in, c++) != ')') {
			System.err.print("PCN/PLF parse error: expected ) at end of cn alt block\n");
			return new CNAlt(null, null);
		} // throw "expected )";
		eatws(in, c);
		return new CNAlt(new Pair<String, List<Float>>(word, probs), cnNext);
	}

	// parse (('foo', 0.23), ('bar', 0.77))
	public static CNCol getCNCol(final String in, int c) {
		CNCol res = new CNCol();
		if (get(in, c++) != '(')
			return res; // error
		eatws(in, c);
		while (true) {
			if (c > (int) in.length()) {
				break;
			}
			if (get(in, c) == ')') {
				c++;
				eatws(in, c);
				break;
			}
			if (get(in, c) == ',' && get(in, c + 1) == ')') {
				c += 2;
				eatws(in, c);
				break;
			}
			if (get(in, c) == ',') {
				c++;
				eatws(in, c);
			}
			res.add(getCNAlt(in, c));
		}
		return res;
	}

	/**
	 * Given a string ((('foo',0.1),('bar',0.9)),...) representation of a
	 * confusion net in PCN format, return a CN object
	 */
	// parse ((('foo', 0.23), ('bar', 0.77)), (('a', 0.3), ('c', 0.7)))
	public static CN parsePCN(final String in) {
		CN res = new CN();
		int c = 0;
		if (in.charAt(c++) != '(')
			return res; // error
		while (true) {
			if (c > (int) in.length()) {
				break;
			}
			if (get(in, c) == ')') {
				c++;
				eatws(in, c);
				break;
			}
			if (get(in, c) == ',' && get(in, c + 1) == ')') {
				c += 2;
				eatws(in, c);
				break;
			}
			if (get(in, c) == ',') {
				c++;
				eatws(in, c);
			}
			res.add(getCNCol(in, c));
		}
		return res;
	}

}
