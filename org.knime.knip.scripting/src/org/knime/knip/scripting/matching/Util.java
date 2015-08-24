package org.knime.knip.scripting.matching;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * Utility methods for column input mappings.
 *
 * This class contains static methods to serialize and deserialize
 * {@link ColumnToModuleItemMapping}s as {@link SettingsModelStringArray}.
 *
 * @author Jonathan Hale (University of Konstanz)
 *
 */
public class Util {

	/**
	 * Fill a SettingsModelStringArray with the contents of a
	 * ColumnToModuleItemMappingService. The mappings are stored as Strings in
	 * the format "columnName\nactive\ninputName".
	 *
	 * @param service
	 *            service to fill the model with
	 * @param model
	 *            model to fill
	 * @return model
	 */
	public static SettingsModelStringArray fillStringArraySettingsModel(
			final ColumnToModuleItemMappingService service,
			final SettingsModelStringArray model) {
		final List<ColumnToModuleItemMapping> mappings = service
				.getMappingsList();
		final ArrayList<String> out = new ArrayList<String>(mappings.size());

		for (final ColumnToModuleItemMapping m : mappings) {
			out.add(m.getColumnName() + "\n"
					+ (m.isActive() ? "true" : "false") + "\n"
					+ m.getItemName());
		}

		model.setStringArrayValue(out.toArray(new String[] {}));
		return model;
	}

	/**
	 * Add contents of a {@link SettingsModelStringArray} to a
	 * {@link ColumnToModuleItemMappingService}.
	 *
	 * @param model
	 *            model to get the contents from
	 * @param service
	 *            service to add the contents of model to
	 * @return true on success, false if model contained a String not in the
	 *         format "columnName\nactive\ninputName".
	 */
	public static boolean fillColumnToModuleItemMappingService(
			final SettingsModelStringArray model,
			final ColumnToModuleItemMappingService service) {
		for (final String s : model.getStringArrayValue()) {
			final String[] names = s.split("\n");

			if (names.length != 3) {
				// Invalid format!
				return false;
			}
			
			/* 
			 * format is
			 * [0] column name
			 * [1] active, either "true" or "false"
			 * [2] module input name
			 */
			final ColumnToModuleItemMapping mapping = service.addMapping(
					names[0], names[2]);

			if (names[1].equals("false")) {
				mapping.setActive(false);
			} // else: keep active as default "true"
		}
		// done, no problems
		return true;
	}

}
