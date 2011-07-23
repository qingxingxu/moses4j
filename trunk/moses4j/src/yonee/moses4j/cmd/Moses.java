package yonee.moses4j.cmd;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import yonee.moses4j.moses.InputType;
import yonee.moses4j.moses.Parameter;
import yonee.moses4j.moses.StaticData;
import yonee.moses4j.moses.Util;
import yonee.utils.TRACE;
import yonee.utils.VERBOSE;

/**
 * 
 * @author YONEE
 * @DOING
 */
public class Moses {
	public static void main(String argv[]) {
		if (VERBOSE.v(1)) {
			TRACE.err("command: ");
			for (int i = 0; i < argv.length; ++i)
				TRACE.err(argv[i] + " ");
			TRACE.err("\n");
		}

		Parameter params = new Parameter();
		if (!params.loadParam(argv)) {
			params.explain();
			return;
		}

		// create threadpool, if necessary
		int threadcount = (params.getParam("threads").size() > 0) ? Integer.valueOf(params
				.getParam("threads").get(0)) : 1;

		if (threadcount > 1) {
			System.err.print("Error: Thread count of " + threadcount
					+ " but moses not built with thread support\n");
			return;
		}

		if (!StaticData.loadDataStatic(params)) {
			return;
		}

		final StaticData staticData = StaticData.instance();
		// set up read/writing class
		IOWrapper ioWrapper = IOWrapper.getIODevice(staticData);

		if (ioWrapper == null) {
			System.err.println("Error; Failed to create IO object");
			return;
		}

		// check on weights
		List<Float> weights = staticData.getAllWeights();
		if (VERBOSE.v(2)) {
			TRACE.err("The score component vector looks like this:\n"
					+ staticData.getScoreIndexManager());
			TRACE.err("The global weight vector looks like this:");
			for (int j = 0; j < weights.size(); j++) {
				TRACE.err(" " + weights.get(j));
			}
			TRACE.err("\n");
		}
		// every score must have a weight! check that here:
		if (weights.size() != staticData.getScoreIndexManager().getTotalNumberOfScores()) {
			TRACE.err("ERROR: " + staticData.getScoreIndexManager().getTotalNumberOfScores()
					+ " score components, but " + weights.size() + " weights defined\n");
			return;
		}

		InputType source = null;
		int lineCount = 0;
		OutputCollector outputCollector = new OutputCollector();// for translations
		OutputCollector nbestCollector = new OutputCollector();

		int nbestSize = staticData.getNBestSize();
		String nbestFile = staticData.getNBestFilePath();
		FileWriter nbestOut = null;// new BufferedWriter(new FileWriter(nbestFile));
		if (nbestSize != 0) {
			if (nbestFile.equals("-") || nbestFile.equals("/dev/stdout")) {
				// nbest to stdout, no 1-best
				nbestCollector = new OutputCollector();
			} else {
				// nbest to file, 1-best to stdout
				// nbestOut.reset(new FileWriter(nbestFile));
				try {
					nbestOut = new FileWriter(nbestFile);
				} catch (IOException e) {
					System.err.println(e.getMessage());
					System.exit(1);
				} 

				// assert(nbestOut->good());
				nbestCollector = new OutputCollector(nbestOut);
				outputCollector = new OutputCollector();
			}
		} else {
			outputCollector = new OutputCollector();
		}

		OutputCollector wordGraphCollector = null;
		if (staticData.getOutputWordGraph()) {
			wordGraphCollector = new OutputCollector((ioWrapper.getOutputWordGraphStream()));
		}

		OutputCollector searchGraphCollector = null;
		if (staticData.getOutputSearchGraph()) {
			searchGraphCollector = new OutputCollector((ioWrapper.getOutputSearchGraphStream()));
		}

		OutputCollector detailedTranslationCollector = null;
		if (staticData.isDetailedTranslationReportingEnabled()) {
			detailedTranslationCollector = new OutputCollector(ioWrapper
					.getDetailedTranslationReportingStream());
		}

		while ((source = IOWrapper.readInput(ioWrapper, staticData.getInputType())) != null) {
			if (VERBOSE.v(1)) {
				Util.resetUserTime();
			}
			TranslationTask task = new TranslationTask(lineCount, source, outputCollector,
					nbestCollector, wordGraphCollector, searchGraphCollector,
					detailedTranslationCollector);
			try {
				task.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
			source = null; // make sure it doesn't get deleted
			++lineCount;
		}

	}
}
