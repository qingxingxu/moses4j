package yonee.moses4j.moses;

import java.util.ArrayList;

/**
 * 
 * @author YONEE
 * @OK
 */
public class TranslationOptionList extends ArrayList<TranslationOption> {

	private static final long serialVersionUID = 1L;

	public TranslationOptionList(TranslationOptionList copy) {
		for (TranslationOption origTransOpt : copy) {
			add(new TranslationOption(origTransOpt));
		}
	}
	public TranslationOptionList() {
	
	}

}
