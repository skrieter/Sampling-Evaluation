package de.ovgu.featureide.sampling.eval.analyzer;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;
import de.ovgu.featureide.fm.core.analysis.cnf.IVariables;
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet;
import de.ovgu.featureide.fm.core.analysis.cnf.Nodes;
import de.ovgu.featureide.fm.core.analysis.cnf.Variables;
import de.ovgu.featureide.fm.core.analysis.cnf.manipulator.remove.CNFSlicer;
import de.ovgu.featureide.fm.core.io.dimacs.DimacsReader;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.sampling.eval.Constants;

public class KconfigDimacsReader {

	private static final Charset charset = Charset.forName("UTF-8");

	public CNF load(String name) throws Exception {
		final Path kbuildPath = Constants.kbuildOutput.resolve(name).toAbsolutePath();
		final Path featureFile = kbuildPath.resolve(name + ".features");
		final Path modelFile = kbuildPath.resolve("model.dimacs");

		final Set<String> featureNames = Files.lines(featureFile, charset).filter(line -> !line.isEmpty()).collect(Collectors.toSet());
		final String source = new String(Files.readAllBytes(modelFile), charset);

		final DimacsReader r = new DimacsReader();
		r.setReadingVariableDirectory(true);
		final CNF cnf = Nodes.convert(r.read(new StringReader(source)));

		final Set<String> dirtyVariables =
			Arrays.stream(cnf.getVariables().getNames()).skip(1).filter(variable -> !featureNames.contains("CONFIG_" + variable)).collect(Collectors.toSet());
		final CNF slicedCNF = LongRunningWrapper.runMethod(new CNFSlicer(cnf, dirtyVariables));

		final IVariables slicedVariables = slicedCNF.getVariables();
		final Variables newVariables = new Variables(featureNames);
		final ClauseList newClauseList = new ClauseList();

		for (LiteralSet clause : slicedCNF.getClauses()) {
			final int[] oldLiterals = clause.getLiterals();
			final int[] newLiterals = new int[oldLiterals.length];
			for (int i = 0; i < oldLiterals.length; i++) {
				final int literal = oldLiterals[i];
				newLiterals[i] = newVariables.getVariable("CONFIG_" + slicedVariables.getName(literal), literal > 0);
			}
			newClauseList.add(new LiteralSet(newLiterals));
		}

		return new CNF(newVariables, newClauseList);
	}

}
