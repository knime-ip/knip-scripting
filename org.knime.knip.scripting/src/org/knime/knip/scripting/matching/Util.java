package org.knime.knip.scripting.matching;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService.ColumnToModuleItemMapping;

public class Util {
	
	/**
	 * Fill a SettingsModelStringArray with the contents of a ColumnToModuleItemMappingService.
	 * @param service service to fill the model with
	 * @param model model to fill
	 * @return model
	 */
	public static SettingsModelStringArray fillStringArraySettingsModel(ColumnToModuleItemMappingService service, SettingsModelStringArray model) {
		List<ColumnToModuleItemMapping> mappings = service.getMappingsList();
		ArrayList<String> out = new ArrayList<String>(mappings.size());
		
		for (ColumnToModuleItemMapping m : mappings) {
			out.add(m.getColumnName() + "\n" + m.getItemName());
		}
		
		model.setStringArrayValue(out.toArray(new String[]{}));
		return model;
	}
	
}
