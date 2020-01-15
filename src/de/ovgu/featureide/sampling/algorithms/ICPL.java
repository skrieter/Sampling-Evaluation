package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;
import java.util.List;

public class ICPL extends ASPLCATSampling {

	public ICPL(int t, Path outputFile, Path fmFile) {
		super(t, outputFile, fmFile);
	}

	@Override
	protected void addAddtionalParameters(List<String> parameters) {
		parameters.add("-a");
		parameters.add("ICPL");
	}

	@Override
	public String getName() {
		return "ICPL";
	}

}
