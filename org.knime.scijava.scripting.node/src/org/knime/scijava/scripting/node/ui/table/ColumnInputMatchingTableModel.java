package org.knime.scijava.scripting.node.ui.table;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.knime.scijava.commands.mapping.ColumnModuleItemMapping;
import org.knime.scijava.commands.mapping.ColumnModuleItemMappingService;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.plugin.Parameter;

/**
 * TableModel for the ColumnFieldMatchingTable.
 *
 * TODO: prevent multiple mapping to input.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public class ColumnInputMatchingTableModel extends AbstractTableModel
		implements Contextual {

	/** Constants for table column indices */
	/** "Column" column index */
	public static final int COLUMN = 0;
	/** "Active" column index */
	public static final int ACTIVE = 1;
	/** "INPUT" column index */
	public static final int INPUT = 2;

	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = 6633031650341577891L;

	@Parameter
	private Context m_context;
	@Parameter
	private ColumnModuleItemMappingService m_colModuleMappingService;

	// can only be read from  
	private List<ColumnModuleItemMapping> m_mappings;

	/**
	 * Constructor.
	 */
	public ColumnInputMatchingTableModel() {
		updateModel();
	}

	/**
	 * Update the table model to the contents of the column input mapping
	 * service.
	 */
	public void updateModel() {
		if (m_colModuleMappingService == null) {
			// Needs to be set via context first.
			return;
		}

		// reference should stay valid as long as m_cimService exists
		m_mappings = m_colModuleMappingService.getMappingsList();

		fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		if (m_mappings == null) {
			updateModel();
		}
		return m_mappings.size();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
		switch (columnIndex) {
		case COLUMN:
			return m_mappings.get(rowIndex).getColumnName();
		case ACTIVE:
			return m_mappings.get(rowIndex).isActive();
		case INPUT:
			return m_mappings.get(rowIndex).getItemName();
		default:
			return new String("<Column Index Out Of Bounds!>");
		}
	}

	@Override
	public String getColumnName(final int column) {
		switch (column) {
		case COLUMN:
			return "Column";
		case ACTIVE:
			return "Active";
		case INPUT:
			return "Input";
		default:
			return "<Column Index Out Of Bounds!>";
		}
	}

	/**
	 * Add an item from the model and the underlying
	 * {@link ColumnModuleItemMappingService}.
	 *
	 * @param columnName
	 *            column name to map
	 * @param inputName
	 *            input name to map to
	 */
	public void addItem(final String columnName, final String inputName) {
		final int row = m_mappings.size();
		m_colModuleMappingService.addMapping(columnName, inputName);

		if (row != m_mappings.size()) {
			// adding the mapping changed the size of the table, therefore it
			// was added to the end of the list.
			this.fireTableRowsInserted(row, row);
		} else {
			// we cannot simply determine which row changed.
			// TODO: prevent multiple mapping to input.
			this.fireTableDataChanged();
		}
	}

	/**
	 * Remove items from the model and the underlying
	 * {@link ColumnModuleItemMappingService}.
	 *
	 * Please make sure that all cell editors cancel edit before calling this
	 * method.
	 *
	 * @param row
	 *            row index to remove
	 */
	public void removeItems(final int...rows){
		List<ColumnModuleItemMapping> mappings = new ArrayList<>();
		for (int row : rows) {
			mappings.add(m_mappings.get(row));
		}
		for (ColumnModuleItemMapping mapping : mappings) {
			m_colModuleMappingService.removeMapping(mapping);
		}
		this.fireTableDataChanged();
	}
	

	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex) {
		if (columnIndex > 2 || columnIndex < 0) {
			return false;
		}
		if (rowIndex >= m_mappings.size() || rowIndex < 0) {
			return false;
		}
		// all existing cells can be edited
		return true;
	}

	@Override
	public void setValueAt(final Object value, final int rowIndex,
			final int columnIndex) {
		switch (columnIndex) {
		case COLUMN:
			m_mappings.get(rowIndex).setColumnName((String) value);
			break;
		case ACTIVE:
			m_mappings.get(rowIndex).setActive((Boolean) value);
			break;
		case INPUT:
			m_mappings.get(rowIndex).setItemName((String) value);
			break;
		default:
			// Column index out of bounds
			break;
		}

		this.fireTableCellUpdated(rowIndex, columnIndex);
	}

	@Override
	public Class<?> getColumnClass(final int columnIndex) {
		if (columnIndex == ACTIVE) {
			return Boolean.class;
		} else if (columnIndex == COLUMN || columnIndex == INPUT) {
			return Object.class;
		} else {
			return Void.class;
		}
	}

	// --- Contextual methods ---

	@Override
	public void setContext(final Context context) {
		context.inject(this);
	}

	@Override
	public Context context() {
		return m_context;
	}

	@Override
	public Context getContext() {
		return m_context;
	}

}