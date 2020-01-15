package de.ovgu.featureide.sampling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import de.ovgu.featureide.fm.benchmark.ABenchmark;
import de.ovgu.featureide.fm.benchmark.properties.StringListProperty;
import de.ovgu.featureide.fm.benchmark.util.CSVWriter;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationGenerator;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationStatistic;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationTester;
import de.ovgu.featureide.sampling.algorithms.ATWiseSampling;
import de.ovgu.featureide.sampling.algorithms.Chvatal;
import de.ovgu.featureide.sampling.algorithms.ICPL;
import de.ovgu.featureide.sampling.algorithms.IncLing;
import de.ovgu.featureide.sampling.algorithms.YASA;
import de.ovgu.featureide.sampling.eval.properties.AlgorithmProperty;

public abstract class ATWiseEvaluator extends ABenchmark<ATWiseSampling> {

	protected static final AlgorithmProperty algorithm = new AlgorithmProperty();
	protected static final StringListProperty t = new StringListProperty("t");
	protected static final StringListProperty m = new StringListProperty("m");

	protected Path samplesDir;

	protected CNF randomCNF;

	public ATWiseEvaluator(String configPath) throws Exception {
		super(configPath);
	}

	@Override
	public void init() throws Exception {
		super.init();

		samplesDir = config.outputPath.resolve("samples");
		Files.createDirectories(samplesDir);
	}

	@Override
	protected List<ATWiseSampling> prepareAlgorithms(int systemIndex) {
		ArrayList<ATWiseSampling> algorithms = new ArrayList<>();

		int algorithmIndex = 0;
		for (String algorithmName : algorithm.getValue()) {
			for (String tValueString : t.getValue()) {
				final int tValue = Integer.parseInt(tValueString);
				final Path sampleFile = config.tempPath.resolve("sample.csv");
				final Path modelFile = config.tempPath.resolve("model.dimacs");
				switch (algorithmName) {
				case "IC": {
					addAlgorithm(new ICPL(tValue, sampleFile, modelFile), algorithmIndex++, algorithms, systemIndex);
					break;
				}
				case "CH": {
					addAlgorithm(new Chvatal(tValue, sampleFile, modelFile), algorithmIndex++, algorithms, systemIndex);
					break;
				}
				case "IL": {
					if (tValue == 2) {
						addAlgorithm(new IncLing(tValue, sampleFile, modelFile), algorithmIndex++, algorithms,
								systemIndex);
					}
					break;
				}
				case "YA": {
					for (String mValue : m.getValue()) {
						addAlgorithm(new YASA(tValue, sampleFile, modelFile, Integer.parseInt(mValue)),
								algorithmIndex++, algorithms, systemIndex);
					}
					break;
				}
				}
			}
		}

		return algorithms;
	}

	private void addAlgorithm(final ATWiseSampling algorithm, int algorithmIndex, List<ATWiseSampling> algorithms,
			int systemIndex) {
		algorithms.add(algorithm);
		if (systemIndex == 1) {
			getAlgorithmCSVWriter().createNewLine();
			getAlgorithmCSVWriter().addValue(algorithmIndex);
			getAlgorithmCSVWriter().addValue(algorithm.getName());
			getAlgorithmCSVWriter().addValue(algorithm.getParameterSettings());
			getAlgorithmCSVWriter().flush();
		}
	}

	protected CNF randomize(String name, final CNF cnf, final List<List<ClauseList>> groupedConditions, Random random,
			int systemIteration, ArrayList<List<ClauseList>> adaptedGroupedConditions) {
		final CNF randomCNF = cnf.randomize(random);
		for (List<ClauseList> conditions : groupedConditions) {
			ArrayList<ClauseList> adaptedConditions = new ArrayList<>();
			for (ClauseList condition : conditions) {
				ClauseList adaptedClauseList = new ClauseList();
				for (LiteralSet clause : condition) {
					adaptedClauseList.add(clause.adapt(cnf.getVariables(), randomCNF.getVariables()));
				}
				adaptedConditions.add(adaptedClauseList);
			}
			Collections.shuffle(adaptedConditions, random);
			adaptedGroupedConditions.add(adaptedConditions);
		}
		Collections.shuffle(adaptedGroupedConditions, random);
		writeFeatureNames(randomCNF, name + "_" + systemIteration);
		return randomCNF;
	}

	protected void writeData(String systemName, ATWiseSampling algorithm, int systemIteration, int algorithmIteration,
			CSVWriter dataCSVWriter) {
		final SolutionList configurationList = algorithm.getResult();
		if (configurationList == null) {
			dataCSVWriter.addValue(-1);
			dataCSVWriter.addValue(false);
			dataCSVWriter.addValue(false);
			return;
		}

		dataCSVWriter.addValue(configurationList.getSolutions().size());

		final ArrayList<LiteralSet> newConfigurationList = new ArrayList<>();
		for (LiteralSet configuration : configurationList.getSolutions()) {
			newConfigurationList.add(configuration.adapt(configurationList.getVariables(), randomCNF.getVariables()));
		}
		writeValidity(randomCNF, TWiseConfigurationGenerator.convertLiterals(randomCNF.getVariables().getLiterals()),
				algorithm.getT(), newConfigurationList);

		if (algorithmIteration == 1) {
			writeSamples(systemName + "_" + systemIteration + "_" + algorithm.getFullName(),
					configurationList.getSolutions());
		}
	}

	private boolean writeValidity(CNF cnf, List<List<ClauseList>> nodes, final int tValue,
			final List<LiteralSet> configurationList) throws AssertionError {
		Logger.getInstance().logInfo("Testing results...", true);
		TWiseConfigurationTester tester = new TWiseConfigurationTester(cnf, tValue, nodes, configurationList);

		Logger.getInstance().logInfo("\tTesting configuration validity...", true);
		LiteralSet invalidSolution = tester.getFirstInvalidSolution();
		boolean validity = invalidSolution == null;
		if (validity) {
			Logger.getInstance().logInfo("\t\tPASS", true);
		} else {
			Logger.getInstance().logInfo("\t\tFAIL", true);
			Logger.getInstance().logInfo("\t\tInvalid configuration: " + invalidSolution, true);
		}

		Logger.getInstance().logInfo("\tTesting combination completeness...", true);
		ClauseList uncoveredCombination = tester.getFirstUncoveredCondition();
		boolean completeness = uncoveredCombination == null;
		if (completeness) {
			Logger.getInstance().logInfo("\t\tPASS", true);
		} else {
			Logger.getInstance().logInfo("\t\tFAIL", true);
			Logger.getInstance().logInfo("\t\tUncovered combination: " + uncoveredCombination, true);
		}

		Logger.getInstance().logInfo("\tCalculating coverage...", true);
		TWiseConfigurationStatistic statistic = tester.getCoverage();
		double coverage = (double) statistic.getNumberOfCoveredConditions()
				/ (double) statistic.getNumberOfValidConditions();
		double coveragePercentage = Math.floor(coverage * 1000.0) / 10.0;
		Logger.getInstance().logInfo("\t\tCoverage: " + coveragePercentage + "%", true);

		getDataCSVWriter().addValue(validity);
		getDataCSVWriter().addValue(completeness);
		return completeness && validity;
	}

	private void writeSamples(final String sampleMethod, final List<LiteralSet> configurationList) {
		try {
			Files.write(samplesDir.resolve(sampleMethod + ".sample"),
					configurationList.stream().map(ATWiseEvaluator::toString).collect(Collectors.toList()));
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

	private static String toString(LiteralSet literalSet) {
		return Arrays.toString(literalSet.getLiterals());
	}

	private void writeFeatureNames(CNF cnf, final String name) {
		try {
			final Path featureNamesFile = samplesDir.resolve(name + ".features");
			if (!Files.exists(featureNamesFile)) {
				List<String> featureNames = Arrays.asList(cnf.getVariables().getNames());
				Files.write(featureNamesFile, featureNames.subList(1, featureNames.size()));
			}
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

}
