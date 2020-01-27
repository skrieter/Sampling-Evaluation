package de.ovgu.featureide.sampling;

import java.util.Arrays;

import de.ovgu.featureide.fm.benchmark.ABenchmark;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.FeatureModelReader;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.NoAbstractCNFCreator;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.sampling.eval.Constants;
import de.ovgu.featureide.sampling.eval.PCProcessor;
import de.ovgu.featureide.sampling.eval.analyzer.PresenceConditionList;

public class PCConverter extends ABenchmark {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Configuration path and name not specified!");
			return;
		}
		final PCConverter evaluator = new PCConverter(args[0], args[1]);
		evaluator.init();
		evaluator.run();
		evaluator.dispose();
	}

	protected CSVWriter conversionWriter;

	public PCConverter(String configPath, String configName) throws Exception {
		super(configPath, configName);
	}

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		conversionWriter = addCSVWriter("conversion.csv",
				Arrays.asList("ID", "Mode", "Iteration", "Time", "Size", "Error"));
	}

	@Override
	public void run() {
		super.run();

		if (config.systemIterations.getValue() > 0) {
			Logger.getInstance().logInfo("Start", false);
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
					evalConvert(pcfmProcessor, Constants.convertedPCFMFileName);

					final PCProcessor pcProcessor = new PCProcessor();
					pcProcessor.init(null, systemName);
					evalConvert(pcProcessor, Constants.convertedPCFileName);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Logger.getInstance().logInfo("Finished", false);
		} else {
			Logger.getInstance().logInfo("Nothing to do", false);
		}
	}

	private PresenceConditionList evalConvert(final PCProcessor pcProcessor, String fileName) {
		PresenceConditionList pcList = null;
		for (int i = 0; i < config.systemIterations.getValue(); i++) {
			conversionWriter.createNewLine();
			try {
				conversionWriter.addValue(systemIndex);
				conversionWriter.addValue(fileName);
				conversionWriter.addValue(i);

				final long localTime = System.nanoTime();
				pcList = pcProcessor.convert();
				long timeNeeded = System.nanoTime() - localTime;

				conversionWriter.addValue(timeNeeded);
				conversionWriter.addValue(pcList != null ? pcList.size() : 0);
				conversionWriter.addValue(pcList == null);

				Logger.getInstance().logInfo("convert -> " + Double.toString((timeNeeded / 1_000_000) / 1_000.0), true);
			} catch (Exception e) {
				conversionWriter.resetLine();
				e.printStackTrace();
			} finally {
				conversionWriter.flush();
			}
		}

		if (pcList != null) {
			PresenceConditionList.writePCList(pcList, config.systemNames.get(systemIndex), fileName);
		}
		return pcList;
	}

}
