package yonee.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CollectionUtils {

	public static <T> T qnth(List<T> sample, int n, Comparator<T> comp) {
		T pivot = sample.get(0);
		List<T> below = new ArrayList<T>(), above = new ArrayList<T>();
		for (T s : sample) {

			int flag = comp.compare(s, pivot);
			if (flag < 0) {
				above.add(s);
			} else if (flag > 0) {
				below.add(s);
			}
		}
		int i = below.size(), j = sample.size() - above.size();
		if (n < i)
			return qnth(below, n, comp);
		else if (n >= j)
			return qnth(above, n - j, comp);
		else
			return pivot;
	}

	public static <T> void init(List<T> list, int count, T o) {
		for (int i = 0; i < count; i++) {
			list.add(o);
		}
	}

	public static <T> void resize(List<T> list, int size, Class<T> clazz) {
		if (size == list.size()) {
			return;
		}
		if (size < list.size()) {
			Iterator<T> iter = list.iterator();
			for (int i = 1; iter.hasNext(); i++) {
				iter.next();
				if (i > size) {
					iter.remove();
				}
			}
		} else {
			for (int i = 0; i <= size - list.size(); i++) {
				try {
					list.add(clazz != null ? clazz.newInstance() : null);
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public static <T> void fill(List<T> list, int start, int end, Class<T> clazz) {
		for (int i = start; i <= end; i++) {
			try {

				list.set(i, clazz != null ? clazz.newInstance() : null);

			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static <T> void fill(T[] list, int start, int end, Class<T> clazz) {
		for (int i = start; i <= end; i++) {
			try {
				list[i] = clazz != null ? clazz.newInstance() : null;
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static <T> T get(Set<T> set, T obj) {
		for (T o : set) {
			if (o.equals(obj)) {
				return o;
			}
		}
		return null;
	}
}
