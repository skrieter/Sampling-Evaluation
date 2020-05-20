package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class FIDEChvatal extends AFIDESampling {

	private final int t;
	
	public FIDEChvatal(int t, Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
		this.t = t;
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("Chvatal");
		addCommandElement("-t");
		addCommandElement(Integer.toString(t));
	}

	@Override
	public String getName() {
		return "FIDE-Chvatal";
	}

	@Override
	public String getParameterSettings() {
		return "t" + t;
	}

}
