package org.knime.scijava.scripting.node.ui.table;

import javax.swing.table.AbstractTableModel;

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
	// private List<ColumnModuleItemMapping> m_mappings;

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
		fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		return m_colModuleMappingService.numMappings();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
		switch (columnIndex) {
		case COLUMN:
			return m_colModuleMappingService.getColumnNameByPosition(rowIndex);
		case ACTIVE:
			return m_colModuleMappingService.isActiveByPosition(rowIndex);
		case INPUT:
			return m_colModuleMappingService.getItemNameByPosition(rowIndex);
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
		m_colModuleMappingService.addMapping(columnName, inputName);
		//
		this.fireTableDataChanged();
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
	public void removeItems(final int... rows) {

		m_colModuleMappingService.removeMappingsByPosition(rows);
		this.fireTableDataChanged();
	}

	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex) {
		// all existing cells can be edited.
		if (columnIndex > 2 || columnIndex < 0) {
			return false;
		}
		if (rowIndex >= m_colModuleMappingService.numMappings()
				|| rowIndex < 0) {
			return false;
		}
		return true;
	}

	@Override
	public void setValueAt(final Object value, final int rowIndex,
			final int columnIndex) {
		switch (columnIndex) {
		case COLUMN:
			m_colModuleMappingService.setColumnNameByPosition(rowIndex,
					(String) value);
			break;
		case ACTIVE:
			m_colModuleMappingService.setActiveByPosition(rowIndex,
					(Boolean) value);
			break;
		case INPUT:
			m_colModuleMappingService.setItemNameByPosition(rowIndex,
					(String) value);
			break;
		default:
			throw new IllegalArgumentException("Column index out of bounds!");
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
