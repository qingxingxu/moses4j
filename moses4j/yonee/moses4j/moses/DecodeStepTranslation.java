package yonee.moses4j.moses;

import yonee.moses4j.moses.TypeDef.InputTypeEnum;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 * 		int iterTargetPhrase, iterEnd;
 */
public class DecodeStepTranslation extends DecodeStep {

	// ! returns phrase table (dictionary) for translation step
	// public final PhraseDictionary getPhraseDictionary() {
	// return super.getPhraseDictionary();
	// }

	@Override
	public void process(TranslationOption inputPartialTranslOpt, DecodeStep decodeStep,
			PartialTranslOptColl outputPartialTranslOptColl, TranslationOptionCollection toc,
			boolean adhereTableLimit) {
		if (inputPartialTranslOpt.getTargetPhrase().getSize() == 0) { // word deletion

			outputPartialTranslOptColl.add(new TranslationOption(inputPartialTranslOpt));

			return;
		}

		// normal trans step
		final WordsRange sourceWordsRange = inputPartialTranslOpt.getSourceWordsRange();
		final PhraseDictionary phraseDictionary = decodeStep.getPhraseDictionary();
		final int currSize = inputPartialTranslOpt.getTargetPhrase().getSize();
		final int tableLimit = phraseDictionary.getTableLimit();

		final TargetPhraseCollection phraseColl = phraseDictionary.getTargetPhraseCollection(toc
				.getSource(), sourceWordsRange);

		if (phraseColl != null) {
			int iterTargetPhrase, iterEnd;
			iterEnd = (!adhereTableLimit || tableLimit == 0 || phraseColl.size() < tableLimit) ? phraseColl
					.size()
					: 0 + tableLimit;

			for (iterTargetPhrase = 0; iterTargetPhrase != iterEnd; iterTargetPhrase++) {
				final TargetPhrase targetPhrase = phraseColl.get(iterTargetPhrase);
				// skip if the
				if (targetPhrase.getSize() != currSize)
					continue;

				TranslationOption newTransOpt = mergeTranslation(inputPartialTranslOpt,
						targetPhrase);
				if (newTransOpt != null) {
					outputPartialTranslOptColl.add(newTransOpt);
				}
			}
		} else if (sourceWordsRange.getNumWordsCovered() == 1) { // unknown handler
			// toc.ProcessUnknownWord(sourceWordsRange.getStartPos(), factorCollection);
		}

	}

	/*
	 * ! initialize list of partial translation options by applying the first translation step
	 * Ideally, this function should be in DecodeStepTranslation class
	 */
	public void processInitialTranslation(final InputType source,
			PartialTranslOptColl outputPartialTranslOptColl, int startPos, int endPos,
			boolean adhereTableLimit) {
		final PhraseDictionary phraseDictionary = (PhraseDictionary) (m_ptr);
		final int tableLimit = phraseDictionary.getTableLimit();

		final WordsRange wordsRange = new WordsRange(startPos, endPos);
		final TargetPhraseCollection phraseColl = phraseDictionary.getTargetPhraseCollection(
				source, wordsRange);

		if (phraseColl != null) {
			if (VERBOSE.v(3)) {
				if (StaticData.instance().getInputType() == InputTypeEnum.SentenceInput)
					TRACE.err("[" + source.getSubString(wordsRange) + "; " + startPos + "-"
							+ endPos + "]\n");
				else
					TRACE.err("[" + startPos + "-" + endPos + "]\n");
			}

			int iterTargetPhrase, iterEnd;
			iterEnd = (!adhereTableLimit || tableLimit == 0 || phraseColl.size() < tableLimit) ? phraseColl
					.size()
					: 0 + tableLimit;

			for (iterTargetPhrase = 0; iterTargetPhrase != iterEnd; iterTargetPhrase++) {
				final TargetPhrase targetPhrase = phraseColl.get(iterTargetPhrase);
				outputPartialTranslOptColl.add(new TranslationOption(wordsRange, targetPhrase,
						source));

				VERBOSE.v(3, "\t" + targetPhrase + "\n");
			}
			VERBOSE.v(3, "\n");
		}
	}

	/*
	 * ! create new TranslationOption from merging oldTO with mergePhrase
	 * This function runs IsCompatible() to ensure the two can be merged
	 */
	private TranslationOption mergeTranslation(final TranslationOption oldTO,
			final TargetPhrase targetPhrase) {
		if (isFilteringStep()) {
			if (!oldTO.isCompatible(targetPhrase, m_conflictFactors))
				return null;
		}

		TranslationOption newTransOpt = new TranslationOption(oldTO);
		newTransOpt.mergeNewFeatures(targetPhrase, targetPhrase.getScoreBreakdown(),
				m_newOutputFactors);
		return newTransOpt;
	}

	public DecodeStepTranslation(PhraseDictionary dictionary, DecodeStep prev) {
		super(dictionary, prev);
	}

}
