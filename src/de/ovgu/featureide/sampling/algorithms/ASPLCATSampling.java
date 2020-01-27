package de.ovgu.featureide.sampling.algorithms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet.Order;
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList;
import de.ovgu.featureide.fm.core.analysis.cnf.Variables;

public abstract class ASPLCATSampling extends ATWiseSampling {

	private final Path outputFile;
	private final Path fmFile;

	public ASPLCATSampling(int t, Path outputFile, Path fmFile) {
		super(t);
		this.outputFile = outputFile;
		this.fmFile = fmFile;
	}

	@Override
	public void preProcess() {
		parameters.clear();
		parameters.add("java");
		parameters.add("-da");
		parameters.add("-Xmx12g");
		parameters.add("-Xms2g");
		parameters.add("-cp");
		parameters.add("build/jar/lib/*");
		parameters.add("no.sintef.ict.splcatool.SPLCATool");
		parameters.add("-t");
		parameters.add("t_wise");
		parameters.add("-fm");
		parameters.add(fmFile.toString());
		parameters.add("-s");
		parameters.add(Integer.toString(t));
		parameters.add("-o");
		parameters.add(outputFile.toString());
		addAddtionalParameters(parameters);
	}

	@Override
	public void postProcess() {
		try {
			Files.deleteIfExists(outputFile);
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

	protected void addAddtionalParameters(List<String> parameters) {
	}

	@Override
	public SolutionList parseResults() throws Exception {
		if (!Files.isReadable(outputFile)) {
			return null;
		}

		List<String> lines = Files.readAllLines(outputFile);
		if (lines.isEmpty()) {
			return null;
		}

		int numberOfConfigurations = 0;
		String header = lines.get(0);
		int length = header.length();
		if (length > 1) {
			int lastSeparatorIndex = header.lastIndexOf(';', length - 2);
			if (lastSeparatorIndex > -1) {
				String lastColumn = header.substring(lastSeparatorIndex + 1, length - 1);
				numberOfConfigurations = Integer.parseInt(lastColumn) + 1;
			}
		}

		final List<String> featureLines = lines.subList(1, lines.size());
		final int numberOfFeatures = featureLines.size();
		final ArrayList<String> featureNames = new ArrayList<>(numberOfFeatures);
		for (String line : featureLines) {
			final String featureName = line.substring(0, line.indexOf(";"));
			featureNames.add(featureName);
		}
		final Variables variables = new Variables(featureNames);

		List<int[]> configurationList = new ArrayList<>(numberOfConfigurations);
		for (int i = 0; i < numberOfConfigurations; i++) {
			configurationList.add(new int[variables.size()]);
		}

		for (String line : featureLines) {
			final String[] columns = line.split(";");
			final int variable = variables.getVariable(columns[0]);
			final int variableIndex = variable - 1;
			int columnIndex = 1;
			for (int[] configuration : configurationList) {
				configuration[variableIndex] = "X".equals(columns[columnIndex++]) ? variable : -variable;
			}
		}

		final ArrayList<LiteralSet> configurationList2 = new ArrayList<>(numberOfConfigurations);
		for (int[] configuration : configurationList) {
			configurationList2.add(new LiteralSet(configuration, Order.INDEX));
		}
		return new SolutionList(variables, configurationList2);
	}

	@Override
	public String getParameterSettings() {
		return "t" + t;
	}

}
