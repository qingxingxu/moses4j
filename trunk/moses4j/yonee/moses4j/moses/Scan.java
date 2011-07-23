package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;
/**
 * 
 * @author YONEE
 * @OK
 */
public class Scan {
	public static List<Float> toFloat(List<String> src) {
		List<Float> r = new ArrayList<Float>();
		for (int i = 0; i < src.size(); i++) {
			r.add(Float.valueOf(src.get(i)));
		}
		return r;
	}
	
	public static List<Float> toFloatList(String[] src) {		
		List<Float> r = new ArrayList<Float>();
		for (int i = 0; i < src.length; i++) {
			r.add(Float.valueOf(src[i]));
		}
		return r;
	}

	public static List<Integer> toInt(List<String> src) {
		List<Integer> r = new ArrayList<Integer>();
		for (int i = 0; i < src.size(); i++) {
			r.add(Integer.valueOf(src.get(i)));
		}
		return r;
	}
}
