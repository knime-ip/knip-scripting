package org.knime.scijava.scripting.util;

import org.knime.scijava.commands.mapping.ColumnToInputMappingService;
import org.knime.scijava.commands.settings.NodeSettingsService;
import org.knime.scijava.scripting.base.CompileProductHelper;
import org.scijava.module.ModuleItem;

public class ScriptingUtils {

	/**
	 * Stores the
	 * 
	 * @param settings
	 *            the SettingsService to store the settings of this product
	 *            (will be cleared, should be unique for this module!)
	 * @param inputMappingService the input mapping service for this module
	 * @param compileProduct the compile product
	 */
	public static void createSettingsForCompileProduct(
			NodeSettingsService settings,
			ColumnToInputMappingService inputMappingService,
			CompileProductHelper compileProduct) {

		settings.clear();
		for (final ModuleItem<?> i : compileProduct.inputs()) {
			final String inputName = i.getName();

			// only unmapped inputs need an ui element.
			boolean isMapped = inputMappingService.isItemMapped(inputName);
			if (!isMapped) {
				settings.createAndAddSettingsModel(i);
			}
		}
	}
}
