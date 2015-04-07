package org.knime.knip.scripting.matching;

import java.util.EventListener;
import java.util.EventObject;
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
	 * Event for {@link ColumnToModuleItemMapping} changes.
	 * 
	 * @author Jonathan Hale (University of Konstanz)
	 */
	class ColumnToModuleItemMappingChangeEvent extends EventObject {

		/**
		 * Generated serialVersionID
		 */
		private static final long serialVersionUID = 8652877000556694115L;

		/**
		 * Constructor
		 * 
		 * @param source
		 *            the changed {@link ColumnToModuleItemMapping}
		 */
		public ColumnToModuleItemMappingChangeEvent(
				ColumnToModuleItemMapping source) {
			super(source);
		}

		/**
		 * Get the changed {@link ColumnToModuleItemMapping}.
		 * 
		 * @return the changed mapping
		 */
		public ColumnToModuleItemMapping getSourceMapping() {
			return (ColumnToModuleItemMapping) source;
		}

	}

	/**
	 * Interface for classes listening to changes to
	 * {@link ColumnToModuleItemMapping}s.
	 * 
	 * @author Jonathan Hale (University of Konstanz)
	 *
	 */
	public interface ColumnToModuleItemMappingChangeListener extends
			EventListener {
		/**
		 * Called when a {@link ColumnToModuleItemMappingChangeEventDispatcher}
		 * this listener listens to fires a event when the column name changed.
		 * 
		 * @param e
		 *            the event that has been fired.
		 */
		void onMappingColumnChanged(ColumnToModuleItemMappingChangeEvent e);

		/**
		 * Called when a {@link ColumnToModuleItemMappingChangeEventDispatcher}
		 * this listener listens to fires a event when the item name changed.
		 * 
		 * @param e
		 *            the event that has been fired.
		 */
		void onMappingInputChanged(ColumnToModuleItemMappingChangeEvent e);
	}

	/**
	 * Interface for classes which fire ColumnToModuleItemMappingChangeEvent
	 * events.
	 * 
	 * @author Jonathan Hale (University of Konstanz)
	 *
	 */
	public interface ColumnToModuleItemMappingChangeEventDispatcher {

		/**
		 * Add a listener to dispatch events to.
		 * 
		 * @param listener
		 *            the listener to add
		 */
		void addMappingChangeListener(
				ColumnToModuleItemMappingChangeListener listener);

		/**
		 * Remove a listener from this dispatcher.
		 * 
		 * @param listener
		 *            the listener to remove
		 */
		void removeMappingChangeListener(
				ColumnToModuleItemMappingChangeListener listener);

		/**
		 * Call
		 * {@link ColumnToModuleItemMappingChangeListener#onMappingColumnChanged(ColumnToModuleItemMappingChangeEvent)}
		 * on all listeners.
		 * 
		 * @param e
		 *            event to dispatch
		 */
		void fireMappingColumnChanged(ColumnToModuleItemMappingChangeEvent e);

		/**
		 * Call
		 * {@link ColumnToModuleItemMappingChangeListener#onMappingInputChanged(ColumnToModuleItemMappingChangeEvent)}
		 * on all listeners.
		 * 
		 * @param e
		 *            event to dispatch
		 */
		void fireMappingInputChanged(ColumnToModuleItemMappingChangeEvent e);
	}

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
		 *            {@link ColumnToInputMapping}
		 */
		void setActive(boolean flag);

		/**
		 * Set this mappings column name and update listeners if changed. Needs
		 * to call
		 * {@link #fireMappingColumnChanged(ColumnToModuleItemMappingChangeEvent)}
		 * .
		 * 
		 * @param columnName
		 *            name of the column to set to
		 */
		void setColumnName(String columnName);

		/**
		 * Set this mappings item name and update listeners if changed. Needs to
		 * call
		 * {@link #fireMappingInputChanged(ColumnToModuleItemMappingChangeEvent)}
		 * .
		 * 
		 * @param itemName
		 *            name of the item to set to.
		 */
		void setItemName(String itemName);
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

	/**
	 * Add a {@link ColumnToModuleItemMapping} from this Service if no such
	 * mapping exists yet.
	 * 
	 * @param columnName
	 *            name of the column to map to inputName
	 * @param inputName
	 *            name of the input to map to
	 * @return created or existing {@link ColumnToModuleItemMapping} mapping
	 *         columnName to inputName
	 */
	ColumnToModuleItemMapping addMapping(String columnName, String inputName);

	/**
	 * Remove a {@link ColumnToModuleItemMapping} from this Service.
	 * 
	 * @param mapping
	 *            {@link ColumnToModuleItemMapping} to remove
	 * @return removed {@link ColumnToModuleItemMapping} or null if no such
	 *         mapping was found in this Service
	 */
	ColumnToModuleItemMapping removeMapping(ColumnToModuleItemMapping mapping);
}
