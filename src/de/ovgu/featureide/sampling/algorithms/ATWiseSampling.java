package de.ovgu.featureide.sampling.algorithms;

import java.util.ArrayList;
import java.util.List;

import de.ovgu.featureide.fm.benchmark.process.Algorithm;
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList;

public abstract class ATWiseSampling extends Algorithm<SolutionList> {

	protected final int t;

	protected final ArrayList<String> parameters = new ArrayList<>();

	public ATWiseSampling(int t) {
		this.t = t;
	}

	@Override
	public List<String> getCommand() {
		return parameters;
	}

	public int getT() {
		return t;
	}

}
