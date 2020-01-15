package de.ovgu.featureide.sampling.eval.analyzer;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.prop4j.NodeReader;

public interface ISourceCodeAnalyzer {

	Set<String> getDefineSet(Path root);

	Stream<RawPresenceCondition> readPresenceConditions(Path root);

	Stream<RawPresenceCondition> extractPresenceConditions(Path root);

	NodeReader getNodeReader();

	void setNodeReader(NodeReader nodeReader);

	FileProvider getFileProvider();

	void setFileProvider(FileProvider fileProvider);

}
