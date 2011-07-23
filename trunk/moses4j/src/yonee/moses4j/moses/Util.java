package yonee.moses4j.moses;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yonee.utils.TRACE;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 */
public class Util {
	public static Timer timer = new Timer();

	public static boolean fileExists(String filePath) {
		return new File(filePath).exists();
	}

	public static String[] tokenizeMultiCharSeparator(String str, final String separator) {
		List<String> tokens = new ArrayList<String>();

		int pos = 0;
		// Find first "non-delimiter".
		int nextPos = str.indexOf(separator, pos);

		while (nextPos != -1) {
			// Found a token, add it to the vector.
			tokens.add(str.substring(pos, nextPos));
			// Skip delimiters. Note the "not_of"
			pos = nextPos + separator.length();
			// Find next "non-delimiter"
			nextPos = str.indexOf(separator, pos);
		}

		tokens.add(str.substring(pos, str.length()));

		return tokens.toArray(new String[tokens.size()]);
	}

	public static String[] tokenize(String str) {
		return tokenize(str, " \t");
	}

	public static String[] tokenize(String str, final String delimiters) {
		// return str.split(delimiters);
		List<String> tokens = new ArrayList<String>();
		// Skip delimiters at beginning.
		int lastPos = findFirstNotOf(str, delimiters, 0);
		// Find first "non-delimiter".
		int pos = findFirstOf(str, delimiters, lastPos);

		while (-1 != pos || -1 != lastPos) {
			// Found a token, add it to the vector.
			tokens.add(str.substring(lastPos, pos == -1 ? str.length() : pos));
			// Skip delimiters. Note the "not_of"
			lastPos = findFirstNotOf(str, delimiters, pos);
			// Find next "non-delimiter"
			pos = findFirstOf(str, delimiters, lastPos);
		}
		return tokens.toArray(new String[tokens.size()]);
	}

	public static int findFirstOf(String str, String delimiters, int pos) {
		if (pos == -1) {
			return -1;
		}
		for (int i = pos; i < str.length(); i++) {
			if (delimiters.indexOf(str.charAt(i)) >= 0) {
				return i;
			}
		}
		return -1;
	}

	public static int findFirstNotOf(String str, String delimiters, int pos) {
		if (pos == -1) {
			return -1;
		}
		for (int i = pos; i < str.length(); i++) {
			if (delimiters.indexOf(str.charAt(i)) == -1) {
				return i;
			}
		}
		return -1;
	}

	public static int[] tokenizeInt(String str, final String delimiters) {
		String s[] = str.split(delimiters);
		int r[] = new int[s.length];
		for (int i = 0; i < s.length; i++) {
			r[i] = Integer.valueOf(s[i]);
		}
		return r;
	}

	public static Integer[] tokenizeInteger(String str, final String delimiters) {
		String s[] = str.split(delimiters);
		Integer r[] = new Integer[s.length];
		for (int i = 0; i < s.length; i++) {
			r[i] = Integer.valueOf(s[i]);
		}
		return r;
	}

	public static float[] tokenizeFloat(String str) {
		return tokenizeFloat(str, " \t");
	}

	public static float[] tokenizeFloat(String str, final String delimiters) {
		String s[] = str.split(delimiters);
		float r[] = new float[s.length];
		for (int i = 0; i < s.length; i++) {
			r[i] = Float.valueOf(s[i]);
		}
		return r;
	}

	public static void printUserTime(String message) {
		timer.check(message);
	}

	public static void resetUserTime() {
		timer.start();
	};

	public static float transformScore(float prob) {
		return (float) Math.log(prob);
	}

	public static float[] transformScore(float prob[]) {
		for (int i = 0; i < prob.length; i++) {
			prob[i] = (float) Math.log(prob[i]);
		}
		return prob;
	}

	public static List<Float> transformScore(List<Float> prob) {
		for (int i = 0; i < prob.size(); i++) {
			prob.set(i, (float) Math.log(prob.get(i)));
		}
		return prob;
	}

	public static float[] floorScore(float logScore[]) {
		for (int i = 0; i < logScore.length; i++) {
			logScore[i] = Math.max(logScore[i], TypeDef.LOWEST_SCORE);
		}
		return logScore;
	}

	public static List<Float> floorScore(List<Float> logScore) {
		for (int i = 0; i < logScore.size(); i++) {
			logScore.set(i, Math.max(logScore.get(i), TypeDef.LOWEST_SCORE));
		}
		return logScore;
	}

	// ! make sure score doesn't fall below LOWEST_SCORE
	final public static float floorScore(float logScore) {
		return Math.max(logScore, TypeDef.LOWEST_SCORE);
	}

	final public static float transformLMScore(Float irstScore) {
		return irstScore * 2.30258509299405f;
	}

	/**
	 * @NOT
	 * @param <T>
	 * @param v
	 */
	final public static <T> void shrinkToFit(T v) {

	}

	public static float untransformLMScore(float logNScore) {
		return logNScore / 2.30258509299405f;
	}

	public static Map<String, String> processAndStripSGML(String line) {

		Map<String, String> meta = new HashMap<String, String>();
		String lline = line.toLowerCase();
		if (lline.indexOf("<seg") != 0)
			return meta;
		int close = lline.indexOf(">");
		if (close == -1)
			return meta; // error
		int end = lline.indexOf("</seg>");
		String seg = lline.substring(4, close).trim();
		String text = line.substring(close + 1, end);
		for (int i = 1; i < seg.length(); i++) {
			if (seg.charAt(i) == '=' && seg.charAt(i - 1) == ' ') {
				String less = seg.substring(0, i - 1) + seg.substring(i);
				seg = less;
				i = 0;
				continue;
			}
			if (seg.charAt(i) == '=' && seg.charAt(i + 1) == ' ') {
				String less = seg.substring(0, i + 1);
				if (i + 2 < seg.length())
					less += seg.substring(i + 2);
				seg = less;
				i = 0;
				continue;
			}
		}
		line = text.trim();
		if (seg == "")
			return meta;
		for (int i = 1; i < seg.length(); i++) {
			if (seg.charAt(i) == '=') {
				String label = seg.substring(0, i);
				String val = seg.substring(i + 1);
				if (val.charAt(0) == '"') {
					val = val.substring(1);
					int close0 = val.indexOf('"');
					if (close0 == -1) {
						TRACE.err("SGML parse error: missing \"\n");
						seg = "";
						i = 0;
					} else {
						seg = val.substring(close0 + 1);
						val = val.substring(0, close0);
						i = 0;
					}
				} else {
					int close1 = val.indexOf(' ');
					if (close1 == -1) {
						seg = "";
						i = 0;
					} else {
						seg = val.substring(close1 + 1);
						val = val.substring(0, close1);
					}
				}
				label = (label.trim());
				seg = (seg.trim());
				meta.put(label, val);
			}
		}
		return meta;

	}

	public static float innerProduct(float[] a, int first1, int last1, List<Float> b, int first2,
			float init) {
		while (first1 != last1)
			init = init + a[first1++] * b.get(first2++);
		return init;
	}

	public static float innerProduct(List<Float> a, int first1, int last1, List<Float> b,
			int first2, float init) {
		while (first1 != last1)
			init = init + a.get(first1++) * b.get(first2++);
		return init;
	}

	public static double getUserTime() {
		return timer.getElapsedTime() ;
	}

	public static <T> String join(final String delimiter, final List<T> items) {
		StringBuilder outstr = new StringBuilder();
		if (items.size() == 0)
			return "";
		outstr.append(items.get(0));
		for (int i = 1; i < items.size(); i++)
			outstr.append(delimiter).append(items.get(i));
		return outstr.toString();
	}

}
