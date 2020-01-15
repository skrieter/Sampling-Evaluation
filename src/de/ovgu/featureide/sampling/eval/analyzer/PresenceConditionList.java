package de.ovgu.featureide.sampling.eval.analyzer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.sampling.eval.Constants;

public class PresenceConditionList extends ArrayList<PresenceCondition> {

	private static final long serialVersionUID = -5665893727516058122L;

	private final CNF formula;
	
	private ArrayList<String> pcNames;

	public PresenceConditionList(List<PresenceCondition> list, CNF formula) {
		super(list);
		this.formula = formula;
	}

	public CNF getFormula() {
		return formula;
	}

	public static PresenceConditionList readPCList(String systemName, String fileName) throws IOException {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(
				Constants.presenceConditionListOutput.resolve(systemName).resolve(fileName + "." + Constants.pcFileExtension).toString()))) {
			return (PresenceConditionList) in.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void writePCList(PresenceConditionList pcList, String systemName, String fileName) {
		try {
			Path filePath = Constants.presenceConditionListOutput.resolve(systemName).resolve(fileName + "." + Constants.pcFileExtension);
			Files.deleteIfExists(filePath);
			Files.createDirectories(Constants.presenceConditionListOutput);
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath.toString()))) {
				out.writeObject(pcList);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> getPCNames() {
		return pcNames;
	}

	public void setPCNames(ArrayList<String> pcNames) {
		this.pcNames = pcNames;
	}

}
