package yonee.utils;

public class ASSERT {

	static public void a(boolean flag) {
		if (!flag) {
			throw new AssertionError("assert error!");
		}
	}
}
