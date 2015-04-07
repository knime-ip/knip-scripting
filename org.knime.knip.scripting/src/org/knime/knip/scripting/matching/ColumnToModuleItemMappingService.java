package org.knime.knip.scripting.matching;

import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.Service;

/**
 * ColumnToInputMappingService provides information on how to match names of
 * {@link DataTable} columns to names of {@link ModuleItem}s.
 * 
 * @author Jonathan Hale (University of Konstanz)
 *
 */
public interface ColumnToModuleItemMappingService extends Service {

	/**
	 * Interfaces for classes containing a mapping from column name to input
	 * name.
	 * 
	 * @author Jonathan Hale (University of Konstanz)
	 */
	public interface ColumnToModuleItemMapping {
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
		 *            {@link ColumnToInputMapping}
		 */
		void setActive(boolean flag);
	}

	/**
	 * Get a list of all {@link ColumnToModuleItemMapping}s currently contained
	 * in this Service.
	 * 
	 * @return {@link List} containing {@link ColumnToModuleItemMapping} of this
	 *         Service.
	 */
	List<ColumnToModuleItemMapping> getMappingsList();

	/**
	 * Get a {@link ColumnToModuleItemMapping} which maps the given columnName.
	 * 
	 * @param columnName
	 *            name of the column to find a mapping for
	 * @return {@link ColumnToModuleItemMapping} which maps columnName or null
	 *         if none could be found.
	 */
	ColumnToModuleItemMapping getMappingForColumnName(String columnName);

	/**
	 * Get a {@link ColumnToModuleItemMapping} which maps the given inputName.
	 * 
	 * @param inputName
	 *            name of the input to find a mapping for
	 * @return {@link ColumnToModuleItemMapping} which maps a column to
	 *         inputName or null if none could be found.
	 */
	ColumnToModuleItemMapping getMappingForModuleItemName(String inputName);

	/**
	 * Get a {@link ColumnToModuleItemMapping} which maps the given columns
	 * name.
	 * 
	 * @param column
	 *            column which to find a mapping for
	 * @return {@link ColumnToModuleItemMapping} which maps the columns name or
	 *         null if none could be found.
	 */
	ColumnToModuleItemMapping getMappingForColumn(DataColumnSpec column);

	/**
	 * Get a {@link ColumnToModuleItemMapping} which maps to the given items
	 * name.
	 * 
	 * @param item
	 *            {@link ModuleItem} to find a mapping for
	 * @return {@link ColumnToModuleItemMapping} wich maps top the given module
	 *         items name or null if none could be found.
	 */
	ColumnToModuleItemMapping getMappingForModuleItem(ModuleItem<?> item);

}
