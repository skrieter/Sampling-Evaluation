package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;
import java.util.List;

public class IncLing extends AFIDESampling {

	public IncLing(int t, Path outputFile, Path fmFile) {
		super(t, outputFile, fmFile);
	}

	@Override
	protected void addAddtionalParameters(List<String> parameters) {
		parameters.add("-a");
		parameters.add("Incling");
	}

	@Override
	public String getName() {
		return "Incling";
	}

}
