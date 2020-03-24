package de.ovgu.featureide.sampling;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import de.ovgu.featureide.fm.benchmark.AAlgorithmBenchmark;
import de.ovgu.featureide.fm.benchmark.process.Algorithm;
import de.ovgu.featureide.fm.benchmark.properties.IntProperty;
import de.ovgu.featureide.fm.benchmark.properties.StringListProperty;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.FeatureModelReader;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet.Order;
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList;
import de.ovgu.featureide.fm.core.analysis.cnf.analysis.CountSolutionsAnalysis;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.NoAbstractCNFCreator;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormatCNF;
import de.ovgu.featureide.fm.core.io.expression.ExpressionGroupFormat;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.sampling.algorithms.Chvatal;
import de.ovgu.featureide.sampling.algorithms.Dummy;
import de.ovgu.featureide.sampling.algorithms.FIDEChvatal;
import de.ovgu.featureide.sampling.algorithms.FIDEICPL;
import de.ovgu.featureide.sampling.algorithms.FIDERandom;
import de.ovgu.featureide.sampling.algorithms.ICPL;
import de.ovgu.featureide.sampling.algorithms.IncLing;
import de.ovgu.featureide.sampling.algorithms.PLEDGE_MAX;
import de.ovgu.featureide.sampling.algorithms.PLEDGE_MIN;
import de.ovgu.featureide.sampling.algorithms.YASA;
import de.ovgu.featureide.sampling.eval.Constants;
import de.ovgu.featureide.sampling.eval.Expressions;
import de.ovgu.featureide.sampling.eval.analyzer.PresenceConditionList;
import de.ovgu.featureide.sampling.eval.properties.AlgorithmProperty;
import de.ovgu.featureide.sampling.eval.properties.GroupingProperty;

public class TWiseSampler extends AAlgorithmBenchmark<SolutionList, Algorithm<SolutionList>> {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Configuration path and name not specified!");
			return;
		}
		final TWiseSampler evaluator = new TWiseSampler(args[0], args[1]);
		evaluator.init();
		evaluator.run();
		evaluator.dispose();
	}

	protected static final AlgorithmProperty algorithmsProperty = new AlgorithmProperty();
	protected static final StringListProperty tProperty = new StringListProperty("t");
	protected static final StringListProperty mProperty = new StringListProperty("m");
	protected static final GroupingProperty grouping = new GroupingProperty();
	protected static final IntProperty randomIterationsProperty = new IntProperty("random_iterations");

	public static int YASA_MIN_SIZE;
	public static int YASA_MAX_SIZE;
	public static long YASA_MIN_TIME;
	public static long YASA_MAX_TIME;

	protected Path samplesDir, curSampleDir;

	public TWiseSampler(String configPath, String configName) throws Exception {
		super(configPath, configName);
	}

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		extendCSVWriter(getModelCSVWriter(), Arrays.asList("Configurations", "FMFeatures", "FMConstraints", "FMPCs",
				"FMPCFeatures", "PCFeatures", "PCConstraints", "PCs"));
		extendCSVWriter(getDataCSVWriter(), Arrays.asList("Size"));
	}

	@Override
	protected List<Algorithm<SolutionList>> prepareAlgorithms() {
		ArrayList<Algorithm<SolutionList>> algorithms = new ArrayList<>();

		for (String algorithmName : algorithmsProperty.getValue()) {
			for (String tValueString : tProperty.getValue()) {
				final int tValue = Integer.parseInt(tValueString);
				final Path sampleFile = config.tempPath.resolve("sample.csv");
				final Path modelFile = config.tempPath.resolve("model.dimacs");
				switch (algorithmName) {
				case "DUMMY": {
					algorithms.add(new Dummy());
					break;
				}
				case "IC": {
					algorithms.add(new ICPL(tValue, sampleFile, modelFile));
					break;
				}
				case "CH": {
					algorithms.add(new Chvatal(tValue, sampleFile, modelFile));
					break;
				}
				case "FIC": {
					algorithms.add(new FIDEICPL(tValue, sampleFile, modelFile));
					break;
				}
				case "FCH": {
					algorithms.add(new FIDEChvatal(tValue, sampleFile, modelFile));
					break;
				}
				case "IL": {
					if (tValue == 2) {
						IncLing incLing = new IncLing(sampleFile, modelFile);
						incLing.setSeed(config.randomSeed.getValue());
						algorithms.add(incLing);
					}
					break;
				}
				case "PL": {
					algorithms.add(new PLEDGE_MIN(sampleFile, modelFile));
					algorithms.add(new PLEDGE_MAX(sampleFile, modelFile));
					break;
				}
				case "RND": {
					FIDERandom fideRandom = new FIDERandom(sampleFile, modelFile);
					fideRandom.setIterations(randomIterationsProperty.getValue());
					final String systemName = config.systemNames.get(systemID);
					switch (systemName) {
					case "axtls":
						fideRandom.setLimit(50);
						break;
					case "fiasco":
						fideRandom.setLimit(40);
						break;
					case "toybox":
						fideRandom.setLimit(50);
						break;
					case "uclibc-ng":
						fideRandom.setLimit(600);
						break;
					case "busybox-1_29_2":
						fideRandom.setLimit(90);
						break;
					case "linux_2_6_28_6":
						fideRandom.setLimit(800);
						break;
					default:
						throw new RuntimeException();
					}
					algorithms.add(fideRandom);
					break;
				}
				case "YA": {
					for (String groupingValue : grouping.getValue()) {
						final Path expressionFile = config.tempPath
								.resolve("expressions_" + groupingValue + ".expression");
						for (String mValue : mProperty.getValue()) {
							YASA yasa = new YASA(sampleFile, modelFile);
							yasa.setT(tValue);
							yasa.setM(Integer.parseInt(mValue));
							yasa.setExpressionFile(expressionFile);
							yasa.setGroupingValue(groupingValue);
							yasa.setSeed(config.randomSeed.getValue());
							algorithms.add(yasa);
						}
					}
					break;
				}
				}
			}
		}

		return algorithms;
	}

	@Override
	protected CNF prepareModel() throws Exception {
		final String systemName = config.systemNames.get(systemID);

		FeatureModelReader fmReader = new FeatureModelReader();
		fmReader.setPathToModels(config.modelPath);
		IFeatureModel fm = fmReader.read(systemName);
		if (fm == null) {
			throw new NullPointerException();
		}
		CNF modelCNF = new FeatureModelFormula(fm).getElement(new NoAbstractCNFCreator()).normalize();

		curSampleDir = samplesDir.resolve(String.valueOf(config.systemIDs.get(systemID)));
		Files.createDirectories(curSampleDir);
		final DIMACSFormatCNF format = new DIMACSFormatCNF();
		final Path fileName = curSampleDir.resolve("model." + format.getSuffix());
		FileHandler.save(fileName, modelCNF, format);

		return modelCNF;
	}

	@Override
	protected CNF adaptModel() {
		final CNF randomCNF = modelCNF.randomize(new Random(config.randomSeed.getValue() + systemIteration));
		final DIMACSFormatCNF format = new DIMACSFormatCNF();
		final Path fileName = config.tempPath.resolve("model" + "." + format.getSuffix());
		FileHandler.save(fileName, randomCNF, format);

		saveExpressions(modelCNF, randomCNF, GroupingProperty.FM_ONLY);
		saveExpressions(modelCNF, randomCNF, GroupingProperty.PC_ALL_FM);
		saveExpressions(modelCNF, randomCNF, GroupingProperty.PC_FOLDER_FM);
		saveExpressions(modelCNF, randomCNF, GroupingProperty.PC_FILE_FM);
		saveExpressions(modelCNF, randomCNF, GroupingProperty.PC_VARS_FM);

		YASA_MIN_SIZE = Integer.MAX_VALUE;
		YASA_MAX_SIZE = -1;
		YASA_MIN_TIME = Long.MAX_VALUE;
		YASA_MAX_TIME = -1;

		return randomCNF;
	}

	private void saveExpressions(final CNF cnf, final CNF randomCNF, String group) {
		List<List<ClauseList>> expressionGroups = adaptConditions(cnf, randomCNF,
				readExpressions(config.systemNames.get(systemID), group).getExpressions());
		randomizeConditions(expressionGroups, new Random(config.randomSeed.getValue() + systemIteration));

		final ExpressionGroupFormat format = new ExpressionGroupFormat();
		final Path fileName = config.tempPath.resolve("expressions_" + group + "." + format.getSuffix());
		FileHandler.save(fileName, expressionGroups, format);
	}

	@Override
	protected void adaptAlgorithm(Algorithm<SolutionList> algorithm) throws Exception {
		if (algorithm instanceof FIDERandom) {
			((FIDERandom) algorithm).setSeed(config.randomSeed.getValue() + algorithmIteration);
		} else if (algorithm instanceof PLEDGE_MIN) {
			((PLEDGE_MIN) algorithm).setTimeout(YASA_MIN_TIME);
		} else if (algorithm instanceof PLEDGE_MAX) {
			((PLEDGE_MAX) algorithm).setTimeout(YASA_MAX_TIME);
		}
	}

	@Override
	protected void writeModel(CSVWriter modelCSVWriter) {
		modelCSVWriter.addValue(config.systemIDs.get(systemID));
		modelCSVWriter.addValue(config.systemNames.get(systemID));

		final String systemName = config.systemNames.get(systemID);
		try {
			PresenceConditionList pcfmList = PresenceConditionList.readPCList(systemName,
					Constants.convertedPCFMFileName);
			CNF formula = pcfmList.getFormula();
			CountSolutionsAnalysis countAnalysis = new CountSolutionsAnalysis(formula);
			countAnalysis.setTimeout(0);
			modelCSVWriter.addValue(LongRunningWrapper.runMethod(countAnalysis));
			modelCSVWriter.addValue(formula.getVariables().size());
			modelCSVWriter.addValue(formula.getClauses().size());
			modelCSVWriter.addValue(pcfmList.size());
			modelCSVWriter.addValue(pcfmList.getPCNames().size());
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			modelCSVWriter.addValue(0);
			modelCSVWriter.addValue(-1);
			modelCSVWriter.addValue(-1);
			modelCSVWriter.addValue(-1);
			modelCSVWriter.addValue(-1);
		}

		try {
			PresenceConditionList pcList = PresenceConditionList.readPCList(systemName, Constants.convertedPCFileName);
			CNF formula = pcList.getFormula();
			modelCSVWriter.addValue(formula.getVariables().size());
			modelCSVWriter.addValue(formula.getClauses().size());
			modelCSVWriter.addValue(pcList.size());
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			modelCSVWriter.addValue(-1);
			modelCSVWriter.addValue(-1);
			modelCSVWriter.addValue(-1);
		}
	}

	public Expressions readExpressions(String name, String grouping) {
		try {
			return Expressions.readConditions(name, Constants.groupedPCFileName + grouping);
		} catch (IOException e) {
			return null;
		}
	}

	protected void writeData(CSVWriter dataCSVWriter) {
		super.writeData(dataCSVWriter);
		final SolutionList configurationList = result.getResult();
		if (configurationList == null) {
			dataCSVWriter.addValue(-1);
			return;
		}
		dataCSVWriter.addValue(configurationList.getSolutions().size());

		writeSamples(config.systemIDs.get(systemID) + "_" + systemIteration + "_" + algorithmIndex + "_"
				+ algorithmIteration, configurationList.getSolutions());

		if (Objects.equals("YASA", algorithmList.get(algorithmIndex).getName())) {
			if (YASA_MAX_SIZE < configurationList.getSolutions().size()) {
				YASA_MAX_SIZE = configurationList.getSolutions().size();
			}
			if (YASA_MIN_SIZE > configurationList.getSolutions().size()) {
				YASA_MIN_SIZE = configurationList.getSolutions().size();
			}
			if (YASA_MAX_TIME < result.getTime()) {
				YASA_MAX_TIME = result.getTime();
			}
			if (YASA_MIN_TIME > result.getTime()) {
				YASA_MIN_TIME = result.getTime();
			}
		}

		Logger.getInstance().logInfo("\t\tDone.", true);
	}

	@Override
	protected void setupDirectories() throws IOException {
		super.setupDirectories();

		samplesDir = config.outputPath.resolve("samples");
		Files.createDirectories(samplesDir);
	}

	protected List<List<ClauseList>> adaptConditions(CNF cnf, CNF randomCNF, List<List<ClauseList>> groupedConditions) {
		ArrayList<List<ClauseList>> adaptedGroupedConditions = new ArrayList<>();
		for (List<ClauseList> conditions : groupedConditions) {
			ArrayList<ClauseList> adaptedConditions = new ArrayList<>();
			for (ClauseList condition : conditions) {
				ClauseList adaptedClauseList = new ClauseList();
				for (LiteralSet clause : condition) {
					adaptedClauseList.add(clause.adapt(cnf.getVariables(), randomCNF.getVariables()));
				}
				adaptedConditions.add(adaptedClauseList);
			}
			adaptedGroupedConditions.add(adaptedConditions);
		}
		return adaptedGroupedConditions;
	}

	protected void randomizeConditions(List<List<ClauseList>> groupedConditions, Random random) {
		for (List<ClauseList> group : groupedConditions) {
			Collections.shuffle(group, random);
		}
		Collections.shuffle(groupedConditions, random);
	}

	protected void writeSamples(final String sampleMethod, final List<LiteralSet> configurationList) {
		try {
			Files.write(curSampleDir.resolve(sampleMethod + ".sample"), configurationList.stream()
					.map(this::reorderSolution).map(TWiseSampler::toString).collect(Collectors.toList()));
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

	private LiteralSet reorderSolution(LiteralSet solution) {
		LiteralSet adaptedSolution = solution.adapt(randomizedModelCNF.getVariables(), modelCNF.getVariables());
		adaptedSolution.setOrder(Order.INDEX);
		return adaptedSolution;
	}

	private static String toString(LiteralSet literalSet) {
		StringBuilder sb = new StringBuilder();
		for (int literal : literalSet.getLiterals()) {
			sb.append(literal);
			sb.append(',');
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

}
