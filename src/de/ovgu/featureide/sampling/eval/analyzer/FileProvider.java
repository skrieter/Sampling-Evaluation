package de.ovgu.featureide.sampling.eval.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FileProvider {

	public static final String CFileRegex = ".+[.](c|h|cxx|hxx)\\Z";
	public static final String PCFileRegex = ".+[.](pc)\\Z";

	public static final Function<String, Predicate<Path>> fileFilterCreator =
		regex -> file -> Files.isReadable(file) && Files.isRegularFile(file) && file.getFileName().toString().matches(regex);

	private final List<Path> excludes = new LinkedList<>();
	private String fileNameRegex = null;

	public Path projectroot;

	public FileProvider(Path projectroot) {
		this.projectroot = projectroot;
	}

	public void setProjectroot(Path projectroot) {
		this.projectroot = projectroot;
	}

	public String getFileNameRegex() {
		return fileNameRegex;
	}

	public void setFileNameRegex(String fileNameRegex) {
		this.fileNameRegex = fileNameRegex;
	}

	public void addExclude(Path path) {
		excludes.add(projectroot.resolve(path));
	}

	public Stream<Path> getFiles(Path root, boolean recursive) {
		return getPaths(root, getFilePredicate(), recursive ? Integer.MAX_VALUE : 1);
	}

	public Stream<Path> getFolders(Path root) {
		return getPaths(root, getFolderPredicate(root), Integer.MAX_VALUE);
	}

	public Stream<Path> getFolderStream() {
		return getFolders(projectroot);
	}

	public Stream<Path> getFileStream() {
		return getFolders(projectroot).flatMap(folder -> getFiles(folder, false));
	}

	private Stream<Path> getPaths(Path root, Predicate<Path> filter, int maxDepth) {
		try {
			return Files.walk(root, maxDepth).sequential().filter(filter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Predicate<Path> getFilePredicate() {
		Predicate<Path> filter = file -> Files.isRegularFile(file);
		if (fileNameRegex != null && !fileNameRegex.isEmpty()) {
			filter = filter.and(fileFilterCreator.apply(fileNameRegex));
		}
		return filter;
	}

	private Predicate<Path> getFolderPredicate(Path root) {
		Predicate<Path> filter = file -> Files.isDirectory(file);
		filter = filter.and(file -> {
			int nameCount = root.getNameCount();
			int nameCount2 = file.getNameCount();
			if (nameCount < nameCount2) {
				Path subPath = file.subpath(nameCount, nameCount2);
				for (Path path : subPath) {
					if (path.toString().startsWith(".")) {
						return false;
					}
				}
				for (Path excludePath : excludes) {
					if (file.startsWith(excludePath)) {
						return false;
					}
				}
			}
			return true;
		});
		return filter;
	}

}
