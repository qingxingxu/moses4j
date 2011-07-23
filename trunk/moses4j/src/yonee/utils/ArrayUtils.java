package yonee.utils;

public class ArrayUtils {
	public static int compare(boolean[] a, boolean[] b , int length){
		for(int i = 0; i < length; i ++){
			if( a[i] && !b[i]){
				return 1;
			} else if( a[i] == b[i]){
				continue;
			} else if( !a[i] && b[i]){
				return -1;
			}
		}
		return 0;
	}
}
