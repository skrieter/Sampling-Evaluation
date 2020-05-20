package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class IncLing extends AFIDESampling {

	public IncLing(Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("Incling");
		addCommandElement("-t");
		addCommandElement(Integer.toString(2));
		if (seed != null) {
			addCommandElement("-s");
			addCommandElement(seed.toString());
		}
	}

	@Override
	public String getName() {
		return "Incling";
	}

	@Override
	public String getParameterSettings() {
		return "t2";
	}

}
