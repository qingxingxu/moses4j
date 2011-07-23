package yonee.moses4j.moses;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yonee.utils.TRACE;
/**
 * 
 * @author YONEE
 * @OK not verify
 */
public class Parameter {

	protected Map<String, List<String>> setting = new HashMap<String, List<String>>();
	protected Map<String, Boolean> valid = new HashMap<String, Boolean>(); 
	protected Map<String, String> abbreviation = new HashMap<String, String>();
	protected Map<String, String> description = new HashMap<String, String>();

	protected String findParam(final String paramSwitch, String argv[]) {
		for (int i = 0; i < argv.length; i++) {
			if (argv[i].equals(paramSwitch)) {
				if (i + 1 < argv.length) {
					return argv[i + 1];
				} else {
					StringBuilder errorMsg = new StringBuilder("");
					errorMsg.append("Option ").append(paramSwitch).append(
							" requires a parameter!");
					UserMessage.add(errorMsg.toString());
					// TODO return some sort of error, not the empty string
				}
			}
		}
		return "";
	}

	protected void overwriteParam(final String paramSwitch,
			final String paramName, String argv[]) {
		int startPos = -1;
		for (int i = 0; i < argv.length; i++) {
			if (argv[i].equals(paramSwitch)) {
				startPos = i + 1;
				break;
			}
		}
		if (startPos < 0)
			return;

		int index = 0;
		setting.put(paramName, new ArrayList<String>()); // defines the parameter, important for
		// boolean switches
		while (startPos < argv.length && (!isOption(argv[startPos]))) {
			if (setting.get(paramName).size() > index)
				setting.get(paramName).set(index, argv[startPos]);
			else
				setting.get(paramName).add(argv[startPos]);
			index++;
			startPos++;
		}
	}

	protected boolean readConfigFile(final String filePath) throws IOException {

		String line = null, paramName = null;

		BufferedReader br = new BufferedReader(new FileReader(filePath));
		while ((line = br.readLine()) != null) {
			// comments
			int comPos = line.indexOf("#");
			if (comPos != -1)
				line = line.substring(0, comPos);
			// trim leading and trailing spaces/tabs
			line = line.trim();
			if (line.length() == 0)
				continue;

			if (line.charAt(0) == '[') { // new parameter
				for (int currPos = 0; currPos < line.length(); currPos++) {
					if (line.charAt(currPos) == ']') {
						paramName = line.substring(1, currPos);
						break;
					}
				}
			} else if (!line.equals("")) { // add value to parameter	
				List<String> l = setting.get(paramName);
				if( l == null ){
					l = new ArrayList<String>();
					setting.put(paramName, l);
				}
				l.add(line);
			}
		}
		br.close();
		return true;
	}

	protected boolean filesExist(final String paramName, int fieldNo) {
		List<String> l = new ArrayList<String>();
		l.add("");
		return filesExist(paramName, fieldNo, l);
	}

	protected boolean filesExist(final String paramName, int fieldNo,
			List<String> extensions) {

		if (!setting.containsKey(paramName)) {
			return true;
		}

		final List<String> pathVec = setting.get(paramName);

		for (String iter : pathVec) {
			String vec[] = iter.split(" \t"); // YONEE
			int tokenizeIndex;
			if (fieldNo == -1)
				tokenizeIndex = vec.length - 1;
			else
				tokenizeIndex = fieldNo;

			if (tokenizeIndex >= vec.length) {
				StringBuilder errorMsg = new StringBuilder("");
				errorMsg.append("Expected at least ").append(tokenizeIndex + 1)
						.append(" tokens per entry in '").append(paramName)
						.append("', but only found ").append(vec.length);
				UserMessage.add(errorMsg.toString());
				return false;
			}
			final String pathStr = vec[tokenizeIndex];

			boolean fileFound = false;
			for (int i = 0; i < extensions.size() && !fileFound; ++i) {
				fileFound |= Util.fileExists(pathStr + extensions.get(i));
			}
			if (!fileFound) {
				StringBuilder errorMsg = new StringBuilder("");
				errorMsg.append("File ").append(pathStr).append(
						" does not exist");
				UserMessage.add(errorMsg.toString());
				return false;
			}
		}
		return true;
	}

	protected boolean isOption(final String token) {
		if (token == null)
			return false;
		String tokenString = new String(token);

		int length = tokenString.length();
		if (length > 0 && tokenString.charAt(0) != '-')
			return false;
		if (length > 1 && !Character.isDigit(tokenString.charAt(1)))
			return true; // ·ÇÊý×Ö
		return false;
	}

	protected boolean validate() {
		boolean noErrorFlag = true;

		// required parameters
		if (getParam("ttable-file").size() == 0) {
			UserMessage.add("No phrase translation table (ttable-file)");
			noErrorFlag = false;
		}

		if (getParam("lmodel-dub").size() > 0) {
			if (getParam("lmodel-file").size() != getParam("lmodel-dub")
					.size()) {
				StringBuilder errorMsg = new StringBuilder("");
				errorMsg.append("Config and parameters specify ").append(
						setting.get("lmodel-file").size()).append(
						" language model files (lmodel-file), but ").append(
						setting.get("lmodel-dub").size()).append(
						" LM upperbounds (lmodel-dub)").append("\n");
				UserMessage.add(errorMsg.toString());
				noErrorFlag = false;
			}
		}

		if (getParam("lmodel-file").size() != getParam("weight-l").size()) {
			StringBuilder errorMsg = new StringBuilder("");
			errorMsg.append("Config and parameters specify ").append(
					setting.get("lmodel-file").size()).append(
					" language model files (lmodel-file), but ").append(
					setting.get("weight-l").size()).append(
					" weights (weight-l)\n");
			errorMsg
					.append("You might be giving '-lmodel-file TYPE FACTOR ORDER FILENAME' but you should be giving these four as a single argument, i.e. '-lmodel-file \"TYPE FACTOR ORDER FILENAME\"'");
			UserMessage.add(errorMsg.toString());
			noErrorFlag = false;
		}

		// do files exist?

		// input file
		if (noErrorFlag && getParam("input-file").size() == 1) {
			noErrorFlag = Util.fileExists(setting.get("input-file").get(0));
		}
		// generation tables
		if (noErrorFlag) {
			List<String> ext = new ArrayList<String>();
			// raw tables in either un compressed or compressed form
			ext.add("");
			ext.add(".gz");
			noErrorFlag = filesExist("generation-file", 3, ext);
		}
		// distortion
		if (noErrorFlag) {
			List<String> ext = new ArrayList<String>();
			// raw tables in either un compressed or compressed form
			ext.add("");
			ext.add(".gz");
			// prefix tree format
			ext.add(".binlexr.idx");
			noErrorFlag = filesExist("distortion-file", 3, ext);
		}
		return noErrorFlag;

	}

	protected void addParam(final String paramName, final String description) {
		this.valid.put(paramName, true);
		this.description.put(paramName, description);
	}

	protected void addParam(final String paramName, final String abbrevName,
			final String description) {
		this.valid.put(paramName, true);
		this.valid.put(abbrevName, true);
		this.abbreviation.put(paramName, abbrevName);
		this.description.put(paramName, description);
	}

	protected void printCredit() {

	}

	public Parameter() {
		addParam("beam-threshold", "b", "threshold for threshold pruning");
		addParam("config", "f", "location of the configuration file");
		addParam("continue-partial-translation", "cpt",
				"start from nonempty hypothesis");
		addParam("drop-unknown", "du",
				"drop unknown words instead of copying them");
		addParam("disable-discarding", "dd", "disable hypothesis discarding");
		addParam("factor-delimiter", "fd",
				"specify a different factor delimiter than the default");
		addParam("generation-file",
				"location and properties of the generation table");
		addParam("global-lexical-file", "gl",
				"discriminatively trained global lexical translation model file");
		addParam("input-factors", "list of factors in the input");
		addParam("input-file", "i",
				"location of the input file to be translated");
		addParam("inputtype",
				"text (0), confusion network (1), word lattice (2) (default = 0)");
		addParam("labeled-n-best-list",
				"print out labels for each weight type in n-best list. default is true");
		addParam("include-alignment-in-n-best",
				"include word alignment in the n-best list. default is false");
		addParam("lmodel-file",
				"location and properties of the language models");
		addParam("lmodel-dub", "dictionary upper bounds of language models");
		addParam("mapping", "description of decoding steps");
		addParam(
				"max-partial-trans-opt",
				"maximum number of partial translation options per input span (during mapping steps)");
		addParam(
				"max-trans-opt-per-coverage",
				"maximum number of translation options per input span (after applying mapping steps)");
		addParam("max-phrase-length", "maximum phrase length (default 20)");
		addParam(
				"n-best-list",
				"file and size of n-best-list to be generated; specify - as the file in order to write to STDOUT");
		addParam(
				"n-best-factor",
				"factor to compute the maximum number of contenders (=factor*nbest-size). value 0 means infinity, i.e. no threshold. default is 0");
		addParam("print-all-derivations",
				"to print all derivations in search graph");
		addParam("output-factors", "list of factors in the output");
		addParam("phrase-drop-allowed", "da",
				"if present, allow dropping of source words"); // da = drop any
		// (word); see
		// -du for
		// comparison
		addParam("report-all-factors",
				"report all factors in output, not just first");
		addParam("report-all-factors-in-n-best",
				"Report all factors in n-best-lists. Default is false");
		addParam("report-segmentation", "t",
				"report phrase segmentation in the output");
		addParam("stack", "s", "maximum stack size for histogram pruning");
		addParam("stack-diversity", "sd",
				"minimum number of hypothesis of each coverage in stack (default 0)");
		addParam("threads", "th",
				"number of threads to use in decoding (defaults to single-threaded)");
		addParam("translation-details", "T",
				"for each best hypothesis, report translation details to the given file");
		addParam("ttable-file",
				"location and properties of the translation tables");
		addParam("ttable-limit", "ttl",
				"maximum number of translation table entries per input phrase");
		addParam("translation-option-threshold", "tot",
				"threshold for translation options relative to best for input phrase");
		addParam("early-discarding-threshold", "edt",
				"threshold for constructing hypotheses based on estimate cost");
		addParam("verbose", "v", "verbosity level of the logging");
		addParam("weight-d", "d",
				"weight(s) for distortion (reordering components)");
		addParam("weight-generation", "g",
				"weight(s) for generation components");
		addParam(
				"weight-i",
				"I",
				"weight(s) for word insertion - used for parameters from confusion network and lattice input links");
		addParam("weight-l", "lm", "weight(s) for language models");
		addParam("weight-lex", "lex", "weight for global lexical model");
		addParam("weight-t", "tm", "weights for translation model components");
		addParam("weight-w", "w", "weight for word penalty");
		addParam("weight-u", "u", "weight for unknown word penalty");
		addParam("weight-e", "e", "weight for word deletion");
		addParam("weight-file", "wf", "file containing labeled weights");
		addParam("output-factors", "list if factors in the output");
		addParam("cache-path", "?");
		addParam(
				"distortion-limit",
				"dl",
				"distortion (reordering) limit in maximum number of words (0 = monotone, -1 = unlimited)");
		addParam("monotone-at-punctuation", "mp",
				"do not reorder over punctuation");
		addParam(
				"distortion-file",
				"source factors (0 if table independent of source), target factors, location of the factorized/lexicalized reordering tables");
		addParam("distortion",
				"configurations for each factorized/lexicalized reordering model.");
		addParam(
				"xml-input",
				"xi",
				"allows markup of input with desired translations and probabilities. values can be 'pass-through' (default), 'inclusive', 'exclusive', 'ignore'");
		addParam("minimum-bayes-risk", "mbr",
				"use miminum Bayes risk to determine best translation");
		addParam("lminimum-bayes-risk", "lmbr",
				"use lattice miminum Bayes risk to determine best translation");
		addParam("consensus-decoding", "con",
				"use consensus decoding (De Nero et. al. 2009)");
		addParam("mbr-size",
				"number of translation candidates considered in MBR decoding (default 200)");
		addParam(
				"mbr-scale",
				"scaling factor to convert log linear score probability in MBR decoding (default 1.0)");
		addParam("lmbr-thetas", "theta(s) for lattice mbr calculation");
		addParam("lmbr-pruning-factor",
				"average number of nodes/word wanted in pruned lattice");
		addParam("lmbr-p", "unigram precision value for lattice mbr");
		addParam("lmbr-r", "ngram precision decay value for lattice mbr");
		addParam("lmbr-map-weight",
				"weight given to map solution when doing lattice MBR (default 0)");
		addParam("lattice-hypo-set",
				"to use lattice as hypo set during lattice MBR");
		addParam("clean-lm-cache",
				"clean language model caches after N translations (default N=1)");
		addParam("use-persistent-cache",
				"cache translation options across sentences (default true)");
		addParam("persistent-cache-size",
				"maximum size of cache for translation options (default 10,000 input phrases)");
		addParam(
				"recover-input-path",
				"r",
				"(conf net/word lattice only) - recover input path corresponding to the best translation");
		addParam(
				"output-word-graph",
				"owg",
				"Output stack info as word graph. Takes filename, 0=only hypos in stack, 1=stack + nbest hypos");
		addParam("time-out",
				"seconds after which is interrupted (-1=no time-out, default is -1)");
		addParam("output-search-graph", "osg",
				"Output connected hypotheses of search into specified filename");
		addParam(
				"output-search-graph-extended",
				"osgx",
				"Output connected hypotheses of search into specified filename, in extended format");
		/*
		 * #ifdef HAVE_PROTOBUF addParam("output-search-graph-pb", "pb",
		 * "Write phrase lattice to protocol buffer objects in the specified path."
		 * ); #endif
		 */
		addParam("cube-pruning-pop-limit", "cbp",
				"How many hypotheses should be popped for each stack. (default = 1000)");
		addParam("cube-pruning-diversity", "cbd",
				"How many hypotheses should be created for each coverage. (default = 0)");
		addParam(
				"search-algorithm",
				"Which search algorithm to use. 0=normal stack, 1=cube pruning, 2=cube growing. (default = 0)");
		addParam("constraint",
				"Location of the file with target sentences to produce constraining the search");
		addParam(
				"use-alignment-info",
				"Use word-to-word alignment: actually it is only used to output the word-to-word alignment. Word-to-word alignments are taken from the phrase table if any. Default is false.");
		addParam(
				"print-alignment-info",
				"Output word-to-word alignment into the log file. Word-to-word alignments are takne from the phrase table if any. Default is false");
		addParam(
				"print-alignment-info-in-n-best",
				"Include word-to-word alignment in the n-best list. Word-to-word alignments are takne from the phrase table if any. Default is false");
		addParam(
				"link-param-count",
				"Number of parameters on word links when using confusion networks or lattices (default = 1)");
		addParam("description", "Source language, target language, description");

		addParam("max-chart-span",
				"maximum num. of source word chart rules can consume (default 10)");
		addParam("non-terminals", "list of non-term symbols, space separated");
		addParam(
				"rule-limit",
				"a little like table limit. But for chart decoding rules. Default is DEFAULT_MAX_TRANS_OPT_SIZE");
		addParam(
				"source-label-overlap",
				"What happens if a span already has a label. 0=add more. 1=replace. 2=discard. Default is 0");
		addParam(
				"glue-rule-type",
				"Left branching, or both branching. 0=left. 2=both. 1=right(not implemented). Default=0");
		addParam(
				"output-hypo-score",
				"Output the hypo score to stdout with the output string. For search error analysis. Default is false");
		addParam("unknown-lhs",
				"file containing target lhs of unknown words. 1 per line: LHS prob");
	}

	public boolean loadParam(String argv[]) {
		// config file (-f) arg mandatory
		String configPath;
		if ((configPath = findParam("-f", argv)) == ""
				&& (configPath = findParam("-config", argv)) == "") {
			printCredit();
			explain();

			UserMessage
					.add("No configuration file was specified.  Use -config or -f");
			return false;
		} else {
			boolean flag = false;
			String errMsg = "";
			try {
				flag = readConfigFile(configPath);
			} catch (IOException e) {
				flag = false;
				errMsg = e.getMessage();
			}
			if (!flag) {
				UserMessage.add("Could not read " + configPath + "," + errMsg);
				return false;
			}

		}

		// overwrite parameters with values from switches
		for (Map.Entry<String, String> e : description.entrySet()) {
			final String paramName = e.getKey();
			overwriteParam("-" + paramName, paramName, argv);
		}

		// ... also shortcuts
		for (Map.Entry<String, String> e : abbreviation.entrySet()) {
			final String paramName = e.getKey();
			final String paramShortName = e.getValue();
			overwriteParam("-" + paramShortName, paramName, argv);
		}

		// logging of parameters that were set in either config or switch
		int verbose = 1;
		if (setting.containsKey("verbose") && setting.get("verbose").size() > 0)
			verbose = Integer.valueOf(setting.get("verbose").get(0));
		if (verbose >= 1) { // only if verbose
			TRACE.err("Defined parameters (per moses.ini or switch):\n");
			for (Map.Entry<String, List<String>> e : setting.entrySet()) {
				TRACE.err("\t" + e.getKey() + ": ");
				for (String s : e.getValue()) {
					TRACE.err(s + " ");
				}
				TRACE.err("\n");
			}
		}

		// check for illegal parameters
		boolean noErrorFlag = true;
		for (int i = 0; i < argv.length; i++) {
			if (isOption(argv[i])) {
				String paramSwitch = argv[i];
				String paramName = paramSwitch.substring(1);
				if (!valid.containsKey(paramName)) {
					UserMessage.add("illegal switch: " + paramSwitch);
					noErrorFlag = false;
				}
			}
		}
		// check if parameters make sense
		return validate() && noErrorFlag;
	}

	public boolean loadParam(String filePath) {
		String argv[] = { "executable", "-f", filePath };
		return loadParam(argv);
	}

	public void explain() {
		System.err.println("Usage:");
		for (Map.Entry<String, String> e : this.description.entrySet()) {
			System.err.print("\t-" + e.getKey());
			String s = abbreviation.get(e.getKey());
			if (s != null) {
				System.err.print(" (" + s + ")");
			}
			System.err.println(": " + e.getValue());
		}
	}

	/**
	 * return a vector of strings holding the whitespace-delimited values on the
	 * ini-file line corresponding to the given parameter name
	 */
	public final List<String> getParam(final String paramName) {		
		List<String> l= setting.get(paramName);
		if( l == null){
			l = new ArrayList<String>();
			setting.put(paramName, l);
		}
		return l;		
	}

	/** check if parameter is defined (either in moses.ini or as switch) */
	public boolean isParamSpecified(final String paramName) {
		return setting.containsKey(paramName);
	}
}
