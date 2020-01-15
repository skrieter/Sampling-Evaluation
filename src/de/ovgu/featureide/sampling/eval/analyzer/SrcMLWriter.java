package de.ovgu.featureide.sampling.eval.analyzer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import de.ovgu.featureide.sampling.eval.Constants;

public class SrcMLWriter {

	private static long countFiles = 0;

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			return;
		}
		final Path root = Paths.get("systems/" + args[0]);

		Path output = Constants.srcMLOutput.resolve(root.getFileName());
		if (Files.exists(output)) {
			Files.walkFileTree(output, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
		Files.createDirectories(output);

		countFiles = Files.walk(root).filter(Constants.fileFilter).count();

		Files.walk(root).parallel().filter(Constants.fileFilter).forEach(file -> {
			System.out.println(countFiles--);
			final Path outputFile = output.resolve(root.relativize(file));
			final Path outputFileDir = outputFile.subpath(0, outputFile.getNameCount() - 1);
			try {
				if (!Files.exists(outputFileDir)) {
					Files.createDirectories(outputFileDir);
				}
				String[] params = new String[] { Constants.srcMLPath, file.toString(), "-o", outputFile.toString() + ".xml" };

				Runtime.getRuntime().exec(params).waitFor();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});
		System.out.println("Done!");
	}

}
