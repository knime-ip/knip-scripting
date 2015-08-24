package org.knime.knip.scripting.matching;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService.ColumnToModuleItemMappingChangeEventDispatcher;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;

/**
 * Interfaces for classes containing a mapping from column name to input
 * name.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public interface ColumnToModuleItemMapping extends
		ColumnToModuleItemMappingChangeEventDispatcher {
	/**
	 * Get column name.
	 *
	 * @return name of the column which is mapped
	 */
	String getColumnName();

	/**
	 * Get module input name.
	 *
	 * @return name of the input which is mapped to
	 */
	String getItemName();

	/**
	 * Get the {@link DataColumnSpec} which matches the column name of this
	 * mapping.
	 *
	 * @param spec
	 *            {@link DataTableSpec} to get {@link DataColumnSpec} from
	 * @return a {@link DataColumnSpec} with the column name contained in
	 *         this mapping or null if spec did not contain a column with
	 *         that name
	 */
	DataColumnSpec getColumnSpec(DataTableSpec spec);

	/**
	 * Get index of the column which is mapped to.
	 *
	 * @param spec
	 *            {1link DataTableSpec} to find the column in
	 * @return column index or -1 if column could not be found in spec
	 */
	Integer getColumnIndex(DataTableSpec spec);

	/**
	 * Get the {@link ModuleItem} with name contained by this mapping.
	 *
	 * @param module
	 *            the module to find the {@link ModuleItem} in
	 * @return {@link ModuleItem} with the contained name or null if module
	 *         does not have a item with that name.
	 */
	ModuleItem<?> getModuleItem(Module module);

	/**
	 * Check if this column to module item mapping is currently active.
	 *
	 * @return true if active, false otherwise.
	 */
	boolean isActive();

	/**
	 * Set this mapping as active or inactive. Inactive columns input
	 * Mappings should usually not be used by other services.
	 *
	 * @param flag
	 *            set to true to active, false to deactivate this
	 *            {@link ColumnToModuleItemMapping}
	 */
	void setActive(boolean flag);

	/**
	 * Set this mappings column name and update listeners if changed. Needs
	 * to call {@link #fireMappingColumnChanged(String)}.
	 *
	 * @param columnName
	 *            name of the column to set to
	 */
	void setColumnName(String columnName);

	/**
	 * Set this mappings item name and update listeners if changed. Needs to
	 * call {@link #fireMappingColumnChanged(String)}.
	 *
	 * @param itemName
	 *            name of the item to set to.
	 */
	void setItemName(String itemName);
}