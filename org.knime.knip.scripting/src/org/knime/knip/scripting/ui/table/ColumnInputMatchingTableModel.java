package org.knime.knip.scripting.ui.table;

import javax.swing.table.AbstractTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.knip.scripting.matching.ColumnInputMatchingsService;
import org.scijava.Context;
import org.scijava.module.ModuleInfo;
import org.scijava.plugin.Parameter;

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

	@Parameter
	private ColumnInputMatchingsService m_cimService;

	private final DataTableSpec m_tableSpec;
	private final ModuleInfo m_moduleInfo;
	
	public ColumnInputMatchingTableModel(DataTableSpec spec, ModuleInfo info, Context context) {
		m_tableSpec = spec;
		m_moduleInfo = info;
		
		context.inject(this);
	}

	@Override
	public int getRowCount() {
		return m_cimService.getMatchingsCount();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch(columnIndex) {
			case 0: return m_cimService.getColumnInputMatchings().get(rowIndex).getFirst();
			case 1: return new Boolean(true);
			case 2: return m_cimService.getColumnInputMatchings().get(rowIndex).getSecond();
			default:
				return new String("column indx oob");
		}
	}
	
	@Override
	public String getColumnName(int column) {
		switch(column){
		case 0: return "Column";
		case 1: return "Active";
		case 2: return "Input";
		default:
			return "column indx oob";
		}
	}
	
	public void addItem(String i, String c) {
		m_cimService.addColumnInputMatching(i, c);
		this.fireTableDataChanged();
	}
	
	public void removeItem(int row) {
		m_cimService.removeColumnInputMatching(row);
		
		this.fireTableDataChanged();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		String input = m_cimService.getColumnInputMatchings().get(rowIndex).getFirst();
		
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 1) {
			return Boolean.class;
		} else {
			return String.class;
		}
	}
	
	public void loadSettingsForDialog(final Config config)
			throws InvalidSettingsException {
		m_cimService.loadSettingsFrom(config, m_tableSpec, m_moduleInfo);
	}

	public void saveSettingsForDialog(final Config config) {
		m_cimService.saveSettingsTo(config);
	}
	
};