package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class PLEDGE_MAX extends PLEDGE {

	public PLEDGE_MAX(Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
	}

	@Override
	public String getName() {
		return "Pledge-Max";
	}

}
