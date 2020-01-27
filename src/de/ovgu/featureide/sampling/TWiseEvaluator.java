package de.ovgu.featureide.sampling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.ovgu.featureide.fm.benchmark.ABenchmark;
import de.ovgu.featureide.fm.benchmark.properties.StringListProperty;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet.Order;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationStatistic;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationTester;
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

	protected CNF modelCNF;
	protected int algorithmIndex;
	protected List<LiteralSet> sample;

	public TWiseEvaluator(String configPath, String configName) throws Exception {
		super(configPath, configName);
	}

	@Override
	protected void addCSVWriters() {
		super.addCSVWriters();
		ArrayList<String> dataHeader = new ArrayList<>(Arrays.asList("ModelID", "AlgorithmID", "SystemIteration"));
		for (String tValue : coverageT.getValue()) {
			for (String groupingValue : coverageGrouping.getValue()) {
				dataHeader.add("coverage_" + groupingValue + "_t" + tValue);
			}
		}
		evaluationWriter = addCSVWriter("evaluation.csv", dataHeader);
	}

	protected final HashMap<String, List<List<ClauseList>>> expressionMap = new HashMap<>();

	@Override
	public void run() {
		super.run();

		if (config.systemIterations.getValue() > 0) {
			Logger.getInstance().logInfo("Start", false);

			Path samplesDir = config.outputPath.resolve("samples");
			List<Path> dirList;
			try (Stream<Path> fileStream = Files.list(samplesDir)) {
				dirList = fileStream.filter(Files::isReadable).filter(Files::isDirectory).collect(Collectors.toList());
			} catch (IOException e) {
				Logger.getInstance().logError(e);
				return;
			}

			dirList.forEach(this::readSamples);
			Logger.getInstance().logInfo("Finished", false);
		} else {
			Logger.getInstance().logInfo("Nothing to do", false);
		}
	}

	private void readSamples(Path sampleDir) {
		try {
			systemIndex = Integer.parseInt(sampleDir.getFileName().toString());
		} catch (Exception e) {
			Logger.getInstance().logError(e);
			return;
		}

		final DIMACSFormatCNF format = new DIMACSFormatCNF();
		Path modelFile = sampleDir.resolve("model." + format.getSuffix());
		modelCNF = new CNF();
		ProblemList problems = FileHandler.load(modelFile, modelCNF, format);
		if (problems.containsError()) {
			Logger.getInstance().logError(problems.getErrors().get(0).error);
			return;
		}

		List<Path> sampleFileList;
		try (Stream<Path> fileStream = Files.list(sampleDir)) {
			sampleFileList = fileStream.filter(Files::isReadable).filter(Files::isRegularFile)
					.filter(file -> file.getFileName().toString().endsWith(".sample")).collect(Collectors.toList());
		} catch (IOException e) {
			Logger.getInstance().logError(e);
			return;
		}

		expressionMap.clear();
		readExpressions(GroupingProperty.FM_ONLY);
		readExpressions(GroupingProperty.PC_ALL_FM);
		readExpressions(GroupingProperty.PC_FOLDER_FM);
		readExpressions(GroupingProperty.PC_FILE_FM);
		readExpressions(GroupingProperty.PC_VARS_FM);

		for (Path sampleFile : sampleFileList) {
			String fileName = sampleFile.getFileName().toString();
			String argumentString = fileName.substring(0, fileName.length() - ".sample".length());
			String[] arguments = argumentString.split("_");
			systemIteration = Integer.parseInt(arguments[1]);
			algorithmIndex = Integer.parseInt(arguments[2]);
			logRun();

			try {
				sample = Files.lines(sampleFile).map(this::parseConfiguration).collect(Collectors.toList());
			} catch (IOException e) {
				Logger.getInstance().logError(e);
				continue;
			}

			writeCSV(evaluationWriter, this::writeData);
		}
	}

	private Expressions readExpressions(String group) {
		try {
			Expressions exp = Expressions.readConditions(config.systemNames.get(systemIndex),
					Constants.groupedPCFileName + group);
			expressionMap.put(group, exp.getExpressions());
			return exp;
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

	private void writeData(CSVWriter evaluationWriter) {
		evaluationWriter.addValue(systemIndex);
		evaluationWriter.addValue(systemIteration);
		evaluationWriter.addValue(algorithmIndex);

		TWiseConfigurationTester tester = new TWiseConfigurationTester(modelCNF);
		tester.setSample(sample);

		writeValidity(tester, evaluationWriter);

		Logger.getInstance().logInfo("\tCalculating configuration coverage...", true);

		final int tSize = coverageT.getValue().size();
		final int gSize = coverageGrouping.getValue().size();
		int tIndex = 0;
		for (String tValue : coverageT.getValue()) {
			tIndex++;
			int gIndex = 0;
			tester.setT(Integer.parseInt(tValue));
			for (String groupingValue : coverageGrouping.getValue()) {
				gIndex++;
				logCoverage(tSize, gSize, tIndex, gIndex);
				tester.setNodes(expressionMap.get(groupingValue));
				writeCoverage(tester, evaluationWriter);
			}
		}
		Logger.getInstance().logInfo("\t\tDone.", true);
	}

	protected boolean writeValidity(TWiseConfigurationTester tester, CSVWriter csvWriter) {
		Logger.getInstance().logInfo("\tTesting configuration validity...", true);
		LiteralSet invalidSolution = tester.getFirstInvalidSolution();
		boolean validity = invalidSolution == null;
		if (validity) {
			Logger.getInstance().logInfo("\t\tPASS", true);
		} else {
			Logger.getInstance().logInfo("\t\tFAIL", true);
			Logger.getInstance().logInfo("\t\tInvalid configuration: " + invalidSolution, true);
		}
		csvWriter.addValue(validity);
		return validity;
	}

	protected double writeCoverage(TWiseConfigurationTester tester, CSVWriter csvWriter) {
		TWiseConfigurationStatistic statistics = tester.getCoverage();
		long numberOfCoveredConditions = statistics.getNumberOfCoveredConditions();
		long numberOfValidConditions = statistics.getNumberOfValidConditions();
		double coverage = numberOfValidConditions == 0 ? 1
				: (double) numberOfCoveredConditions / (double) numberOfValidConditions;

		csvWriter.addValue(coverage);
		return coverage;
	}

	private void logRun() {
		StringBuilder sb = new StringBuilder();
		sb.append("System: ");
		sb.append(systemIndex + 1);
		sb.append(" | Iteration: ");
		sb.append(systemIteration);
		sb.append(" | Algorithm: ");
		sb.append(algorithmIndex + 1);
		Logger.getInstance().logInfo(sb.toString(), 2, false);
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
		Logger.getInstance().logInfo(sb.toString(), 3, false);
	}

}
