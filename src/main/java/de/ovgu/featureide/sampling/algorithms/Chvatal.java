package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class Chvatal extends ASPLCATSampling {

	public Chvatal(int t, Path outputFile, Path fmFile) {
		super(t, outputFile, fmFile);
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("Chvatal");
	}

	@Override
	public String getName() {
		return "Chvatal";
	}

}
