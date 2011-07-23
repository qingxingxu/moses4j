package yonee.moses4j.moses;

import java.util.List;

/**
 * 
 * @author YONEE
 * @OK
 */
public interface CellCollection {
	public List<Word> getHeadwords(WordsRange coverage);
}
