package yonee.utils;

import yonee.moses4j.moses.StaticData;

public class VERBOSE {
	static public boolean v(int level) {
		return StaticData.instance().getVerboseLevel() >= level;
	}

	static public void v(int level, String str) {
		if (StaticData.instance().getVerboseLevel() >= level) {
			TRACE.err(str);
		}
	}
}
