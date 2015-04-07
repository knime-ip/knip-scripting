package org.knime.knip.scripting.ui.table;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
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

	public static final int COLUMN = 0;
	public static final int ACTIVE = 1;
	public static final int INPUT = 2;
	
	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = 6633031650341577891L;

	@Parameter
	private ColumnToModuleItemMappingService m_cimService;

	private final DataTableSpec m_tableSpec;
	private final ModuleInfo m_moduleInfo;

	// reference to the m_cimServices mappings list. Should only be used for
	// read.
	private List<ColumnToModuleItemMapping> m_mappingsList;

	public ColumnInputMatchingTableModel(DataTableSpec spec, ModuleInfo info,
			Context context) {
		m_tableSpec = spec;
		m_moduleInfo = info;

		context.inject(this);

		// reference should stay valid as long as m_cimService exists
		m_mappingsList = m_cimService.getMappingsList();
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

	public void addItem(String columnName, String inputName) {
		m_cimService.addMapping(columnName, inputName);
		this.fireTableDataChanged();
	}

	public void removeItem(int row) {
		m_cimService.removeMapping(m_mappingsList.get(row));
		this.fireTableDataChanged();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		// all cells can be edited
		return true;
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case COLUMN:
			m_mappingsList.get(rowIndex).setColumnName((String)value);
			break;
		case ACTIVE:
			m_mappingsList.get(rowIndex).setActive((Boolean)value);
			break;
		case INPUT:
			m_mappingsList.get(rowIndex).setItemName((String)value);
			break;
		default:
			// Column index out of bounds
			break;
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == ACTIVE) {
			return Boolean.class;
		} else if (columnIndex == COLUMN || columnIndex == INPUT){
			return String.class;
		} else {
			return Void.class;
		}
	}

	public void loadSettingsForDialog(final Config config)
			throws InvalidSettingsException {
//		m_cimService.loadSettingsFrom(config, m_tableSpec, m_moduleInfo);
	}

	public void saveSettingsForDialog(final Config config) {
//		m_cimService.saveSettingsTo(config);
	}

};