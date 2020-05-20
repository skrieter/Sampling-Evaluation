package de.ovgu.featureide.sampling.algorithms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sk.utils.Logger;

import de.ovgu.featureide.fm.benchmark.process.Algorithm;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet.Order;
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList;
import de.ovgu.featureide.fm.core.analysis.cnf.Variables;
import de.ovgu.featureide.sampling.TWiseSampler;

public abstract class PLEDGE extends Algorithm<SolutionList> {

	private final Path outputFile;
	private final Path fmFile;

	private long numberOfConfigurations = 10;
	private long timeout = 1000;

	public PLEDGE(Path outputFile, Path fmFile) {
		this.outputFile = outputFile;
		this.fmFile = fmFile;
	}

	public long getNumberOfConfigurations() {
		return numberOfConfigurations;
	}

	public void setNumberOfConfigurations(long numberOfConfigurations) {
		this.numberOfConfigurations = numberOfConfigurations;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public void preProcess() throws Exception {
		numberOfConfigurations = TWiseSampler.YASA_MAX_SIZE;
		super.preProcess();
	}

	@Override
	protected void addCommandElements() {
		addCommandElement("java");
		addCommandElement("-da");
		addCommandElement("-Xmx14g");
		addCommandElement("-Xms2g");
		addCommandElement("-cp");
		addCommandElement("tools/Pledge/*");
		addCommandElement("pledge.Main");
		addCommandElement("generate_products");
		addCommandElement("-dimacs");
		addCommandElement("-fm");
		addCommandElement(fmFile.toString());
		addCommandElement("-o");
		addCommandElement(outputFile.toString());
		addCommandElement("-timeAllowedMS");
		addCommandElement(Long.toString(timeout));
		addCommandElement("-nbProds");
		addCommandElement(Long.toString(numberOfConfigurations));
	}

	@Override
	public void postProcess() {
		try {
			Files.deleteIfExists(outputFile);
		} catch (IOException e) {
			Logger.getInstance().logError(e);
		}
	}

	@Override
	public SolutionList parseResults() throws IOException {
		if (!Files.isReadable(outputFile)) {
			return null;
		}

		List<String> lines = Files.readAllLines(outputFile);
		if (lines.isEmpty()) {
			return null;
		}

		final Pattern variableNamePattern = Pattern.compile("\\A\\d+->(.*)\\Z");

		final ArrayList<String> featureNames = new ArrayList<>();
		final ListIterator<String> it = lines.listIterator();
		while (it.hasNext()) {
			final String line = it.next().trim();
			Matcher matcher = variableNamePattern.matcher(line);
			if (matcher.matches()) {
				featureNames.add(matcher.group(1));
			} else {
				it.previous();
				break;
			}
		}
		final Variables variables = new Variables(featureNames);

		final ArrayList<LiteralSet> configurationList = new ArrayList<>();
		while (it.hasNext()) {
			final String line = it.next().trim();
			int[] configurationArray = new int[variables.size()];
			String[] featureSelections = line.split(";");
			for (int i = 0; i < configurationArray.length; i++) {
				configurationArray[i] = Integer.parseInt(featureSelections[i]);
			}
			configurationList.add(new LiteralSet(configurationArray, Order.INDEX));
		}
		return new SolutionList(variables, configurationList);
	}

	@Override
	public String getParameterSettings() {
		return "";
	}

}
