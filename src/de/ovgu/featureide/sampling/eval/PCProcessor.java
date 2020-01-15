package de.ovgu.featureide.sampling.eval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.prop4j.ErrorLiteral;
import org.prop4j.Literal;
import org.prop4j.Node;
import org.prop4j.NodeReader;
import org.prop4j.NodeReader.ErrorHandling;

import de.ovgu.featureide.fm.benchmark.ABenchmark;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;
import de.ovgu.featureide.fm.core.analysis.cnf.IVariables;
import de.ovgu.featureide.fm.core.analysis.cnf.Nodes;
import de.ovgu.featureide.fm.core.analysis.cnf.Variables;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.NoAbstractCNFCreator;
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationGenerator;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.sampling.eval.analyzer.FileProvider;
import de.ovgu.featureide.sampling.eval.analyzer.ISourceCodeAnalyzer;
import de.ovgu.featureide.sampling.eval.analyzer.PCLocatorWrapper;
import de.ovgu.featureide.sampling.eval.analyzer.PresenceCondition;
import de.ovgu.featureide.sampling.eval.analyzer.PresenceConditionList;
import de.ovgu.featureide.sampling.eval.analyzer.RawPresenceCondition;
import de.ovgu.featureide.sampling.eval.analyzer.SrcMLAnalyzer;
import de.ovgu.featureide.sampling.eval.properties.GroupingProperty;

public class PCProcessor {

	private ISourceCodeAnalyzer sourceCodeAnalyzer;
	private NodeReader nodeReader;
	private CNF fmFormula;
	private Path systemPath;
	private boolean inititalized = false;

	private final Object idObject = new Object();

	public Function<PresenceCondition, ?> allGrouper = pc -> idObject;
	public Function<PresenceCondition, ?> fileGrouper = PresenceCondition::getFilePath;
	public Function<PresenceCondition, ?> folderGrouper = pc -> pc.getFilePath().getParent();

	public CNF readModel(ABenchmark<?> benchmark, String name) throws Exception {
		final IFeatureModel fm = benchmark.init(name);
		return fm == null ? null : new FeatureModelFormula(fm).getElement(new NoAbstractCNFCreator()).normalize();
	}

	public PresenceConditionList convert() {
		if (!Constants.useSrcml) {
			Path path = Constants.presenceConditionsOutput.resolve(systemPath.getFileName());
			if (!Files.isReadable(path)) {
				return null;
			}
			FileProvider fileProvider = new FileProvider(path);
			fileProvider.setFileNameRegex(FileProvider.PCFileRegex);
			sourceCodeAnalyzer.setFileProvider(fileProvider);
		} else {
			// TODO implement srcml reader
		}

		final Collection<String> pcNames = new LinkedHashSet<>();
		final Stream<RawPresenceCondition> rawPcs = sourceCodeAnalyzer.readPresenceConditions(systemPath);
		final Stream<RawPresenceCondition> processedPCs = rawPcs //
				.peek(pc -> System.out.println("\t" + pc.getFormula())) //
				.peek(pc -> System.out.println("\t" + getDepth(pc))) //
				.peek(pc -> pc.setNode(convertToNode(pc))) //
				.filter(pc -> pc.getNode() != null) //
				.peek(pc -> setNF(pc, pcNames)) //
				.filter(pc -> pc.hasFormula()) //
				.peek(pc -> System.out.println("\t\t" + pc.getDnf().getClauseString())) //
				.peek(pc -> System.out.println("\t\t" + pc.getCnf().getClauseString())) //
				.collect(Collectors.toList()).stream();
		rawPcs.close();

		CNF formula = fmFormula != null ? fmFormula : new CNF(new Variables(pcNames));

		List<PresenceCondition> convertedPCs = processedPCs //
				.peek(pc -> adaptClauseList((Variables) formula.getVariables(), pc))
				.map(pc -> new PresenceCondition(pc.getFilePath(), pc.getCnf(), pc.getDnf())) //
				.collect(Collectors.toList());

		PresenceConditionList presenceConditionList = new PresenceConditionList(convertedPCs, formula);
		presenceConditionList.setPCNames(new ArrayList<>(pcNames));

		return presenceConditionList;
	}

	public Expressions group(String grouping) throws Exception {
		switch (grouping) {
		case GroupingProperty.PC_ALL_FM:
		case GroupingProperty.PC_ALL:
			return group(allGrouper);
		case GroupingProperty.PC_FOLDER_FM:
		case GroupingProperty.PC_FOLDER:
			return group(folderGrouper);
		case GroupingProperty.PC_FILE_FM:
		case GroupingProperty.PC_FILE:
			return group(fileGrouper);
		case GroupingProperty.FM_ONLY:
		case GroupingProperty.PC_VARS:
			return groupVars();
		case GroupingProperty.PC_VARS_FM:
			return groupPCFMVars();
		default:
			return null;
		}
	}

	public boolean extract() {

		if (Files.isReadable(systemPath)) {
			try {
				Path outputPath = Constants.presenceConditionsOutput.resolve(systemPath.getFileName());
				Files.createDirectories(outputPath);
				if (!Constants.useSrcml) {
					FileProvider fileProvider = new FileProvider(systemPath);
					fileProvider.setFileNameRegex(FileProvider.CFileRegex);
					fileProvider.addExclude(Paths.get("scripts"));
					fileProvider.addExclude(Paths.get("examples"));
					fileProvider.addExclude(Paths.get("include/config"));
					sourceCodeAnalyzer.setFileProvider(fileProvider);
				}

				final Stream<RawPresenceCondition> rawPcs = sourceCodeAnalyzer.extractPresenceConditions(systemPath);
				rawPcs.forEach(pc -> System.out.println("\t" + pc.getFormula()));
				rawPcs.close();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				Path outputPath = Constants.presenceConditionsOutput.resolve(systemPath.getFileName());
				Files.createDirectories(outputPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public Expressions group(Function<PresenceCondition, ?> grouper) {
		PresenceConditionList pcList = null;
		try {
			pcList = PresenceConditionList.readPCList(systemPath.getFileName().toString(),
					fmFormula != null ? Constants.convertedPCFMFileName : Constants.convertedPCFileName);
		} catch (FileNotFoundException e) {} catch (IOException e) {
			e.printStackTrace();
		}
		if (pcList == null) {
			return null;
		}
		Map<?, List<PresenceCondition>> groupedPCs = pcList.stream().collect(Collectors.groupingBy(grouper));
		final Expressions expressions = new Expressions();
		groupedPCs.values().stream().map(this::createExpressions).forEach(expressions.getExpressions()::add);
		expressions.setCnf(pcList.getFormula());
		return expressions;
	}

	public Expressions groupVars() {
		PresenceConditionList pcList = null;
		try {
			pcList = PresenceConditionList.readPCList(systemPath.getFileName().toString(),
					fmFormula != null ? Constants.convertedPCFMFileName : Constants.convertedPCFileName);
		} catch (FileNotFoundException e) {} catch (IOException e) {
			e.printStackTrace();
		}
		if (pcList == null) {
			return null;
		}
		final IVariables newVariables = pcList.getFormula().getVariables();
		List<List<ClauseList>> convertLiterals = TWiseConfigurationGenerator.convertLiterals(newVariables.getLiterals());

		final Expressions expressions = new Expressions();
		expressions.setExpressions(convertLiterals);
		expressions.setCnf(pcList.getFormula());
		return expressions;
	}

	public Expressions groupPCFMVars() {
		if (fmFormula == null) {
			throw new RuntimeException();
		}
		PresenceConditionList pcList = null;
		try {
			pcList = PresenceConditionList.readPCList(systemPath.getFileName().toString(), Constants.convertedPCFMFileName);
		} catch (FileNotFoundException e) {} catch (IOException e) {
			e.printStackTrace();
		}
		if (pcList == null) {
			return null;
		}
		final IVariables newVariables = pcList.getFormula().getVariables();
		List<List<ClauseList>> convertLiterals = TWiseConfigurationGenerator.convertLiterals(newVariables.convertToVariables(pcList.getPCNames()));

		final Expressions expressions = new Expressions();
		expressions.setExpressions(convertLiterals);
		expressions.setCnf(pcList.getFormula());
		return expressions;
	}

	public void init(CNF fmFormula, String completeName) throws Exception {
		if (!inititalized) {
			inititalized = true;
			final String name = completeName;
			final Path kbuildPath = Constants.kbuildOutput.resolve(completeName).toAbsolutePath();
			systemPath = Constants.systems.resolve(name).toAbsolutePath().normalize();
			PCLocatorWrapper.projectroot = systemPath;
			if (Files.exists(kbuildPath)) {
				PCLocatorWrapper.includes = Arrays.asList(systemPath.resolve("include"));
				PCLocatorWrapper.mockdir = kbuildPath;
				PCLocatorWrapper.kmaxfile = kbuildPath.resolve(completeName + ".kmax");
				PCLocatorWrapper.platform = kbuildPath.resolve("platform.h");
				// PCLocatorWrapper.platform =
				// systemPath.resolve("include").resolve("platform.h");
				// cnf = new KconfigDimacsReader().load(completeName);
			} else {
				PCLocatorWrapper.includes = null;
				PCLocatorWrapper.mockdir = null;
				PCLocatorWrapper.kmaxfile = null;
				PCLocatorWrapper.platform = null;
			}

			this.fmFormula = fmFormula;

			nodeReader = new NodeReader();
			nodeReader.activateShortSymbols2();
			nodeReader.activateJavaSymbols();
			nodeReader.setIgnoreMissingFeatures(ErrorHandling.REMOVE);
			nodeReader.setIgnoreUnparsableSubExpressions(ErrorHandling.REMOVE);
			if (fmFormula != null) {
				String[] names = fmFormula.getVariables().getNames();
				List<String> fmNames = Arrays.asList(names).subList(1, names.length);
				nodeReader.setFeatureNames(fmNames);
			} else {
				nodeReader.setFeatureNames(null);
			}

			final FileProvider fileProvider = new FileProvider(systemPath);
			if (Constants.useSrcml) {
				fileProvider.setFileNameRegex(".+[.](xml)\\Z");
				sourceCodeAnalyzer = new SrcMLAnalyzer(fileProvider, nodeReader);
			} else {
				sourceCodeAnalyzer = new PCLocatorWrapper();
				sourceCodeAnalyzer.setNodeReader(nodeReader);
			}
		}
	}

	private int getDepth(RawPresenceCondition rawPresenceCondition) {
		Node cleanedNode = convertToNode(rawPresenceCondition);
		return cleanedNode != null ? cleanedNode.getMaxDepth() : 0;
	}

	private void setNF(RawPresenceCondition rawPresenceCondition, Collection<String> varNames) {
		final Node node = rawPresenceCondition.getNode();
		varNames.addAll(node.getUniqueContainedFeatures());
		rawPresenceCondition.setCnf(Nodes.convertCNF(node.toRegularCNF(true)));
		rawPresenceCondition.setDnf(Nodes.convertDNF(node.toRegularDNF(true)));
	}

	private Node convertToNode(RawPresenceCondition rawPresenceCondition) {
		return nodeReader.stringToNode(rawPresenceCondition.getFormula());
	}

	private void adaptClauseList(Variables variables, RawPresenceCondition pc) {
		pc.setCnf(pc.getCnf().adapt(variables));
		pc.setDnf(pc.getDnf().adapt(variables));
	}

	private List<ClauseList> createExpressions(List<PresenceCondition> pcList) {
		List<ClauseList> exps = pcList.stream() //
				.flatMap(this::createExpression) //
				.peek(Collections::sort) //
				.distinct() //
				.collect(Collectors.toList());

		sort(exps);
		return exps;
	}

	private final Stream<ClauseList> createExpression(PresenceCondition pc) {
		Stream.Builder<ClauseList> streamBuilder = Stream.builder();
		streamBuilder.accept(pc.getDnf().getClauses());
		streamBuilder.accept(pc.getDnf().getClauses().negate());
		return streamBuilder.build().filter(list -> !list.isEmpty());
	};

	@SuppressWarnings("unused")
	private Node adaptVariableNames(Node node) {
		if (node instanceof ErrorLiteral) {
			final Literal l = (Literal) node;
			final String var = l.var.toString();
			if (var.startsWith("CONFIG_")) {
				node = new Literal(var.substring("CONFIG_".length()), l.positive);
			}
		} else {
			Node[] children = node.getChildren();
			if (children != null) {
				for (int i = 0; i < children.length; i++) {
					children[i] = adaptVariableNames(children[i]);
				}
			}
		}
		return node;
	}

	private void sort(List<ClauseList> exps) {
		Collections.sort(exps, (Comparator<ClauseList>) (o1, o2) -> {
			final int clauseCountDiff = o1.size() - o2.size();
			if (clauseCountDiff != 0) {
				return clauseCountDiff;
			}
			int clauseLengthDiff = 0;
			for (int i = 0; i < o1.size(); i++) {
				clauseLengthDiff += o1.get(i).size() - o2.get(i).size();
			}
			return clauseLengthDiff;
		});
	}

}
