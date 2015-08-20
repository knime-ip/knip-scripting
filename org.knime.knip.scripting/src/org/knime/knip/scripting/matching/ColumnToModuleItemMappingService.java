package org.knime.knip.scripting.matching;

import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
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

		private final String oldValue;

		/**
		 * Constructor
		 *
		 * @param source
		 *            the changed {@link ColumnToModuleItemMapping}
		 */
		public ColumnToModuleItemMappingChangeEvent(
				final ColumnToModuleItemMapping source, final String oldValue) {
			super(source);

			this.oldValue = oldValue;
		}

		/**
		 * Get the changed {@link ColumnToModuleItemMapping}.
		 *
		 * @return the changed mapping
		 */
		public ColumnToModuleItemMapping getSourceMapping() {
			return (ColumnToModuleItemMapping) source;
		}

		/**
		 * Get the previous value of the column/input name.
		 *
		 * @return the previous column or input name.
		 */
		public String getPreviousValue() {
			return oldValue;
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
		void onMappingItemChanged(ColumnToModuleItemMappingChangeEvent e);
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
		 * @param oldValue
		 *            value which has been overwritten
		 */
		void fireMappingColumnChanged(String oldValue);

		/**
		 * Call
		 * {@link ColumnToModuleItemMappingChangeListener#onMappingItemChanged(ColumnToModuleItemMappingChangeEvent)}
		 * on all listeners.
		 *
		 * @param oldValue
		 *            value which has been overwritten
		 */
		void fireMappingItemChanged(String oldValue);
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

	/**
	 * Remove all {@link ColumnToModuleItemMapping}s from this Service.
	 */
	void clear();
}
