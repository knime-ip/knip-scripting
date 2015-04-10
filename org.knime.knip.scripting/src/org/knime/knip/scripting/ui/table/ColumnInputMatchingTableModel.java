package org.knime.knip.scripting.ui.table;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService.ColumnToModuleItemMapping;
import org.scijava.Context;
import org.scijava.module.ModuleInfo;
import org.scijava.plugin.Parameter;

/**
 * TableModel for the ColumnFieldMatchingTable.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public class ColumnInputMatchingTableModel extends AbstractTableModel {

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
	private ColumnToModuleItemMappingService m_cimService;

	// reference to the m_cimServices mappings list. Should only be used for
	// read.
	private List<ColumnToModuleItemMapping> m_mappingsList;

	public ColumnInputMatchingTableModel(DataTableSpec spec, ModuleInfo info,
			Context context) {
		context.inject(this);
		updateModel(spec, info);
	}

	public void updateModel(DataTableSpec spec, ModuleInfo info) {
		// reference should stay valid as long as m_cimService exists
		m_mappingsList = m_cimService.getMappingsList();

		fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		return m_mappingsList.size();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case COLUMN:
			return m_mappingsList.get(rowIndex).getColumnName();
		case ACTIVE:
			return m_mappingsList.get(rowIndex).isActive();
		case INPUT:
			return m_mappingsList.get(rowIndex).getItemName();
		default:
			return new String("<Column Index Out Of Bounds!>");
		}
	}

	@Override
	public String getColumnName(int column) {
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
	 * {@link ColumnToModuleItemMappingService}.
	 * 
	 * @param columnName
	 *            column name to map
	 * @param inputName
	 *            input name to map to
	 */
	public void addItem(String columnName, String inputName) {
		m_cimService.addMapping(columnName, inputName);
		this.fireTableDataChanged();
	}

	/**
	 * Remove an item from the model and the underlying
	 * {@link ColumnToModuleItemMappingService}.
	 * 
	 * Please make sure that all cell editors cancel edit before calling this
	 * method.
	 * 
	 * @param row
	 *            row index to remove
	 */
	public void removeItem(int row) {
		m_cimService.removeMapping(m_mappingsList.get(row));
		this.fireTableRowsDeleted(row, row);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex > 2 || columnIndex < 0) {
			return false;
		}
		if (rowIndex >= m_mappingsList.size() || rowIndex < 0) {
			return false;
		}
		// all existing cells can be edited
		return true;
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case COLUMN:
			m_mappingsList.get(rowIndex).setColumnName((String) value);
			break;
		case ACTIVE:
			m_mappingsList.get(rowIndex).setActive((Boolean) value);
			break;
		case INPUT:
			m_mappingsList.get(rowIndex).setItemName((String) value);
			break;
		default:
			// Column index out of bounds
			break;
		}

		this.fireTableCellUpdated(rowIndex, columnIndex);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == ACTIVE) {
			return Boolean.class;
		} else if (columnIndex == COLUMN || columnIndex == INPUT) {
			return Object.class;
		} else {
			return Void.class;
		}
	}

};