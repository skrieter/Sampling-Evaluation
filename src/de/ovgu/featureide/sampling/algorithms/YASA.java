package de.ovgu.featureide.sampling.algorithms;

import java.nio.file.Path;

public class YASA extends AFIDESampling {

	private Path expressionFile;
	private String groupingValue;

	private int t;
	private int m;

	public YASA(Path outputFile, Path fmFile) {
		super(outputFile, fmFile);
	}

	@Override
	protected void addCommandElements() {
		super.addCommandElements();
		addCommandElement("-a");
		addCommandElement("YASA");
		addCommandElement("-t");
		addCommandElement(Integer.toString(t));
		addCommandElement("-m");
		addCommandElement(Integer.toString(m));
		if (expressionFile != null) {
			addCommandElement("-e");
			addCommandElement(expressionFile.toString());
		}
		if (seed != null) {
			addCommandElement("-s");
			addCommandElement(seed.toString());
		}
	}

	@Override
	public String getName() {
		return "YASA";
	}

	@Override
	public String getParameterSettings() {
		return "t" + t + "_m" + m + "_" + groupingValue;
	}

	public Path getExpressionFile() {
		return expressionFile;
	}

	public void setExpressionFile(Path expressionFile) {
		this.expressionFile = expressionFile;
	}

	public String getGroupingValue() {
		return groupingValue;
	}

	public void setGroupingValue(String groupingValue) {
		this.groupingValue = groupingValue;
	}

	public int getT() {
		return t;
	}

	public void setT(int t) {
		this.t = t;
	}

	public int getM() {
		return m;
	}

	public void setM(int m) {
		this.m = m;
	}

}
