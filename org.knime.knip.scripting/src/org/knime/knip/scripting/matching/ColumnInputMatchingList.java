package org.knime.knip.scripting.matching;

import java.util.ArrayList;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.scijava.module.ModuleInfo;

public class ColumnInputMatchingList extends ArrayList<ColumnInputMatching> {
	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = -3805178750028114505L;
	
	
	public static final String CFG_NUM_ENTRIES = "NumEntries";
	public static final String CFG_ENTRY = "Entry";
	
	public void loadSettingsForDialog(final Config config, DataTableSpec tableSpec, ModuleInfo moduleInfo) throws InvalidSettingsException {
		int numEntries = config.getInt(CFG_NUM_ENTRIES);
		for (int i = 0; i < numEntries; ++i) {
			ColumnInputMatching e = new ColumnInputMatching(false, null, null);
			e.loadSettingsForDialog(config.getConfig(CFG_ENTRY + i), tableSpec, moduleInfo);
			add(e);
		}
	}
	
	public void saveSettingsForDialog(final Config config) {
		config.addInt(CFG_NUM_ENTRIES, size());
		
		int i = 0;
		for (ColumnInputMatching e : this) {
			e.saveSettingsForDialog(config.addConfig(CFG_ENTRY + i++));
		}
	}
}
