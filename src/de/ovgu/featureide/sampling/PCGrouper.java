package de.ovgu.featureide.sampling;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import de.ovgu.featureide.fm.benchmark.ABenchmark;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.FeatureModelReader;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.NoAbstractCNFCreator;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.sampling.eval.Constants;
import de.ovgu.featureide.sampling.eval.Expressions;
import de.ovgu.featureide.sampling.eval.PCProcessor;
import de.ovgu.featureide.sampling.eval.properties.GroupingProperty;

public class PCGrouper extends ABenchmark {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Configuration path and name not specified!");
			return;
		}
		final PCGrouper evaluator = new PCGrouper(args[0], args[1]);
		evaluator.init();
		evaluator.run();
		evaluator.dispose();
	}

	protected CSVWriter groupingWriter;

	public PCGrouper(String configPath, String configName) throws Exception {
		super(configPath, configName);
	}

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		groupingWriter = addCSVWriter("grouping.csv",
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
					evalGroup(GroupingProperty.FM_ONLY, pcfmProcessor);
					evalGroup(GroupingProperty.PC_ALL_FM, pcfmProcessor);
					evalGroup(GroupingProperty.PC_FOLDER_FM, pcfmProcessor);
					evalGroup(GroupingProperty.PC_FILE_FM, pcfmProcessor);
					evalGroup(GroupingProperty.PC_VARS_FM, pcfmProcessor);

					final PCProcessor pcProcessor = new PCProcessor();
					pcProcessor.init(null, systemName);
					evalGroup(GroupingProperty.PC_ALL, pcProcessor);
					evalGroup(GroupingProperty.PC_FOLDER, pcProcessor);
					evalGroup(GroupingProperty.PC_FILE, pcProcessor);
					evalGroup(GroupingProperty.PC_VARS, pcProcessor);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Logger.getInstance().logInfo("Finished", false);
		} else {
			Logger.getInstance().logInfo("Nothing to do", false);
		}
	}

	private Expressions evalGroup(String groupingValue, final PCProcessor pcProcessor) {
		Expressions expressions = null;
		for (int i = 0; i < config.systemIterations.getValue(); i++) {
			groupingWriter.createNewLine();
			try {
				groupingWriter.addValue(systemIndex);
				groupingWriter.addValue(groupingValue);
				groupingWriter.addValue(i);

				final long localTime = System.nanoTime();
				expressions = pcProcessor.group(groupingValue);
				long timeNeeded = System.nanoTime() - localTime;

				groupingWriter.addValue(timeNeeded);

				if (expressions != null) {
					final HashSet<ClauseList> pcs = new HashSet<>();
					for (List<ClauseList> group : expressions.getExpressions()) {
						pcs.addAll(group);
					}
					groupingWriter.addValue(pcs.size());
					groupingWriter.addValue(false);
				} else {
					groupingWriter.addValue(0);
					groupingWriter.addValue(true);
				}

				Logger.getInstance()
						.logInfo(groupingValue + " -> " + Double.toString((timeNeeded / 1_000_000) / 1_000.0), true);
			} catch (FileNotFoundException e) {
				groupingWriter.resetLine();
			} catch (Exception e) {
				groupingWriter.resetLine();
				e.printStackTrace();
			} finally {
				groupingWriter.flush();
			}
		}

		if (expressions != null) {
			Expressions.writeConditions(expressions, config.systemNames.get(systemIndex),
					Constants.groupedPCFileName + groupingValue);
		}
		return expressions;
	}

}
