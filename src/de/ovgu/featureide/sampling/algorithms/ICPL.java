package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class ICPL extends ASPLCATSampling {

	public ICPL(int t, Path outputFile, Path fmFile) {
		super(t, outputFile, fmFile);
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("ICPL");
	}

	@Override
	public String getName() {
		return "ICPL";
	}

}
