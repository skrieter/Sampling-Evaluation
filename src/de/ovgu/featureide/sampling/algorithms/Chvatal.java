package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;
import java.util.List;

public class Chvatal extends ASPLCATSampling {

	public Chvatal(int t, Path outputFile, Path fmFile) {
		super(t, outputFile, fmFile);
	}

	@Override
	protected void addAddtionalParameters(List<String> parameters) {
		parameters.add("-a");
		parameters.add("Chvatal");
	}

	@Override
	public String getName() {
		return "Chvatal";
	}

}
