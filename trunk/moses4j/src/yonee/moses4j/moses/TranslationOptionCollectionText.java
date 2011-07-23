package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;
/**
 * 
 * @author YONEE
 * @OK
 *
 */
public class TranslationOptionCollectionText extends TranslationOptionCollection {

	public boolean hasXmlOptionsOverlappingRange(int startPosition, int endPosition) {
		Sentence source = (Sentence) (m_source);
		return source.xmlOverlap(startPosition, endPosition);
	}

	public void createXmlOptionsForRange(int startPosition, int endPosition) {
		Sentence source = (Sentence) (m_source);
		List<TranslationOption> xmlOptions = new ArrayList<TranslationOption>();
		source.getXmlTranslationOptions(xmlOptions, startPosition, endPosition);
		// get vector of TranslationOptions from Sentence
		for (int i = 0; i < xmlOptions.size(); i++) {
			xmlOptions.get(i).calcScore();
			add(xmlOptions.get(i));
		}
	}

	public TranslationOptionCollectionText(Sentence sentence, int maxNoTransOptPerCoverage,
			float transOptThreshold) {
		super(sentence, maxNoTransOptPerCoverage, transOptThreshold);
	}

	protected void processUnknownWord(int sourcePos) {
		final Word sourceWord = m_source.getWord(sourcePos);
		processOneUnknownWord(sourceWord, sourcePos);

	}

}
