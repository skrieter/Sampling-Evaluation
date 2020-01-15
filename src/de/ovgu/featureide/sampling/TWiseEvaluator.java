package de.ovgu.featureide.sampling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import de.ovgu.featureide.fm.benchmark.properties.IntProperty;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;
import de.ovgu.featureide.fm.core.analysis.cnf.analysis.CountSolutionsAnalysis;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.NoAbstractCNFCreator;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.sampling.eval.Constants;
import de.ovgu.featureide.sampling.eval.Expressions;
import de.ovgu.featureide.sampling.eval.PCProcessor;
import de.ovgu.featureide.sampling.eval.analyzer.PresenceConditionList;
import de.ovgu.featureide.sampling.eval.properties.GroupingProperty;

public class TWiseEvaluator extends ATWiseEvaluator {

	public static void main(String[] args) throws Exception {
		final TWiseEvaluator evaluator;
		if (args.length == 1) {
			evaluator = new TWiseEvaluator(args[0]);
		} else {
			evaluator = new TWiseEvaluator(null);
		}
		evaluator.run();
		evaluator.dispose();
	}

	protected static final GroupingProperty grouping = new GroupingProperty();
	protected static final IntProperty extractIterations = new IntProperty("extract", 0);
	protected static final IntProperty convertIterations = new IntProperty("convert", 0);
	protected static final IntProperty groupIterations = new IntProperty("group", 0);

	protected CSVWriter extractionWriter;

	public TWiseEvaluator(String configPath) throws Exception {
		super(configPath);
	}

	@Override
	protected void initCSVWriters() {
		extendModelCSVWriter(Arrays.asList("Configurations", "FMFeatures", "FMConstraints", "FMPCs", "FMPCFeatures", "PCFeatures", "PCConstraints", "PCs", "MaxMutants"));
//		extendModelCSVWriter(Arrays.asList("Configurations", "FMFeatures", "FMConstraints"));
		extendDataCSVWriter(Arrays.asList("Size", "Valid", "Complete"));
		extractionWriter = createCSVWriter("extraction.csv", Arrays.asList("ID", "Grouping", "Iteration", "Time", "Size", "Error"));
	}

	@Override
	protected void prepareModel(String systemName, int id, int systemIteration) throws Exception {
		boolean extract = extractIterations.getValue() > 0;
		boolean convert = convertIterations.getValue() > 0;
		boolean group = groupIterations.getValue() > 0;

		try {
			if (extract || convert || group) {
				final IFeatureModel fm = init(systemName);
				CNF fmFormula = new FeatureModelFormula(fm).getElement(new NoAbstractCNFCreator()).normalize();

				final PCProcessor pcfmProcessor = new PCProcessor();
				pcfmProcessor.init(fmFormula, systemName);
				evalExtract(id, pcfmProcessor);
				evalConvert(systemName, id, pcfmProcessor, Constants.convertedPCFMFileName);
				evalGroup(systemName, id, GroupingProperty.FM_ONLY, pcfmProcessor);
				evalGroup(systemName, id, GroupingProperty.PC_ALL_FM, pcfmProcessor);
				evalGroup(systemName, id, GroupingProperty.PC_FOLDER_FM, pcfmProcessor);
				evalGroup(systemName, id, GroupingProperty.PC_FILE_FM, pcfmProcessor);
				evalGroup(systemName, id, GroupingProperty.PC_VARS_FM, pcfmProcessor);

				final PCProcessor pcProcessor = new PCProcessor();
				pcProcessor.init(null, systemName);
				evalConvert(systemName, id, pcProcessor, Constants.convertedPCFileName);
				evalGroup(systemName, id, GroupingProperty.PC_ALL, pcProcessor);
				evalGroup(systemName, id, GroupingProperty.PC_FOLDER, pcProcessor);
				evalGroup(systemName, id, GroupingProperty.PC_FILE, pcProcessor);
				evalGroup(systemName, id, GroupingProperty.PC_VARS, pcProcessor);
			}

			getModelCSVWriter().createNewLine();
			getModelCSVWriter().addValue(id);
			getModelCSVWriter().addValue(systemName);

			ArrayList<String> fmNames = null, pcNames = null;
			try {
				PresenceConditionList pcfmList = PresenceConditionList.readPCList(systemName,
						Constants.convertedPCFMFileName);
				CNF formula = pcfmList.getFormula();
				CountSolutionsAnalysis countAnalysis = new CountSolutionsAnalysis(formula);
				countAnalysis.setTimeout(0);
				getModelCSVWriter().addValue(LongRunningWrapper.runMethod(countAnalysis));
				getModelCSVWriter().addValue(formula.getVariables().size());
				getModelCSVWriter().addValue(formula.getClauses().size());
				getModelCSVWriter().addValue(pcfmList.size());
				getModelCSVWriter().addValue(pcfmList.getPCNames().size());

				String[] names = formula.getVariables().getNames();
				fmNames = new ArrayList<>(Arrays.asList(names).subList(1, names.length));
				Collections.sort(fmNames);
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				getModelCSVWriter().addValue(0);
				getModelCSVWriter().addValue(-1);
				getModelCSVWriter().addValue(-1);
				getModelCSVWriter().addValue(-1);
				getModelCSVWriter().addValue(-1);
			}

			try {
				PresenceConditionList pcList = PresenceConditionList.readPCList(systemName,
						Constants.convertedPCFileName);
				CNF formula = pcList.getFormula();
				getModelCSVWriter().addValue(formula.getVariables().size());
				getModelCSVWriter().addValue(formula.getClauses().size());
				getModelCSVWriter().addValue(pcList.size());

				String[] names = formula.getVariables().getNames();
				pcNames = new ArrayList<>(Arrays.asList(names).subList(1, names.length));
				Collections.sort(pcNames);
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
				getModelCSVWriter().addValue(-1);
				getModelCSVWriter().addValue(-1);
				getModelCSVWriter().addValue(-1);
			}

//			getModelsCSVWriter().addValue(mutants.size());
			getModelCSVWriter().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Expressions readExpressions(String name, String grouping) {
		try {
			return Expressions.readConditions(name, Constants.groupedPCFileName + grouping);
		} catch (IOException e) {
			return null;
		}
	}

	private PresenceConditionList evalConvert(String name, int id, final PCProcessor pcProcessor, String fileName) {
		PresenceConditionList pcList = null;
		for (int i = 0; i < convertIterations.getValue(); i++) {
			try {
				extractionWriter.createNewLine();
				extractionWriter.addValue(id);
				extractionWriter.addValue("convert");
				extractionWriter.addValue(i);

				final long localTime = System.nanoTime();
				pcList = pcProcessor.convert();
				long timeNeeded = System.nanoTime() - localTime;

				extractionWriter.addValue(timeNeeded);
				extractionWriter.addValue(pcList != null ? pcList.size() : 0);
				extractionWriter.addValue(pcList == null);

				Logger.getInstance().logInfo("convert -> " + Double.toString((timeNeeded / 1_000_000) / 1_000.0), true);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				extractionWriter.flush();
			}
		}

		if (pcList != null) {
			PresenceConditionList.writePCList(pcList, name, fileName);
		}
		return pcList;
	}

	private Expressions evalGroup(String name, int id, String groupingValue, final PCProcessor pcProcessor) {
		Expressions expressions = null;
		for (int i = 0; i < groupIterations.getValue(); i++) {
			try {
				extractionWriter.createNewLine();
				extractionWriter.addValue(id);
				extractionWriter.addValue(groupingValue);
				extractionWriter.addValue(i);

				final long localTime = System.nanoTime();
				expressions = pcProcessor.group(groupingValue);
				long timeNeeded = System.nanoTime() - localTime;

				extractionWriter.addValue(timeNeeded);

				if (expressions != null) {
					final HashSet<ClauseList> pcs = new HashSet<>();
					for (List<ClauseList> group : expressions.getExpressions()) {
						for (ClauseList pc : group) {
							pcs.add(pc);
						}
					}
					extractionWriter.addValue(pcs.size());
					extractionWriter.addValue(false);
				} else {
					extractionWriter.addValue(0);
					extractionWriter.addValue(true);
				}

				Logger.getInstance()
						.logInfo(groupingValue + " -> " + Double.toString((timeNeeded / 1_000_000) / 1_000.0), true);
			} catch (FileNotFoundException e) {
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				extractionWriter.flush();
			}
		}

		if (expressions != null) {
			Expressions.writeConditions(expressions, name, Constants.groupedPCFileName + groupingValue);
		}
		return expressions;
	}

	private void evalExtract(int id, final PCProcessor pcProcessor) {
		for (int i = 0; i < extractIterations.getValue(); i++) {
			try {
				extractionWriter.createNewLine();
				extractionWriter.addValue(id);
				extractionWriter.addValue("extract");
				extractionWriter.addValue(i);

				final long localTime = System.nanoTime();
				boolean extracted = pcProcessor.extract();
				long timeNeeded = System.nanoTime() - localTime;

				extractionWriter.addValue(timeNeeded);
				extractionWriter.addValue(0);
				extractionWriter.addValue(!extracted);

				Logger.getInstance().logInfo("extract -> " + Double.toString((timeNeeded / 1_000_000) / 1_000.0), true);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				extractionWriter.flush();
			}
		}
	}

}
