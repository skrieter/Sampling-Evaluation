package de.ovgu.featureide.sampling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.ovgu.featureide.fm.benchmark.util.FeatureModelReader;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;
import de.ovgu.featureide.fm.core.analysis.cnf.IVariables;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationGenerator;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormatCNF;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;

public class SimpleTWiseEvaluator extends ATWiseEvaluator {

	public static void main(String[] args) throws Exception {
		final SimpleTWiseEvaluator evaluator;
		if (args.length == 1) {
			evaluator = new SimpleTWiseEvaluator(args[0]);
		} else {
			evaluator = new SimpleTWiseEvaluator(null);
		}
		evaluator.run();
		evaluator.dispose();
	}

	public SimpleTWiseEvaluator(String configPath) throws Exception {
		super(configPath);
	}

	@Override
	protected void initCSVWriters() {
		extendModelCSVWriter(Arrays.asList("Configurations", "FMFeatures", "FMConstraints"));
		extendDataCSVWriter(Arrays.asList("Size", "Valid", "Complete"));
	}

	@Override
	protected void prepareModel(String systemName, int id, int systemIteration) throws Exception {
		FeatureModelReader fmReader = new FeatureModelReader();
		fmReader.setPathToModels(config.modelPath);
		IFeatureModel fm = fmReader.read(systemName);
		if (fm == null) {
			throw new NullPointerException();
		}
		final CNF cnf = new FeatureModelFormula(fm).getCNF();

		final IVariables newVariables = cnf.getVariables();
		List<List<ClauseList>> convertLiterals = TWiseConfigurationGenerator
				.convertLiterals(newVariables.getLiterals());

		Random random = new Random(config.randomSeed.getValue() + systemIteration);
		ArrayList<List<ClauseList>> adaptedGroupedConditions = new ArrayList<>();
		randomCNF = randomize(systemName, cnf, convertLiterals, random, systemIteration, adaptedGroupedConditions);

		FileHandler.save(config.tempPath.resolve("model.dimacs"), randomCNF, new DIMACSFormatCNF());

		if (systemIteration == 1) {
			getModelCSVWriter().createNewLine();
			getModelCSVWriter().addValue(id);
			getModelCSVWriter().addValue(systemName);
			getModelCSVWriter().addValue(-1);
			getModelCSVWriter().addValue(randomCNF.getVariables().size());
			getModelCSVWriter().addValue(randomCNF.getClauses().size());
			getModelCSVWriter().flush();
		}
	}

}
