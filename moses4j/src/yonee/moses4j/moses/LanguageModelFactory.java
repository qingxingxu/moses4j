package yonee.moses4j.moses;

import java.io.IOException;
import java.util.List;

import yonee.moses4j.moses.TypeDef.LMImplementation;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class LanguageModelFactory {

	public static LanguageModel createLanguageModel(
			LMImplementation lmImplementation,
			List<Integer> factorTypes,// vector<FactorType>
			int nGramOrder, String languageModelFile, float weight,
			ScoreIndexManager scoreIndexManager, int dub) throws IOException{

		LanguageModel lm = null;
		switch (lmImplementation) {
		case RandLM:
			// #ifdef LM_RAND
			// lm = new LanguageModelRandLM(true,
			// scoreIndexManager);
			// #endif
			break;
		case Remote:
			// #ifdef LM_REMOTE
			// lm = new LanguageModelRemote(true,scoreIndexManager);
			// #endif
			break;

		case SRI:
			// #ifdef LM_SRI
			// lm = new LanguageModelSRI(true, scoreIndexManager);
			// #elif LM_INTERNAL
			lm = new LanguageModelInternal(true, scoreIndexManager);
			// #endif
			break;
		case IRST:
			// #ifdef LM_IRST
			// lm = new LanguageModelIRST(true, scoreIndexManager, dub);
			// #endif
			break;
		case Skip:
			// #ifdef LM_SRI
			// lm = new LanguageModelSkip(new LanguageModelSRI(false,
			// scoreIndexManager)
			// , true
			// , scoreIndexManager);
			// #elif LM_INTERNAL
			lm = new LanguageModelSkip(new LanguageModelInternal(false,
					scoreIndexManager), true, scoreIndexManager);
			// #endif
			break;
		case Joint:
			// #ifdef LM_SRI
			// lm = new LanguageModelJoint(new LanguageModelSRI(false,
			// scoreIndexManager)
			// , true
			// , scoreIndexManager);
			// #elif LM_INTERNAL
			lm = new LanguageModelJoint(new LanguageModelInternal(false,
					scoreIndexManager), true, scoreIndexManager);
			// #endif
			break;
		case ParallelBackoff:
			// #ifdef LM_SRI
			// lm = new LanguageModelParallelBackoff(true, scoreIndexManager);
			// #endif
			break;
		case Internal:
			// #ifdef LM_INTERNAL
			lm = new LanguageModelInternal(true, scoreIndexManager);
			// #endif
			break;
		}

		if (lm == null) {
			UserMessage
					.add("Language model type unknown. Probably not compiled into library");
		} else {
			switch (lm.getLMType()) {
			case SingleFactor:				
				if (!((LanguageModelSingleFactor)lm).load(languageModelFile, factorTypes.get(0).intValue(),
						weight, nGramOrder)) {
					System.err.println("single factor model failed");

					lm = null;
				}
				break;
			case MultiFactor:
				if (!((LanguageModelMultiFactor)lm)
						.load(languageModelFile, factorTypes, weight,
								nGramOrder)) {
					System.err.println("multi factor model failed");

					lm = null;
				}
				break;
			}
		}
		return lm;

	}

}
