package yonee.moses4j.moses;

import yonee.moses4j.moses.TypeDef.SearchAlgorithm;

/**
 * 
 * @author YONEE
 * @OK
 */
public abstract class Search {

	protected Phrase m_constraint;
	protected Manager m_manager;

	public abstract HypothesisStack[] getHypothesisStacks();

	public abstract Hypothesis getBestHypothesis();

	public abstract void processSentence();

	public Search(Manager manager) {
		m_manager = manager;
	}

	// Factory
	public static Search createSearch(Manager manager, final InputType source,
			SearchAlgorithm searchAlgorithm, final TranslationOptionCollection transOptColl) {
		switch (searchAlgorithm) {
		case Normal:
			return new SearchNormal(manager, source, transOptColl);
		case CubePruning:
			return new SearchCubePruning(manager, source, transOptColl);
		case CubeGrowing:
			return null;
		default:
			UserMessage.add("ERROR: search. Aborting\n");
			System.exit(0);
			return null;
		}
	}

}
