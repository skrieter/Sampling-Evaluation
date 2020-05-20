package de.ovgu.featureide.sampling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sk.utils.Logger;
import org.sk.utils.io.CSVWriter;

import de.ovgu.featureide.fm.benchmark.ABenchmark;
import de.ovgu.featureide.fm.benchmark.properties.StringListProperty;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet.Order;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.PresenceCondition;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.PresenceConditionManager;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationUtil;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.test.CoverageStatistic;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.test.TWiseStatisticGenerator;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.test.TWiseStatisticGenerator.ConfigurationScore;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.test.ValidityStatistic;
import de.ovgu.featureide.fm.core.analysis.cnf.solver.AdvancedSatSolver;
import de.ovgu.featureide.fm.core.io.ProblemList;
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormatCNF;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;
import de.ovgu.featureide.sampling.eval.Constants;
import de.ovgu.featureide.sampling.eval.Expressions;
import de.ovgu.featureide.sampling.eval.properties.GroupingProperty;

public class TWiseEvaluator extends ABenchmark {

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Configuration path and name not specified!");
			return;
		}
		final TWiseEvaluator evaluator = new TWiseEvaluator(args[0], args[1]);
		evaluator.init();
		evaluator.run();
		evaluator.dispose();
	}

	protected static final StringListProperty coverageT = new StringListProperty("t");
	protected static final GroupingProperty coverageGrouping = new GroupingProperty("grouping");

	protected CSVWriter evaluationWriter;

	private CNF modelCNF;
	private TWiseStatisticGenerator tWiseStatisticGenerator;

	private List<int[]> sampleArguments;
	private List<ValidityStatistic> sampleValidityStatistics;
	private List<CoverageStatistic> coverageStatistics;
	private String coverageCriterion;

	public TWiseEvaluator(String configPath, String configName) throws Exception {
		super(configPath, configName);
	}

	@Override
	protected void addCSVWriters() throws IOException {
		super.addCSVWriters();
		evaluationWriter = addCSVWriter("evaluation.csv", Arrays.asList("ModelID", "AlgorithmID", "SystemIteration",
				"AlgorithmIteration", "SamplePercentage", "Criterion", "Value"));
	}

	protected final HashMap<String, PresenceConditionManager> expressionMap = new HashMap<>();

	@Override
	public void run() {
		super.run();

		if (config.systemIterations.getValue() > 0) {
			Logger.getInstance().logInfo("Start", 0);

			Path samplesDir = config.outputPath.resolve("samples");
			List<Path> dirList;
			try (Stream<Path> fileStream = Files.list(samplesDir)) {
				dirList = fileStream.filter(Files::isReadable).filter(Files::isDirectory).collect(Collectors.toList());
			} catch (IOException e) {
				Logger.getInstance().logError(e);
				return;
			}
			Collections.sort(dirList, (p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()));

			dirList.forEach(this::readSamples);
			Logger.getInstance().logInfo("Finished", 0);
		} else {
			Logger.getInstance().logInfo("Nothing to do", 0);
		}
	}

	private void readSamples(Path sampleDir) {
		try {
			systemIndex = config.systemIDs.indexOf(Integer.parseInt(sampleDir.getFileName().toString()));
		} catch (Exception e) {
			Logger.getInstance().logError(e);
			return;
		}

		Logger.getInstance().logInfo("System " + (systemIndex + 1), 0);
		Logger.getInstance().incTabLevel();
		Logger.getInstance().logInfo("Preparing...", 0);

		final DIMACSFormatCNF format = new DIMACSFormatCNF();
		Path modelFile = sampleDir.resolve("model." + format.getSuffix());
		modelCNF = new CNF();
		ProblemList problems = FileHandler.load(modelFile, modelCNF, format);
		if (problems.containsError()) {
			Logger.getInstance().logError(problems.getErrors().get(0).error);
			return;
		}

		final TWiseConfigurationUtil util;
		if (!modelCNF.getClauses().isEmpty()) {
			util = new TWiseConfigurationUtil(modelCNF, new AdvancedSatSolver(modelCNF));
		} else {
			util = new TWiseConfigurationUtil(modelCNF, null);
		}

		util.computeRandomSample();
		if (!modelCNF.getClauses().isEmpty()) {
			util.computeMIG();
		}
		tWiseStatisticGenerator = new TWiseStatisticGenerator(util);

		List<Path> sampleFileList;
		try (Stream<Path> fileStream = Files.list(sampleDir)) {
			sampleFileList = fileStream.filter(Files::isReadable).filter(Files::isRegularFile)
					.filter(file -> file.getFileName().toString().endsWith(".sample")).collect(Collectors.toList());
		} catch (IOException e) {
			Logger.getInstance().logError(e);
			return;
		}
		Collections.sort(sampleFileList,
				(p1, p2) -> p1.getFileName().toString().compareTo(p2.getFileName().toString()));

		Logger.getInstance().logInfo("Reading Samples...", 0);
		List<List<? extends LiteralSet>> samples = new ArrayList<>(sampleFileList.size());
		sampleArguments = new ArrayList<>(sampleFileList.size());
		for (Path sampleFile : sampleFileList) {

			final List<LiteralSet> sample;
			int[] argumentValues;
			try {
				final String fileName = sampleFile.getFileName().toString();
				final String[] arguments = fileName.substring(0, fileName.length() - ".sample".length()).split("_");
				sample = Files.lines(sampleFile).map(this::parseConfiguration).collect(Collectors.toList());

				argumentValues = new int[4];
				argumentValues[0] = Integer.parseInt(arguments[1]);
				argumentValues[1] = Integer.parseInt(arguments[2]);
				argumentValues[2] = Integer.parseInt(arguments[3]);
				argumentValues[3] = 100;

			} catch (Exception e) {
				Logger.getInstance().logError(e);
				continue;
			}
			// if Random
			if (argumentValues[1] == 8) {
				for (int p = 5; p <= 100; p += 5) {
					samples.add(sample.subList(0, (sample.size() * p) / 100));
					int[] argumentValues2 = new int[4];
					argumentValues2[0] = argumentValues[0];
					argumentValues2[1] = argumentValues[1];
					argumentValues2[2] = argumentValues[2];
					argumentValues2[3] = p;
					sampleArguments.add(argumentValues2);
				}
			} else {
				samples.add(sample);
				sampleArguments.add(argumentValues);
			}
		}

		Logger.getInstance().logInfo("Testing Validity...", 0);
		sampleValidityStatistics = tWiseStatisticGenerator.getValidity(samples);
		for (int i = 0; i < sampleArguments.size(); i++) {
			final int i2 = i;
			writeCSV(evaluationWriter, writer -> writeValidity(writer, i2));
		}

		final int tSize = coverageT.getValue().size();
		final int gSize = coverageGrouping.getValue().size();
		int gIndex = 0;
		Logger.getInstance().incTabLevel();
		for (String groupingValue : coverageGrouping.getValue()) {
			gIndex++;
			List<List<PresenceCondition>> nodes = readExpressions(groupingValue, util).getGroupedPresenceConditions();
			int tIndex = 0;
			for (String tValue : coverageT.getValue()) {
				tIndex++;
				logCoverage(tSize, gSize, tIndex, gIndex);

				coverageCriterion = groupingValue + "_t" + tValue;
				coverageStatistics = tWiseStatisticGenerator.getCoverage(samples, nodes, Integer.parseInt(tValue),
						ConfigurationScore.NONE, true);
				for (int i = 0; i < sampleArguments.size(); i++) {
					final int i2 = i;
					writeCSV(evaluationWriter, writer -> writeCoverage(writer, i2));
				}

			}
		}
		Logger.getInstance().decTabLevel();
		Logger.getInstance().decTabLevel();
	}

	private PresenceConditionManager readExpressions(String group, TWiseConfigurationUtil util) {
		try {
			Expressions exp = Expressions.readConditions(config.systemNames.get(systemIndex),
					Constants.groupedPCFileName + group);
			return new PresenceConditionManager(util, exp.getExpressions());
		} catch (IOException e) {
			return null;
		}
	}

	private LiteralSet parseConfiguration(String configuration) {
		String[] literalStrings = configuration.split(",");
		int[] literals = new int[literalStrings.length];
		for (int i = 0; i < literalStrings.length; i++) {
			literals[i] = Integer.parseInt(literalStrings[i]);
		}
		LiteralSet solution = new LiteralSet(literals, Order.INDEX, false);
		return solution;
	}

	private void writeValidity(CSVWriter csvWriter, int i) {
		int[] argumentValues = sampleArguments.get(i);
		ValidityStatistic validityStatistic = sampleValidityStatistics.get(i);
		csvWriter.addValue(config.systemIDs.get(systemIndex));
		csvWriter.addValue(argumentValues[1]);
		csvWriter.addValue(argumentValues[0]);
		csvWriter.addValue(argumentValues[2]);
		csvWriter.addValue(argumentValues[3]);
		csvWriter.addValue("validity");
		csvWriter.addValue(validityStatistic.getValidInvalidRatio());
	}

	private void writeCoverage(CSVWriter csvWriter, int i) {
		int[] argumentValues = sampleArguments.get(i);
		CoverageStatistic coverageStatistic = coverageStatistics.get(i);
		csvWriter.addValue(config.systemIDs.get(systemIndex));
		csvWriter.addValue(argumentValues[1]);
		csvWriter.addValue(argumentValues[0]);
		csvWriter.addValue(argumentValues[2]);
		csvWriter.addValue(argumentValues[3]);
		csvWriter.addValue(coverageCriterion);
		csvWriter.addValue(coverageStatistic.getCoverage());
	}

	private void logCoverage(final int tSize, final int gSize, int tIndex, int gIndex) {
		StringBuilder sb = new StringBuilder();
		sb.append("t: ");
		sb.append(tIndex);
		sb.append("/");
		sb.append(tSize);
		sb.append(" | g: ");
		sb.append(gIndex);
		sb.append("/");
		sb.append(gSize);
		Logger.getInstance().logInfo(sb.toString(), 0);
	}

}
