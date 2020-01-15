package de.ovgu.featureide.sampling.eval.analyzer;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;

public class PresenceCondition implements Serializable {

	private static final long serialVersionUID = 2504588981897467392L;

	private final CNF cnf;
	private final CNF dnf;
	private final URI filePath;

	public PresenceCondition(Path filePath, CNF cnf, CNF dnf) {
		this.filePath = filePath.toUri();
		this.cnf = cnf;
		this.dnf = dnf;
	}

	public Path getFilePath() {
		return Paths.get(filePath); 
	}

	public CNF getCnf() {
		return cnf;
	}

	public CNF getDnf() {
		return dnf;
	}

	@Override
	public int hashCode() {
		return (dnf == null) ? 0 : dnf.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(dnf, ((PresenceCondition) obj).dnf);
	}

}
