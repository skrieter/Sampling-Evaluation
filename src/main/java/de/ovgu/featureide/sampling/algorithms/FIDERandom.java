package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class FIDERandom extends AFIDESampling {

	public FIDERandom(Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
	}

	@Override
	public void preProcess() throws Exception {
		super.preProcess();
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("Random");
		addCommandElement("-l");
		addCommandElement(Integer.toString(limit));
		if (seed != null) {
			addCommandElement("-s");
			addCommandElement(seed.toString());
		}
	}

	@Override
	public String getName() {
		return "FIDE-Random";
	}

	@Override
	public String getParameterSettings() {
		return "";
	}

}
