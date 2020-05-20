package de.ovgu.featureide.sampling.eval.analyzer;

import java.io.Serializable;
import java.nio.file.Path;

import org.prop4j.Node;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;

public class RawPresenceCondition implements Serializable {

	private static final long serialVersionUID = 231L;

	private final String formula;
	private final Path filePath;
	private CNF cnf;
	private CNF dnf;

	private transient Node node;

	public RawPresenceCondition(String formula, Path filePath) {
		this.formula = formula;
		this.filePath = filePath;
	}

	public String getFormula() {
		return formula;
	}

	public Path getFilePath() {
		return filePath;
	}

	public CNF getCnf() {
		return cnf;
	}

	public void setCnf(CNF cnf) {
		this.cnf = cnf;
	}

	public CNF getDnf() {
		return dnf;
	}

	public void setDnf(CNF dnf) {
		this.dnf = dnf;
	}

	public boolean hasFormula() {
		return getCnf() != null && !getCnf().getClauses().isEmpty() && getDnf() != null
				&& !getDnf().getClauses().isEmpty();
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}
}
