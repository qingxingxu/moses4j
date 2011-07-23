package yonee.utils;

public class Ref<T> {
	public T v;

	public Ref(T v) {
		this.v = v;
	}
	public String toString(){
		return v.toString();
	}
}
