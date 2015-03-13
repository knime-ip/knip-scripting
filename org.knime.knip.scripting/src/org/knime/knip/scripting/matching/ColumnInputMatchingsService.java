package org.knime.knip.scripting.matching;

import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.Pair;
import org.scijava.module.ModuleInfo;
import org.scijava.service.Service;

/**
 * TODO
 * @author Jonathan Hale (University of Konstanz)
 *
 */
public interface ColumnInputMatchingsService extends Service {
	
	/**
	 * @return A List of all {@link ColumnInputMatching}s held by this service.
	 */
	List<Pair<String, String>> getColumnInputMatchings();
	
	/**
	 * Get a column matching for an input name.
	 * @param inputName
	 * @return the column matching for the input or null if none could be found.
	 */
	String getColumnNameFor(String inputName);
	
	/**
	 * Return number of matchings currently held.
	 * @return number of matchings held by this service.
	 */
	int getMatchingsCount();

	/**
	 * Add a {@link ColumnInputMatching} to this service.
	 * @param cim the matching to add.
	 * 
	 * @see #removeColumnInputMatching(ColumnInputMatching)
	 */
	void addColumnInputMatching(String input, String col);

	/**
	 * Remove a {@link ColumnInputMatching} from this service.
	 * @param cim the columnInputMatching to remove.
	 * 
	 * @see #addColumnInputMatching(ColumnInputMatching)
	 */
	void removeColumnInputMatchingFor(String input);
	
	/**
	 * Save Settings to a KNIME config.
	 * @param config
	 */
	void saveSettingsTo(Config config);
	
	/**
	 * Load settings from a KNIME config.
	 * @param config the {@link Config} to load from.
	 * @param spec the {@link DataTableSpec} to use when resolving columns.
	 * @param info the {@link ModuleInfo} to use when resolving input names.
	 * @throws InvalidSettingsException 
	 */
	void loadSettingsFrom(Config config, DataTableSpec spec, ModuleInfo info) throws InvalidSettingsException;

	void removeColumnInputMatching(int row);
	
}
