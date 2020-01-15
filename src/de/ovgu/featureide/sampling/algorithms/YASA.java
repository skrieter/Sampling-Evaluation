package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;
import java.util.List;

public class YASA extends AFIDESampling {

	private final int m;

	public YASA(int t, Path outputFile, Path fmFile, int m) {
		super(t, outputFile, fmFile);
		this.m = m;
	}

	@Override
	protected void addAddtionalParameters(List<String> parameters) {
		parameters.add("-a");
		parameters.add("YASA");
		parameters.add("-m");
		parameters.add(Integer.toString(m));
	}

	@Override
	public String getName() {
		return "YASA";
	}

	@Override
	public String getParameterSettings() {
		return "t" + t + "_m" + m;
	}

}
