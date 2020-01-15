package de.ovgu.featureide.sampling.eval.properties;

import de.ovgu.featureide.fm.benchmark.properties.StringListProperty;

public class GroupingProperty extends StringListProperty {

	public static final String PC_ALL_FM = "pc_all_fm";
	public static final String PC_FOLDER_FM = "pc_folder_fm";
	public static final String PC_FILE_FM = "pc_file_fm";
	public static final String PC_VARS_FM = "pc_vars_fm";
	public static final String PC_ALL = "pc_all";
	public static final String PC_FOLDER = "pc_folder";
	public static final String PC_FILE = "pc_file";
	public static final String PC_VARS = "pc_vars";
	public static final String FM_ONLY = "fm_only";

	public GroupingProperty() {
		super("grouping");
	}

}
