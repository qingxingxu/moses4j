package yonee.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CollectionUtilsTest {

	@Test
	public void testResize() {
		List<Integer> l = new ArrayList<Integer>();
		for (int i = 0; i < 10; i++) {
			l.add(i);
		}
		// expand
		CollectionUtils.resize(l, 8, null);
		Integer[] except = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, null, null };
		for (int i = 0; i < l.size(); i++) {
			Assert.assertEquals(except[i], l.get(i));
		}
		// decrease
		CollectionUtils.resize(l, 10, null);
		for (int i = 0; i < l.size(); i++) {
			Assert.assertEquals(except[i], l.get(i));
		}
	}

	/*
	 * @Test
	 * public void testCompare() {
	 * List<Integer> a = new ArrayList<Integer>();
	 * Integer[] aa = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7 };
	 * for (int i = 0; i < aa.length; i++) {
	 * a.add(aa[i]);
	 * }
	 * List<Integer> b = new ArrayList<Integer>();
	 * Integer[] bb = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7 };
	 * for (int i = 0; i < bb.length; i++) {
	 * b.add(bb[i]);
	 * }
	 * 
	 * //System.out.println(CollectionUtils.compare(a, b));
	 * 
	 * 
	 * }
	 */

	@Test
	public void testQnth() {
		List<Integer> a = new ArrayList<Integer>();
		Integer[] aa = new Integer[] { 0, 4, 5, 3, 1, 3, 6, 7 };
		for (int i = 0; i < aa.length; i++) {
			a.add(aa[i]);
		}
		/*
		 * int c = CollectionUtils.qnth(a, 3, new Comparator<Integer>(){
		 * public int compare(Integer a0, Integer a1) {
		 * return a0 - a1;
		 * }
		 * });
		 */
		Collections.sort(a, new Comparator<Integer>() {
			public int compare(Integer a0, Integer a1) {
				return a0 - a1;
			}
		});

		System.out.println( " " + a);

		/*
		 * List<Integer> b = new ArrayList<Integer>();
		 * Integer[] bb = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7 };
		 * for (int i = 0; i < bb.length; i++) {
		 * b.add(bb[i]);
		 * }
		 */

		// System.out.println(CollectionUtils.compare(a, b));

	}

}
