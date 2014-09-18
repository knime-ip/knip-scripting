package org.knime.knip.scripting.ui.table;

import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.knip.scripting.matching.ColumnInputMatching;
import org.knime.knip.scripting.matching.ColumnInputMatchingList;
import org.scijava.module.ModuleInfo;

/**
 * TableModel for the ColumnFieldMatchingTable.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public class ColumnInputMatchingTableModel extends AbstractTableModel {

	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = 6633031650341577891L;

	private ColumnInputMatchingList m_data = new ColumnInputMatchingList();

	private final DataTableSpec m_tableSpec;
	private final ModuleInfo m_moduleInfo;
	
	public ColumnInputMatchingTableModel(DataTableSpec spec, ModuleInfo info) {
		m_tableSpec = spec;
		m_moduleInfo = info;
	}

	@Override
	public int getRowCount() {
		return m_data.size();
	}

	@Override
	public int getColumnCount() {
		return ColumnInputMatching.COLUMNS;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return m_data.get(rowIndex).m_values[columnIndex];
	}
	
	@Override
	public String getColumnName(int column) {
		return ColumnInputMatching.COLUMN_NAMES[column];
	}
	
	public void addItem(ColumnInputMatching i) {
		m_data.add(i);
		this.fireTableDataChanged();
	}
	
	public void removeItem(int row) {
		m_data.remove(row);
		
		this.fireTableDataChanged();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		m_data.get(rowIndex).m_values[columnIndex] = aValue;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return ColumnInputMatching.COLUMN_CLASSES[columnIndex];
	}
	
	public void loadSettingsForDialog(final Config config)
			throws InvalidSettingsException {
		m_data.loadSettingsForDialog(config, m_tableSpec, m_moduleInfo);
	}

	public void saveSettingsForDialog(final Config config) {
		m_data.saveSettingsForDialog(config);
	}
};