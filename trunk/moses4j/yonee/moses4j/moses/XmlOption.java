package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author YONEE
 * @NOT
 */
public class XmlOption {
	public static boolean processAndStripXMLTags(String line, List<List<XmlOption>> xmlOptionsList,
			ReorderingConstraint mReorderingConstraint, List<Integer> xmlWalls) {

		return true;
	}

	public WordsRange range;
	public TargetPhrase targetPhrase;
	public List<XmlOption> linkedOptions;

	XmlOption(final WordsRange r, final TargetPhrase tp) {
		range = r;
		targetPhrase = tp;
		linkedOptions = new ArrayList<XmlOption>(0);
	}

}
