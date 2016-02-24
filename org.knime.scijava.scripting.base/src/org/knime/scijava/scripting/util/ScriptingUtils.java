package org.knime.scijava.scripting.util;

import org.knime.scijava.commands.mapping.ColumnToInputMappingService;
import org.knime.scijava.commands.settings.NodeSettingsService;
import org.knime.scijava.scripting.base.CompileProductHelper;

public class ScriptingUtils {

	/**
	 * Stores the
	 *
	 * @param settings
	 *            the SettingsService to store the settings of this product
	 *            (will be cleared, should be unique for this module!)
	 * @param inputMappingService
	 *            the input mapping service for this module
	 * @param compileProduct
	 *            the compile product
	 */
	public static void createSettingsForCompileProduct(
			NodeSettingsService settings,
			ColumnToInputMappingService inputMappingService,
			CompileProductHelper compileProduct) {

		settings.clear();
		// create settingsmodels for unmapped inputs
		compileProduct.inputs().forEach(input -> {
			if (!inputMappingService.isItemMapped(input.getName())) {
				settings.createAndAddSettingsModel(input, false);
			}
		});
	}
}
