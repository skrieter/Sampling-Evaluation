package de.ovgu.featureide.sampling.eval;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.ClauseList;

public final class Expressions implements Serializable {

	private static final long serialVersionUID = 2430619166140896491L;

	private CNF cnf;
	private final List<List<ClauseList>> expressions = new ArrayList<>(1);

	public static Expressions readConditions(String systemName, String fileName) throws IOException {
		try (ObjectInputStream in = new ObjectInputStream(
				new FileInputStream(Constants.expressionsOutput.resolve(systemName).resolve(fileName + "." + Constants.pcFileExtension).toString()))) {
			return (Expressions) in.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void writeConditions(Expressions expressions, String systemName, String fileName) {
		try {
			Path filePath = Constants.expressionsOutput.resolve(systemName).resolve(fileName + "." + Constants.pcFileExtension);
			Files.deleteIfExists(filePath);
			Files.createDirectories(Constants.expressionsOutput.resolve(systemName));
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath.toString()))) {
				out.writeObject(expressions);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CNF getCnf() {
		return cnf;
	}

	public void setCnf(CNF cnf) {
		this.cnf = cnf;
	}

	public void setExpressions(List<List<ClauseList>> expressions) {
		this.expressions.clear();
		this.expressions.addAll(expressions);
	}

	public List<List<ClauseList>> getExpressions() {
		return expressions;
	}

}
