package de.ovgu.featureide.sampling.algorithms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList;
import de.ovgu.featureide.fm.core.io.csv.ConfigurationListFormat;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;

public abstract class AFIDESampling extends ATWiseSampling {

	private final Path outputFile;
	private final Path fmFile;

	public AFIDESampling(int t, Path outputFile, Path fmFile) {
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
		parameters.add("de.ovgu.featureide.fm.core.cli.FeatureIDECLI");
		parameters.add("genconfig");
		parameters.add("-t");
		parameters.add(Integer.toString(t));
		parameters.add("-o");
		parameters.add(outputFile.toString());
		parameters.add("-fm");
		parameters.add(fmFile.toString());
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

	protected void addAddtionalParameters(List<String> parameters) {}

	@Override
	public SolutionList parseResults() {
		SolutionList configurationList = new SolutionList();
		FileHandler.load(outputFile, configurationList, new ConfigurationListFormat());
		return configurationList;
	}

	@Override
	public String getParameterSettings() {
		return "t" + t;
	}

}
