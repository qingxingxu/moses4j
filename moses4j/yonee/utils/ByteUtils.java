package yonee.utils;

public class ByteUtils {
	public static byte[] getBytes(int u) {
		byte b[] = new byte[4];
		b[0] = (byte) (u);
		b[1] = (byte) (u >> 8);
		b[2] = (byte) (u >> 16);
		b[3] = (byte) (u >> 24);
		return b;
	}

	public static byte[] getBytes(long u) {
		byte b[] = new byte[8];
		b[0] = (byte) (u);
		b[1] = (byte) (u >> 8);
		b[2] = (byte) (u >> 16);
		b[3] = (byte) (u >> 24);
		b[4] = (byte) (u >> 24);
		b[5] = (byte) (u >> 32);
		b[6] = (byte) (u >> 40);
		b[7] = (byte) (u >> 48);
		return b;
	}
}
