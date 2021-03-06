package de.ovgu.featureide.sampling;

import java.io.IOException;
import java.util.Arrays;

import org.sk.utils.Logger;
import org.sk.utils.io.CSVWriter;

import de.ovgu.featureide.fm.benchmark.ABenchmark;
import de.ovgu.featureide.fm.benchmark.util.FeatureModelReader;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.NoAbstractCNFCreator;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.sampling.eval.PCProcessor;

public class PCExtractor extends ABenchmark {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Configuration path and name not specified!");
			return;
		}
		final PCExtractor evaluator = new PCExtractor(args[0], args[1]);
		evaluator.init();
		evaluator.run();
		evaluator.dispose();
	}

	protected CSVWriter extractionWriter;

	public PCExtractor(String configPath, String configName) throws Exception {
		super(configPath, configName);
	}

	@Override
	protected void addCSVWriters() throws IOException {
		super.addCSVWriters();
		extractionWriter = addCSVWriter("extraction.csv",
				Arrays.asList("ID", "Mode", "Iteration", "Time", "Size", "Error"));
	}

	@Override
	public void run() {
		super.run();

		if (config.systemIterations.getValue() > 0) {
			Logger.getInstance().logInfo("Start", 0);

			final int systemIndexEnd = config.systemNames.size();
			for (systemIndex = 0; systemIndex < systemIndexEnd; systemIndex++) {
				logSystem();
				final String systemName = config.systemNames.get(systemIndex);

				FeatureModelReader fmReader = new FeatureModelReader();
				fmReader.setPathToModels(config.modelPath);
				IFeatureModel fm = fmReader.read(systemName);
				if (fm == null) {
					throw new NullPointerException();
				}
				final CNF cnf = new FeatureModelFormula(fm).getElement(new NoAbstractCNFCreator()).normalize();

				try {
					final PCProcessor pcfmProcessor = new PCProcessor();
					pcfmProcessor.init(cnf, systemName);
					evalExtract(pcfmProcessor);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Logger.getInstance().logInfo("Finished", 0);
		} else {
			Logger.getInstance().logInfo("Nothing to do", 0);
		}
	}

	private void evalExtract(final PCProcessor pcProcessor) {
		for (int i = 0; i < config.systemIterations.getValue(); i++) {
			extractionWriter.createNewLine();
			try {
				extractionWriter.addValue(config.systemIDs.get(systemIndex));
				extractionWriter.addValue("extract");
				extractionWriter.addValue(i);

				final long localTime = System.nanoTime();
				boolean extracted = pcProcessor.extract();
				long timeNeeded = System.nanoTime() - localTime;

				extractionWriter.addValue(timeNeeded);
				extractionWriter.addValue(0);
				extractionWriter.addValue(!extracted);

				Logger.getInstance().logInfo("extract -> " + Double.toString((timeNeeded / 1_000_000) / 1_000.0), 1);
			} catch (Exception e) {
				extractionWriter.resetLine();
				e.printStackTrace();
			} finally {
				extractionWriter.flush();
			}
		}
	}

}
