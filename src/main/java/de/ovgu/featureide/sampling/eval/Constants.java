package de.ovgu.featureide.sampling.eval;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Predicate;

public class Constants {

	public final static Path systems = Paths.get("resources/systems");

	public static final boolean useSrcml = false;
	public final static Path srcMLOutput = Paths.get("gen/srcml");
	public final static String srcMLPath = "lib/srcML/bin/srcml.exe";

	public final static Path kbuildOutput = Paths.get("gen/kbuild");
	public final static Path expressionsOutput = Paths.get("gen/presenceConditions");
	public final static Path presenceConditionListOutput = Paths.get("gen/presenceConditions");
	public final static Path presenceConditionsOutput = Paths.get("gen/presenceConditions");

	public final static String convertedPCFileName = "pclist";
	public final static String convertedPCFMFileName = "pclist_fm";
	public final static String groupedPCFileName = "grouped_";

	public final static String pcFileExtension = "s";

	public static final Function<String, Predicate<Path>> fileFilterCreator =
		regex -> file -> Files.isReadable(file) && Files.isRegularFile(file) && file.getFileName().toString().matches(regex);

	public static final String FileNameRegex = ".+[.](c|h|cxx|hxx)\\Z";
	public static final Predicate<Path> fileFilter = fileFilterCreator.apply(FileNameRegex);

}
