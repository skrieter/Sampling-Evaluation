package de.ovgu.featureide.sampling.eval.analyzer;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.prop4j.NodeReader;

import de.ovgu.featureide.sampling.eval.Constants;

public class SrcMLAnalyzer implements ISourceCodeAnalyzer {

	private static final Function<Path, Path> pathMapper = path -> Constants.srcMLOutput.resolve(path.subpath(1, path.getNameCount()).toString() + ".xml");

	private NodeReader nodeReader;
	private FileProvider fileProvider;

	public SrcMLAnalyzer(FileProvider fileProvider, NodeReader nodeReader) {
		this.nodeReader = nodeReader;
		this.fileProvider = fileProvider;
	}

	@Override
	public Set<String> getDefineSet(Path root) {
		return getFileStream().flatMap(file -> new SrcMLReader.DefineFinder().readExpressions(nodeReader, file)).map(Object::toString).distinct()
				.collect(Collectors.toSet());
	}

	@Override
	public Stream<RawPresenceCondition> readPresenceConditions(Path root) {
		return getFileStream().flatMap(file -> new SrcMLReader().readExpressions(nodeReader, file));
	}

	@Override
	public Stream<RawPresenceCondition> extractPresenceConditions(Path root) {
		return getFileStream().flatMap(file -> new SrcMLReader().readExpressions(nodeReader, file));
	}

	private Stream<Path> getFileStream() {
		return fileProvider.getFileStream().map(pathMapper);
	}

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
