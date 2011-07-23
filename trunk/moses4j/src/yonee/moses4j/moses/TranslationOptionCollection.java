package yonee.moses4j.moses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import yonee.moses4j.moses.TypeDef.FactorDirection;
import yonee.moses4j.moses.TypeDef.XmlInputType;
import yonee.utils.ASSERT;
import yonee.utils.CollectionUtils;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @OK not verify
 */
public abstract class TranslationOptionCollection {
	class CompareTranslationOption implements Comparator<TranslationOption> {
		public int compare(TranslationOption a, TranslationOption b) {
			return (int) (a.getFutureScore() - b.getFutureScore());
		}
	}

	// friend std::ostream& operator<<(std::ostream& out, const TranslationOptionCollection& coll);
	// private TranslationOptionCollection(final TranslationOptionCollection copy) {
	//		
	//
	// } /* < no copy constructor */

	protected List<List<TranslationOptionList>> m_collection = new ArrayList<List<TranslationOptionList>>(); /*
																											 * <
																											 * contains
																											 * translation
																											 * options
																											 */
	protected InputType m_source; /* < reference to the input */
	protected SquareMatrix m_futureScore; /*
										 * < matrix of future costs for contiguous parts (span) of
										 * the input
										 */
	protected final int m_maxNoTransOptPerCoverage; /*
													 * < maximum number of translation options per
													 * input span
													 */
	protected final float m_translationOptionThreshold; /*
														 * < threshold for translation options with
														 * regard to best option for input span
														 */
	protected List<Phrase> m_unksrcs = new ArrayList<Phrase>();

	protected TranslationOptionCollection(InputType src, int maxNoTransOptPerCoverage,
			float translationOptionThreshold) {
		m_source = src;
		m_futureScore = new SquareMatrix(src.getSize());
		m_maxNoTransOptPerCoverage = (maxNoTransOptPerCoverage);
		m_translationOptionThreshold = (translationOptionThreshold);
		// create 2-d vector
		int size = src.getSize();
		for (int startPos = 0; startPos < size; ++startPos) {
			m_collection.add(new ArrayList<TranslationOptionList>());

			int maxSize = size - startPos;
			int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
			maxSize = Math.min(maxSize, maxSizePhrase);

			for (int endPos = 0; endPos < maxSize; ++endPos) {
				m_collection.get(startPos).add(new TranslationOptionList());
			}
		}
	}

	protected void calcFutureScore() {
		// setup the matrix (ignore lower triangle, set upper triangle to -inf
		int size = m_source.getSize(); // the width of the matrix

		for (int row = 0; row < size; row++) {
			for (int col = row; col < size; col++) {
				m_futureScore.setScore(row, col, Float.NEGATIVE_INFINITY);
			}
		}

		// walk all the translation options and record the cheapest option for each span
		for (int startPos = 0; startPos < size; ++startPos) {
			int maxSize = m_source.getSize() - startPos;
			int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
			maxSize = Math.min(maxSize, maxSizePhrase);

			for (int endPos = startPos; endPos < startPos + maxSize; ++endPos) {
				TranslationOptionList transOptList = getTranslationOptionList(startPos, endPos);

				for (TranslationOption transOpt : transOptList) {
					float score = transOpt.getFutureScore();
					if (score > m_futureScore.getScore(startPos, endPos))
						m_futureScore.setScore(startPos, endPos, score);
				}
			}
		}

		// now fill all the cells in the strictly upper triangle
		// there is no way to modify the diagonal now, in the case
		// where no translation option covers a single-word span,
		// we leave the +inf in the matrix
		// like in chart parsing we want each cell to contain the highest score
		// of the full-span trOpt or the sum of scores of joining two smaller spans

		for (int colstart = 1; colstart < size; colstart++) {
			for (int diagshift = 0; diagshift < size - colstart; diagshift++) {
				int startPos = diagshift;
				int endPos = colstart + diagshift;
				for (int joinAt = startPos; joinAt < endPos; joinAt++) {
					float joinedScore = m_futureScore.getScore(startPos, joinAt)
							+ m_futureScore.getScore(joinAt + 1, endPos);
					/*
					 * // uncomment to see the cell filling scheme
					 * TRACE_ERR( "["
					 * <<startPos<<","<<endPos<<"] <-? ["<<startPos<<","<<joinAt<<"]+["
					 * <<joinAt+1<<","<<endPos
					 * << "] (colstart: "<<colstart<<", diagshift: "<<diagshift<<")"<<endl);
					 */
					if (joinedScore > m_futureScore.getScore(startPos, endPos))
						m_futureScore.setScore(startPos, endPos, joinedScore);
				}
			}
		}

		if (VERBOSE.v(3)) {
			int total = 0;
			for (int row = 0; row < size; row++) {
				int maxSize = size - row;
				int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
				maxSize = Math.min(maxSize, maxSizePhrase);

				for (int col = row; col < row + maxSize; col++) {
					int count = getTranslationOptionList(row, col).size();
					TRACE.err("translation options spanning from  " + row + " to " + col + " is "
							+ count + "\n");
					total += count;
				}
			}
			TRACE.err("translation options generated in total: " + total + "\n");

			for (int row = 0; row < size; row++)
				for (int col = row; col < size; col++)
					TRACE.err("future cost from " + row + " to " + col + " is "
							+ m_futureScore.getScore(row, col) + "\n");
		}
	}

	// ! Force a creation of a translation option where there are none for a particular source
	// position.
	protected void processUnknownWord(final List<DecodeGraph> decodeStepVL) {
		int size = m_source.getSize();
		// try to translation for coverage with no trans by expanding table limit
		for (int startVL = 0; startVL < decodeStepVL.size(); startVL++) {
			final DecodeGraph decodeStepList = decodeStepVL.get(startVL);
			for (int pos = 0; pos < size; ++pos) {
				TranslationOptionList fullList = getTranslationOptionList(pos, pos);
				int numTransOpt = fullList.size();
				if (numTransOpt == 0) {
					createTranslationOptionsForRange(decodeStepList, pos, pos, false);
				}
			}
		}

		boolean alwaysCreateDirectTranslationOption = StaticData.instance()
				.isAlwaysCreateDirectTranslationOption();
		// create unknown words for 1 word coverage where we don't have any trans options
		for (int pos = 0; pos < size; ++pos) {
			TranslationOptionList fullList = getTranslationOptionList(pos, pos);
			if (fullList.size() == 0 || alwaysCreateDirectTranslationOption)
				processUnknownWord(pos);
		}
	}

	// ! special handling of ONE unknown words.
	protected void processOneUnknownWord(final Word sourceWord, int sourcePos) {
		processOneUnknownWord(sourceWord, sourcePos, 1, null);
	}

	protected void processOneUnknownWord(final Word sourceWord, int sourcePos, int length) {
		processOneUnknownWord(sourceWord, sourcePos, length, null);
	}

	protected void processOneUnknownWord(final Word sourceWord, int sourcePos, int length,
			final float[] inputScores) {
		// unknown word, add as trans opt
		FactorCollection factorCollection = FactorCollection.instance();

		int isDigit = 0;

		final Factor f = sourceWord.get(0);
		final String s = f.getString();
		boolean isEpsilon = (s .equals( "") || s .equals( TypeDef.EPSILON));
		if (StaticData.instance().getDropUnknown()) {
			isDigit = Util.findFirstOf(s, "0123456789", 0);
			if (isDigit == -1)
				isDigit = 0;
			else
				isDigit = 1;
			// modify the starting bitmap
		}

		Phrase m_unksrc = new Phrase(FactorDirection.Input);
		m_unksrc.addWord(sourceWord);// YONEE
		m_unksrcs.add(m_unksrc);

		TranslationOption transOpt;
		TargetPhrase targetPhrase = new TargetPhrase(FactorDirection.Output);
		targetPhrase.setSourcePhrase(m_unksrc);
		if (inputScores != null) {
			targetPhrase.setScore(inputScores);
		} else {
			targetPhrase.setScore();
		}

		if (!(StaticData.instance().getDropUnknown() || isEpsilon || isDigit == 1)) {
			// add to dictionary
			Word targetWord = targetPhrase.addWord();

			for (int currFactor = 0; currFactor < TypeDef.MAX_NUM_FACTORS; currFactor++) {
				int factorType = (currFactor);

				final Factor sourceFactor = sourceWord.get(currFactor);
				if (sourceFactor == null)
					targetWord.set(factorType, factorCollection.addFactor(FactorDirection.Output,
							factorType, TypeDef.UNKNOWN_FACTOR));
				else
					targetWord.set(factorType, factorCollection.addFactor(FactorDirection.Output,
							factorType, sourceFactor.getString()));
			}
			// create a one-to-one aignment between UNKNOWN_FACTOR and its verbatim translation
		} else {
			// drop source word. create blank trans opt

			// targetPhrase.SetAlignment();

		}
		transOpt = new TranslationOption(new WordsRange(sourcePos, sourcePos + length - 1),
				targetPhrase, m_source, 0);
		transOpt.calcScore();
		add(transOpt);
	}

	// ! pruning: only keep the top n (m_maxNoTransOptPerCoverage) elements */
	protected void prune() {
		// quit, if max size, threshold
		if (m_maxNoTransOptPerCoverage == 0
				&& m_translationOptionThreshold == Float.NEGATIVE_INFINITY)
			return;

		// bookkeeping for how many options used, pruned
		int total = 0;
		int totalPruned = 0;

		// loop through all spans
		int size = m_source.getSize();
		for (int startPos = 0; startPos < size; ++startPos) {
			int maxSize = size - startPos;
			int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
			maxSize = Math.min(maxSize, maxSizePhrase);

			for (int endPos = startPos; endPos < startPos + maxSize; ++endPos) {
				// consider list for a span
				TranslationOptionList fullList = getTranslationOptionList(startPos, endPos);
				total += fullList.size();

				// size pruning
				if (m_maxNoTransOptPerCoverage > 0 && fullList.size() > m_maxNoTransOptPerCoverage) {
					// sort in vector
					Collections.sort(fullList, new CompareTranslationOption());
					totalPruned += fullList.size() - m_maxNoTransOptPerCoverage;

					CollectionUtils.resize(fullList, m_maxNoTransOptPerCoverage, null);
					// delete the rest
					// for (int i = m_maxNoTransOptPerCoverage ; i < fullList.size() ; ++i)
					// {
					// fullList.remove(i);
					//					  
					// //delete fullList.Get(i);
					// }
					// fullList.resize(m_maxNoTransOptPerCoverage);
				}

				// threshold pruning
				if (fullList.size() > 1 && m_translationOptionThreshold != Float.NEGATIVE_INFINITY) {
					// first, find the best score
					float bestScore = Float.NEGATIVE_INFINITY;
					for (int i = 0; i < fullList.size(); ++i) {
						if (fullList.get(i).getFutureScore() > bestScore)
							bestScore = fullList.get(i).getFutureScore();
					}
					// std::cerr << "best score for span " << startPos << "-" << endPos << " is " <<
					// bestScore << "\n";
					// then, remove items that are worse than best score + threshold

					for (int i = 0; i < fullList.size(); ++i) {
						if (fullList.get(i).getFutureScore() < bestScore
								+ m_translationOptionThreshold) {
							// std::cerr << "\tremoving item " << i << ", score " <<
							// fullList.Get(i)->GetFutureScore() << ": " <<
							// fullList.Get(i)->GetTargetPhrase() << "\n";
							fullList.remove(i);
							total--;
							totalPruned++;
							i--;
						}
						// else
						// {
						// std::cerr << "\tkeeping item " << i << ", score " <<
						// fullList.Get(i)->GetFutureScore() << ": " <<
						// fullList.Get(i)->GetTargetPhrase() << "\n";
						// }
					}
				} // end of threshold pruning
			}
		} // end of loop through all spans

		VERBOSE.v(2, "       Total translation options: " + total + "\n"
				+ "Total translation options pruned: " + totalPruned + "\n");
	}

	// ! sort all trans opt in each list for cube pruning */
	protected void sort() {
		int size = m_source.getSize();
		for (int startPos = 0; startPos < size; ++startPos) {
			int maxSize = size - startPos;
			int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
			maxSize = Math.min(maxSize, maxSizePhrase);

			for (int endPos = startPos; endPos < startPos + maxSize; ++endPos) {
				TranslationOptionList transOptList = getTranslationOptionList(startPos, endPos);
				Collections.sort(transOptList, new CompareTranslationOption());
			}
		}
	}

	// ! list of trans opt for a particular span
	protected TranslationOptionList getTranslationOptionList(int startPos, int endPos) {
		int maxSize = endPos - startPos;
		int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
		maxSize = Math.min(maxSize, maxSizePhrase);

		ASSERT.a(maxSize < m_collection.get(startPos).size());
		return m_collection.get(startPos).get(maxSize);
	}

	protected void add(TranslationOption translationOption) {
		final WordsRange coverage = translationOption.getSourceWordsRange();
		ASSERT.a(coverage.getEndPos() - coverage.getStartPos() < m_collection.get(
				coverage.getStartPos()).size());
		m_collection.get(coverage.getStartPos()).get(coverage.getEndPos() - coverage.getStartPos())
				.add(translationOption);
	}

	// ! implemented by inherited class, called by this class
	protected abstract void processUnknownWord(int sourcePos);

	protected void cacheLexReordering() {
		final List<LexicalReordering> lexReorderingModels = StaticData.instance()
				.getReorderModels();
		int size = m_source.getSize();
		for (LexicalReordering lexreordering : lexReorderingModels) {

			for (int startPos = 0; startPos < size; startPos++) {
				int maxSize = size - startPos;
				int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
				maxSize = Math.min(maxSize, maxSizePhrase);

				for (int endPos = startPos; endPos < startPos + maxSize; endPos++) {
					TranslationOptionList transOptList = getTranslationOptionList(startPos, endPos);
					for (TranslationOption transOpt : transOptList) {
						// Phrase sourcePhrase = m_source.getSubString(WordsRange(startPos,endPos));
						final Phrase sourcePhrase = transOpt.getSourcePhrase();
						if (sourcePhrase != null) {
							List<Float> score = lexreordering.getProb(sourcePhrase, transOpt
									.getTargetPhrase());
							if (!score.isEmpty())
								transOpt.cacheScores(lexreordering, score);
						}
					}
				}
			}
		}
	}

	// ! input sentence/confusion network
	public final InputType getSource() {
		return m_source;
	}

	// ! get length/size of source input
	public int getSize() {
		return m_source.getSize();
	};

	// ! Create all possible translations from the phrase tables
	public void createTranslationOptions(final List<DecodeGraph> decodeStepVL) {
		// loop over all substrings of the source sentence, look them up
		// in the phraseDictionary (which is the- possibly filtered-- phrase
		// table loaded on initialization), generate TranslationOption objects
		// for all phrases

		int size = m_source.getSize();
		for (int startVL = 0; startVL < decodeStepVL.size(); startVL++) {
			final DecodeGraph decodeStepList = decodeStepVL.get(startVL);
			for (int startPos = 0; startPos < size; startPos++) {
				int maxSize = size - startPos;
				int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
				maxSize = Math.min(maxSize, maxSizePhrase);

				for (int endPos = startPos; endPos < startPos + maxSize; endPos++) {
					createTranslationOptionsForRange(decodeStepList, startPos, endPos, true);
				}
			}
		}

		VERBOSE.v(2, "Translation Option Collection\n " + this + "\n");

		processUnknownWord(decodeStepVL);

		// Prune
		prune();

		sort();

		// future score matrix
		calcFutureScore();

		// Cached lex reodering costs
		cacheLexReordering();
	}

	// ! Create translation options that exactly cover a specific input span.
	public void createTranslationOptionsForRange(final DecodeGraph decodeGraph, int startPos,
			int endPos, boolean adhereTableLimit) {
		if ((StaticData.instance().getXmlInputType() != XmlInputType.XmlExclusive)
				|| !hasXmlOptionsOverlappingRange(startPos, endPos)) {
			Phrase sourcePhrase = null; // can't initialise with substring, in case it's confusion
			// network

			// consult persistent (cross-sentence) cache for stored translation options
			boolean skipTransOptCreation = false, useCache = StaticData.instance()
					.getUseTransOptCache();
			if (useCache) {
				final WordsRange wordsRange = new WordsRange(startPos, endPos);
				sourcePhrase = new Phrase(m_source.getSubString(wordsRange));

				final TranslationOptionList transOptList = StaticData.instance()
						.findTransOptListInCache(decodeGraph, sourcePhrase);
				// is phrase in cache?
				if (transOptList != null) {
					skipTransOptCreation = true;
					for (TranslationOption to : transOptList) {
						TranslationOption transOpt = new TranslationOption(to, wordsRange);
						add(transOpt);
					}
				}

			} // useCache

			if (!skipTransOptCreation) {
				// partial trans opt stored in here
				PartialTranslOptColl oldPtoc = new PartialTranslOptColl();
				int totalEarlyPruned = 0;

				// initial translation step

				Iterator<DecodeStep> iter = decodeGraph.c().iterator();
				DecodeStep decodeStep = null;
				ASSERT.a(iter.hasNext());

				decodeStep = iter.next();
				((DecodeStepTranslation) decodeStep).processInitialTranslation(m_source, oldPtoc,
						startPos, endPos, adhereTableLimit);

				// do rest of decode steps
				int indexStep = 0;
				for (; iter.hasNext();) {
					decodeStep = iter.next();
					PartialTranslOptColl newPtoc = new PartialTranslOptColl();

					// go thru each intermediate trans opt just created
					final List<TranslationOption> partTransOptList = oldPtoc.getList();

					for (TranslationOption inputPartialTranslOpt : partTransOptList) {
						decodeStep.process(inputPartialTranslOpt, decodeStep, newPtoc, this,
								adhereTableLimit);
					}
					// last but 1 partial trans not required anymore
					totalEarlyPruned += newPtoc.getPrunedCount();
					oldPtoc = null;
					oldPtoc = newPtoc;
					indexStep++;
				} // for (++iterStep

				// add to fully formed translation option list
				PartialTranslOptColl lastPartialTranslOptColl = oldPtoc;
				final List<TranslationOption> partTransOptList = lastPartialTranslOptColl.getList();

				for (TranslationOption transOpt : partTransOptList) {
					transOpt.calcScore();
					add(transOpt);
				}

				// storing translation options in persistent cache (kept across sentences)
				if (useCache) {
					if (partTransOptList.size() > 0) {
						TranslationOptionList transOptList0 = getTranslationOptionList(startPos,
								endPos);
						StaticData.instance().addTransOptListToCache(decodeGraph, sourcePhrase,
								transOptList0);
					}
				}

				lastPartialTranslOptColl.detachAll();
				totalEarlyPruned += oldPtoc.getPrunedCount();
				oldPtoc = null;
				// TRACE_ERR( "Early translation options pruned: " << totalEarlyPruned << endl);
			} // if (!skipTransOptCreation)

			if (useCache)
				sourcePhrase = null;
		} // if ((StaticData.instance().getXmlInputType() != XmlExclusive) ||
		// !HasXmlOptionsOverlappingRange(startPos,endPos))

		if ((StaticData.instance().getXmlInputType() != XmlInputType.XmlPassThrough)
				&& hasXmlOptionsOverlappingRange(startPos, endPos)) {
			createXmlOptionsForRange(startPos, endPos);
		}
	}

	// !Check if this range has XML options
	public boolean hasXmlOptionsOverlappingRange(int startPosition, int endPosition) {
		return false;
	}

	// ! Create xml-based translation options for the specific input span
	public void createXmlOptionsForRange(int startPosition, int endPosition) {
		// not implemented for base class
	}

	// ! returns future cost matrix for sentence
	public final SquareMatrix getFutureScore() {
		return m_futureScore;
	}

	// ! list of trans opt for a particular span
	public final TranslationOptionList getTranslationOptionList(final WordsRange coverage) {
		return getTranslationOptionList(coverage.getStartPos(), coverage.getEndPos());
	}

	// TO_STRING(){}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int size = getSize();
		for (int startPos = 0; startPos < size; ++startPos) {
			int maxSize = size - startPos;
			int maxSizePhrase = StaticData.instance().getMaxPhraseLength();
			maxSize = Math.min(maxSize, maxSizePhrase);

			for (int endPos = startPos; endPos < startPos + maxSize; ++endPos) {
				final TranslationOptionList fullList = getTranslationOptionList(startPos, endPos);
				int sizeFull = fullList.size();
				for (int i = 0; i < sizeFull; i++) {
					sb.append(fullList.get(i)).append("\n");
				}
			}
		}

		// std::vector< std::vector< TranslationOptionList > >::const_iterator i =
		// coll.m_collection.begin();
		// int j = 0;
		// for (; i!=coll.m_collection.end(); ++i) {
		// out << "s[" << j++ << "].size=" << i->size() << std::endl;
		// }

		return sb.toString();
	}

}
