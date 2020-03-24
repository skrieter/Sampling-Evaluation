package de.ovgu.featureide.sampling.algorithms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.ovgu.featureide.fm.benchmark.process.Algorithm;
import de.ovgu.featureide.fm.benchmark.util.Logger;
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList;
import de.ovgu.featureide.fm.core.io.ProblemList;
import de.ovgu.featureide.fm.core.io.csv.ConfigurationListFormat;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;

public abstract class AFIDESampling extends Algorithm<SolutionList> {

	private final Path outputFile;
	private final Path fmFile;

	protected Long seed;
	protected int limit;

	public AFIDESampling(Path outputFile, Path fmFile) {
		this.outputFile = outputFile;
		this.fmFile = fmFile;
	}

	@Override
	protected void addCommandElements() {
		addCommandElement("java");
		addCommandElement("-da");
		addCommandElement("-Xmx14g");
		addCommandElement("-Xms2g");
		addCommandElement("-cp");
		addCommandElement("tools/FIDE/*");
		addCommandElement("de.ovgu.featureide.fm.core.cli.FeatureIDECLI");
		addCommandElement("genconfig");
		addCommandElement("-o");
		addCommandElement(outputFile.toString());
		addCommandElement("-fm");
		addCommandElement(fmFile.toString());
	}

	@Override
	public void postProcess() {
		try {
			Files.deleteIfExists(outputFile);
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

	@Override
	public SolutionList parseResults() throws IOException {
		SolutionList configurationList = new SolutionList();
		ProblemList problems = FileHandler.load(outputFile, configurationList, new ConfigurationListFormat());
		if (problems.containsError()) {
			throw new IOException();
		}
		return configurationList;
	}

	public Long getSeed() {
		return seed;
	}

	public void setSeed(Long seed) {
		this.seed = seed;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}
