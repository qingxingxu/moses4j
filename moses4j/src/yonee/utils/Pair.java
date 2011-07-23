package yonee.utils;

public class Pair<A, B> {
	public A first;
	public B second;

	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public int hashCode() {
		return 19810108;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		Pair<A, B> a = (Pair<A, B>) obj;
		return first.equals(a.first) && second.equals(a.second);
	}

	public String toString() {
		return first + "+" + second;
	}
}