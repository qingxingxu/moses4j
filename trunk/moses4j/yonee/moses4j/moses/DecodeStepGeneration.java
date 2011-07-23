package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import yonee.moses4j.moses.GenerationDictionary.OutputWordCollection;
import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.utils.Pair;

/**
 * 
 * @author YONEE
 * @OK
 * @RISK
 *       incrementIterators
 *       WordList ArrayList 性能是否存在问题
 */
public class DecodeStepGeneration extends DecodeStep {
	// --------------
	// helpers
	// typedef pair<Word, ScoreComponentCollection> WordPair;
	// typedef list< WordPair > WordList;
	// // 1st = word
	// // 2nd = score
	// typedef list< WordPair >::const_iterator WordListIterator;

	class WordPair extends Pair<Word, ScoreComponentCollection> {
		public WordPair(Word first, ScoreComponentCollection second) {
			super(first, second);
		}
	}

	class WordList extends ArrayList<WordPair> {
		private static final long serialVersionUID = 1L;

	}

	/**
	 * used in generation: increases iterators when looping through the exponential number of
	 * generation expansions
	 */
	public static void incrementIterators(int[] wordListIters, final List<WordPair> wordLists[]) {
		for (int currPos = 0; currPos < wordLists.length; currPos++) {
			wordListIters[currPos]++;
			if (wordListIters[currPos] != wordLists[currPos].size()) {
				return;
			} else { // eg 9 -> 10
				wordListIters[currPos] = 0;
			}
		}
	}

	// ----------------------------------
	public DecodeStepGeneration(GenerationDictionary generationDictionary, DecodeStep prev) {
		super(generationDictionary, prev);
	}

	// ! returns phrase table (dictionary) for translation step
	public GenerationDictionary getGenerationDictionary() {
		return (GenerationDictionary) (m_ptr);
	}

	@Override
	public void process(TranslationOption inputPartialTranslOpt, DecodeStep decodeStep,
			PartialTranslOptColl outputPartialTranslOptColl, TranslationOptionCollection toc,
			boolean adhereTableLimit) {
		if (inputPartialTranslOpt.getTargetPhrase().getSize() == 0) { // word deletion

			TranslationOption newTransOpt = new TranslationOption(inputPartialTranslOpt);
			outputPartialTranslOptColl.add(newTransOpt);

			return;
		}

		// normal generation step
		final GenerationDictionary generationDictionary = decodeStep.getGenerationDictionary();
		// final WordsRange sourceWordsRange = inputPartialTranslOpt.getSourceWordsRange();

		final Phrase targetPhrase = inputPartialTranslOpt.getTargetPhrase();
		int targetLength = targetPhrase.getSize();

		// generation list for each word in phrase
		WordList[] wordListVector = new WordList[targetLength];
		for (int i = 0; i < targetLength; i++) {
			wordListVector[i] = new WordList();
		}

		// create generation list
		int wordListVectorPos = 0;
		for (int currPos = 0; currPos < targetLength; currPos++) // going thorugh all words
		{
			// generatable factors for this word to be put in wordList
			WordList wordList = wordListVector[wordListVectorPos];
			final Word word = targetPhrase.getWord(currPos);

			// consult dictionary for possible generations for this word
			final OutputWordCollection wordColl = generationDictionary.findWord(word);

			if (wordColl == null) { // word not found in generation dictionary
				// toc->ProcessUnknownWord(sourceWordsRange.getStartPos(), factorCollection);
				return; // can't be part of a phrase, special handling
			} else {
				// sort(*wordColl, CompareWordCollScore);
				for (Entry<Word, ScoreComponentCollection> e : wordColl.entrySet()) {
					final Word outputWord = e.getKey();
					final ScoreComponentCollection score = e.getValue();
					wordList.add(new WordPair(outputWord, score));
				}

				wordListVectorPos++; // done, next word
			}
		}

		// use generation list (wordList)
		// set up iterators (total number of expansions)
		int numIteration = 1;

		int[] wordListIterVector = new int[targetLength];

		Word[] mergeWords = new Word[targetLength];

		for (int currPos = 0; currPos < targetLength; currPos++) {
			wordListIterVector[currPos] = 0;
			numIteration *= wordListVector[currPos].size();
		}

		// go thru each possible factor for each word create hypothesis
		for (int currIter = 0; currIter < numIteration; currIter++) {
			ScoreComponentCollection generationScore = new ScoreComponentCollection(); // total
			// score for this string of words

			// create vector of words with new factors for last phrase
			for (int currPos = 0; currPos < targetLength; currPos++) {
				final WordPair wordPair = wordListVector[currPos].get(wordListIterVector[currPos]);
				mergeWords[currPos] = (wordPair.first);
				generationScore.plusEquals(wordPair.second);
			}

			// merge with existing trans opt
			Phrase genPhrase = new Phrase(FactorDirection.Output, mergeWords);
			TranslationOption newTransOpt = mergeGeneration(inputPartialTranslOpt, genPhrase,
					generationScore);
			if (newTransOpt != null) {
				outputPartialTranslOptColl.add(newTransOpt);
			}

			// increment iterators
			incrementIterators(wordListIterVector, wordListVector);
		}
	}

	/*
	 * ! create new TranslationOption from merging oldTO with mergePhrase
	 * This function runs IsCompatible() to ensure the two can be merged
	 */
	private TranslationOption mergeGeneration(final TranslationOption oldTO, Phrase mergePhrase,
			final ScoreComponentCollection generationScore) {
		if (isFilteringStep()) {
			if (!oldTO.isCompatible(mergePhrase, m_conflictFactors))
				return null;
		}

		TranslationOption newTransOpt = new TranslationOption(oldTO);
		newTransOpt.mergeNewFeatures(mergePhrase, generationScore, m_newOutputFactors);
		return newTransOpt;
	}

}
