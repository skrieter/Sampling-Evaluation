package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class PLEDGE_MIN extends PLEDGE {

	public PLEDGE_MIN(Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
	}

	@Override
	public String getName() {
		return "Pledge-Min";
	}

}
