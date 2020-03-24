package de.ovgu.featureide.sampling.algorithms;

import java.io.IOException;
import java.util.Random;

import de.ovgu.featureide.fm.benchmark.process.Algorithm;
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList;

public class Dummy extends Algorithm<SolutionList> {

	private long id = new Random().nextLong();

	@Override
	public String getName() {
		return "Dummy";
	}

	@Override
	public String getParameterSettings() {
		return Long.toString(id);
	}

	@Override
	public void postProcess() throws Exception {
	}

	@Override
	public SolutionList parseResults() throws IOException {
		return new SolutionList();
	}

	@Override
	protected void addCommandElements() throws Exception {
	}

}
