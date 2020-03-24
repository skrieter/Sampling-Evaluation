package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class FIDEICPL extends AFIDESampling {

	private final int t;
	
	public FIDEICPL(int t, Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
		this.t = t;
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("ICPL");
		addCommandElement("-t");
		addCommandElement(Integer.toString(t));
	}

	@Override
	public String getName() {
		return "FIDE-ICPL";
	}

	@Override
	public String getParameterSettings() {
		return "t" + t;
	}

}
