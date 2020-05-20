package de.ovgu.featureide.sampling.eval.analyzer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.prop4j.NodeReader;
import org.sk.utils.Logger;

import de.ovgu.featureide.sampling.eval.Constants;

public class PCLocatorWrapper implements ISourceCodeAnalyzer {

	public static List<Path> includes;
	public static Path kmaxfile;
	public static Path platform;
	public static Path mockdir;
	public static Path projectroot;

	private static final Function<Path, Stream<RawPresenceCondition>> presenceConditionLocator = file -> {
		List<String> params = new ArrayList<>();
		params.add("java");
		params.add("-jar");
		params.add("lib/PCLocator.jar");
		params.add("--annotator");
		params.add("featurecopp");
		params.add("--locator");
//		if (kmaxfile != null) {
//			params.add("kmax");
//		} else {
			params.add("simple");
//		}
		params.add("--raw");

		// if (mockdir != null) {
		// params.add("--mockdir");
		// params.add(escapeArgument(mockdir.toAbsolutePath().toString()));
		// }
//		if (includes != null) {
//			params.add("-I");
//			params.addAll(includes.stream().map(Object::toString).map(PCLocatorWrapper::escapeArgument).collect(Collectors.toList()));
//		}
//		if (platform != null) {
//			params.add("--platform");
//			params.add(escapeArgument(platform.toString()));
//		}
//		if (kmaxfile != null) {
//			params.add("--kmaxfile");
//			params.add(escapeArgument(kmaxfile.toString()));
//			params.add("--projectroot");
//			params.add(escapeArgument(projectroot.toAbsolutePath().toString()));
//		}
		params.add(escapeArgument(file.toString()));

		try {
			final ProcessBuilder processBuilder = new ProcessBuilder(params.toArray(new String[0]));
			final Process process = processBuilder.start();

			try (final InputStream errorStream = process.getErrorStream(); final InputStream inputStream = process.getInputStream()) {
				ExecutorService executorService = Executors.newFixedThreadPool(2);
				Future<List<String>> output =
					executorService.submit(() -> new BufferedReader(new InputStreamReader(inputStream)).lines().distinct().collect(Collectors.toList()));
				Future<List<String>> errors =
					executorService.submit(() -> new BufferedReader(new InputStreamReader(errorStream)).lines().collect(Collectors.toList()));
				executorService.shutdown();

				final List<String> errorLines = errors.get();
				final List<String> outputLines = output.get();

				errorLines.stream().forEach(Logger.getInstance()::logError);

				Path filePath = file.toAbsolutePath().normalize();

				Path systemPath = projectroot.toAbsolutePath().normalize();
				Path relativizeFilePath = systemPath.getFileName().resolve(systemPath.relativize(filePath));
				Path outputPath = Constants.presenceConditionsOutput.resolve(relativizeFilePath).getParent();
				Path outputFile = outputPath.resolve(filePath.getFileName().toString() + ".pc");
				Files.deleteIfExists(outputFile);
				Files.createDirectories(outputPath);
				Files.write(outputFile, Arrays.asList(relativizeFilePath.toString()), StandardOpenOption.CREATE);
				Files.write(outputFile, outputLines, StandardOpenOption.APPEND);

				return createRawPresenceConditions(outputLines, filePath);
			}
		} catch (Exception e) {
			Logger.getInstance().logError(e);
			return null;
		}
	};

	private static Stream<RawPresenceCondition> createRawPresenceConditions(final List<String> outputLines, Path filePath) {
		List<RawPresenceCondition> rawPCs = new ArrayList<>(outputLines.size());
		for (String string : outputLines) {
			rawPCs.add(new RawPresenceCondition(string, filePath));
		}
		return rawPCs.stream();
	}

	private static String escapeArgument(String argument) {
		return argument;
	}

	private static final Function<Path, Stream<RawPresenceCondition>> presenceConditionReader = file -> {
		try {
			List<String> lines = Files.readAllLines(file);
			return createRawPresenceConditions(lines.subList(1, lines.size()), Paths.get(lines.get(0)));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	};

	@Override
	public Set<String> getDefineSet(Path root) {
		return Collections.emptySet();
	}

	public Stream<RawPresenceCondition> getPresenceConditions(Function<Path, Stream<RawPresenceCondition>> presenceConditionProvider) {
		fileCount = fileProvider.getFileStream().count();
		fileCounter = 0;
		return fileProvider.getFileStream().peek(this::logFile) //
				.flatMap(presenceConditionProvider);
	}

	public Stream<RawPresenceCondition> extractPresenceConditions(Path root) {
		return getPresenceConditions(presenceConditionLocator);
	}

	public Stream<RawPresenceCondition> readPresenceConditions(Path root) {
		return getPresenceConditions(presenceConditionReader);
	}

	long fileCounter = 0;
	long fileCount = 0;

	private void logFile(Path file) {
		System.out.println("(" + ++fileCounter + "/" + fileCount + ") " + file.toString());
	}

	protected NodeReader nodeReader;
	protected FileProvider fileProvider;

	@Override
	public NodeReader getNodeReader() {
		return nodeReader;
	}

	@Override
	public void setNodeReader(NodeReader nodeReader) {
		this.nodeReader = nodeReader;
	}

	@Override
	public FileProvider getFileProvider() {
		return fileProvider;
	}

	@Override
	public void setFileProvider(FileProvider fileProvider) {
		this.fileProvider = fileProvider;
	}

}
