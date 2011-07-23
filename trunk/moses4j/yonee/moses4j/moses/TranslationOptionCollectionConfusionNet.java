package yonee.moses4j.moses;

import yonee.utils.Pair;

/**
 * 
 * @author YONEE
 * @OK
 */
public class TranslationOptionCollectionConfusionNet extends TranslationOptionCollection {

	public TranslationOptionCollectionConfusionNet(ConfusionNet input,
			int maxNoTransOptPerCoverage, float translationOptionThreshold) {
		super(input, maxNoTransOptPerCoverage, translationOptionThreshold);
	}

	protected void processUnknownWord(int sourcePos) {
		ConfusionNet source = (ConfusionNet) (m_source);

		ConfusionNet.Column coll = source.getColumn(sourcePos);
		int j = 0;
		for (Pair<Word, float[]> i : coll) {
			processOneUnknownWord(i.first, sourcePos, source.getColumnIncrement(sourcePos, j++),
					(i.second));
		}
	}

}
