import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

public class TestUnknow {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// System.out.println("abc".substring(0,1));

		// System.out.println("the ||| ет ||| 0.3 ||| |||".indexOf("|||", 22));
		// StringWriter out = new StringWriter();

		// PrintWriter pw = new PrintWriter(out);

		// System.out.println(out.toString());
		// PrintStream ps = new PrintStream(out);

		try {
			testGzip();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void testGzip() throws IOException {
		BufferedReader zip = new BufferedReader(new InputStreamReader(new GZIPInputStream(
				new FileInputStream("e:/nlpcode/sample-models/lm/chinese.lm.gz"))));

		String line;
		while ((line = zip.readLine()) != null) {
			System.out.println(line);
		}
		zip.close();

	}

	public static void testFile() {

		try {
			RandomAccessFile r = new RandomAccessFile("e:/a.test", "rw");

			// r.write(8);
			r.writeLong(500);
			r.writeChars("aaaa");
			r.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static void testIncrementIterators() {
		List<String>[] a = new ArrayList[4];
		int[] b = new int[4];
		int numIteration = 1;
		for (int i = 0; i < 4; i++) {
			a[i] = new ArrayList<String>();

			a[i].add(i + "0");
			a[i].add(i + "1");
			a[i].add(i + "2");

			b[i] = 0;

			numIteration *= a[i].size();
		}

		for (int i = 0; i < numIteration; i++) {

			for (int j = 0; j < 4; j++) {
				// System.out.print(b[i % 4].second + ((i + 1) % 4 == 0 ? "\n" : "|"));
				System.out.print(a[j].get(b[j]) + "|");
			}
			System.out.println();
			incrementIterators(b, a);

		}
		/*
		 * 00|10|20|30|
		 * 01|10|20|30|
		 * 02|10|20|30|
		 * 00|11|20|30|
		 * 01|11|20|30|
		 * 02|11|20|30|
		 * 00|12|20|30|
		 * 01|12|20|30|
		 * 02|12|20|30|
		 * 00|10|21|30|
		 * 01|10|21|30|
		 * 02|10|21|30|
		 * 00|11|21|30|
		 * 01|11|21|30|
		 * 02|11|21|30|
		 * 00|12|21|30|
		 * 01|12|21|30|
		 * 02|12|21|30|
		 * 00|10|22|30|
		 * 01|10|22|30|
		 */

		// 00|01|02|10|00|01|02|11|00|01|02|12|20|00|01|02|10|00|01|02|11|00|01|02|12|21|00|01|02|10|00|01|02|11|00|01|02|12|22|30|00|01|02|10|
		// 00|01|02|10|00|01|02|11|00|01|02|12|20|00|01|02|10|00|01|02|11|00|01|02|12|21|00|01|02|10|00|01|02|11|00|01|02|12|22|30|00|01|02|10|
		// 00|01|02|10|00|01|02|11|00|01|02|12|20|00|01|02|10|00|01|02|11|00|01|02|12|21|00|01|02|10|00|01|02|11|00|01|02|12|22|30|00|01|02|10|00|01|02|11|00|01|02|12|20|00|01|02|10|00|01|02|11|00|01|02|12|21|00|01|02|10|00|01|02|11|00|01|02|12|22|31|00|01|02|10|00|01|02|11|00|01|02|12|20|00|01|02|10|00|01|02|11|00|01|02|12|21|00|01|02|10|00|01|02|11|00|01|02|12|22|32|

	}

	public static void incrementIterators(int[] wordListIters, final List<String> wordLists[]) {
		for (int currPos = 0; currPos < wordLists.length; currPos++) {
			wordListIters[currPos]++;
			if (wordListIters[currPos] != wordLists[currPos].size()) {
				return;
			} else { // eg 9 -> 10
				wordListIters[currPos] = 0;
			}
		}
	}

	public static void test() {
		// System.out.println(FactorDirection.Input.ordinal());
		/*
		 * Map<String, String> map = new HashMap<String, String>();
		 * 
		 * for (int i = 0; i < 10; i++) {
		 * map.put(i + "", i + "");
		 * }
		 * 
		 * Iterator<Map.Entry<String, String>> iter = map.entrySet().iterator();
		 * while (iter.hasNext()) {
		 * Map.Entry<String, String> e = iter.next();
		 * if (Integer.valueOf(e.getValue()) > 5) {
		 * iter.remove();
		 * }
		 * }
		 * for (Map.Entry<String, String> e : map.entrySet()) {
		 * System.out.println(e.getValue());
		 * }
		 * 
		 * List<Integer> l = new ArrayList<Integer>(10);
		 * CollectionUtils.init(l, 10, null);
		 * l.set(8, 8);
		 * 
		 * String ss[] = "-1.113943   ет  -0.2284793".split(" ");
		 * for (String s : ss) {
		 * System.out.println(s);
		 * }
		 */
	}

}
