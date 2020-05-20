package de.ovgu.featureide.sampling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.prop4j.Node;
import org.prop4j.NodeReader;
import org.prop4j.Or;

public class TestSuiteParser {

	private static final Pattern regex = Pattern.compile("\\ATestCase (\\d+) with configurations (.+):\\Z");

	private static final Pattern regex2 = Pattern.compile("\\ASolution \\[literals=\\[(.+)\\]\\]\\Z");

	private static final NodeReader nr = new NodeReader();
	static {
		nr.activatePropositionalModelSymbols();
	}

	private static class FileFilter implements Predicate<Path> {

		private final Pattern regex;

		public FileFilter(Pattern regex) {
			this.regex = regex;
		}

		public FileFilter(String regex) {
			this.regex = Pattern.compile(regex);
		}

		@Override
		public boolean test(Path t) {
			return regex.matcher(t.getFileName().toString()).matches();
		}
	}

	private static class MinDir implements Comparator<Path> {

		@Override
		public int compare(Path o1, Path o2) {
			int n1 = Integer.parseInt(o1.getFileName().toString());
			int n2 = Integer.parseInt(o2.getFileName().toString());
			return n1 - n2;
		}
	}

	public static void main(String[] args) throws IOException {
		String systemName = "lcm";
		int t = 2;
		String mode = "presenceCondition_all";

		Path filePath = Paths.get("mutation_tests/" + systemName);

		FileFilter fileFilter = new FileFilter("testSuite_mutation_\\d+[.]txt");
		List<Node> mutations = Files.walk(filePath) //
				.filter(fileFilter) // Filter mutation files
				.peek(System.out::println) // Console output
				.map(TestSuiteParser::extractNodes) // Extract test conditions
				.collect(Collectors.toList()); // To list
		mutations.forEach(System.out::println);

		Path resultDir = Paths.get("gen/results/");
		MinDir minDir = new MinDir();
		FileFilter fileFilter2 = new FileFilter("\\d+");
		Optional<Path> lastResultPath = Files.walk(resultDir).filter(Files::isDirectory).filter(fileFilter2).min(minDir);
		if (lastResultPath.isPresent()) {
			Path sampleDir = lastResultPath.get().resolve("/samples/");
			String name = mode + "_" + t + "_" + systemName;
			final ArrayList<String> features = getFeatures(name, sampleDir);
			final List<List<String>> selectedFeatureList = getSamples(sampleDir, name, features);
		}
	}

	private static List<List<String>> getSamples(Path sampleDir, String name, final ArrayList<String> features) throws IOException {
		final Path sampleFile = sampleDir.resolve(name + ".sample");
		final List<List<String>> selectedFeatureList = new ArrayList<>();
		for (String line : Files.readAllLines(sampleFile)) {
			final ArrayList<String> selectedFeatures = new ArrayList<>();
			final Matcher matcher = regex2.matcher(line);
			if (matcher.find()) {
				for (String string : matcher.group(1).split(", ")) {
					final int index = Integer.parseInt(string);
					if (index > 0) {
						selectedFeatures.add(features.get(index - 1));
					}
				}
			}
			selectedFeatureList.add(selectedFeatures);
		}
		return selectedFeatureList;
	}

	private static ArrayList<String> getFeatures(String name, Path sampleDir) throws IOException {
		Path featureFile = sampleDir.resolve(name + ".features");
		final ArrayList<String> features = new ArrayList<>();
		for (String line : Files.readAllLines(featureFile)) {
			final String featureName = line.trim();
			if (!featureName.isEmpty()) {
				features.add(featureName.substring("__SELECTED_FEATURE_".length()));
			}
		}
		return features;
	}

	private static Node extractNodes(Path file) {
		final List<Node> nodeList = new ArrayList<>();
		try {
			for (String line : Files.readAllLines(file)) {
				final Matcher matcher = regex.matcher(line);
				if (matcher.find()) {
					nodeList.add(nr.stringToNode(matcher.group(2)).toRegularCNF());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new Or(nodeList);
	}

}
