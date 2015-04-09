package org.knime.knip.scripting.ui.table;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

/**
 * A table cell editor which is capable of sharing one Component for editing.
 * 
 * @author Jonathan Hale (University of Konstanz)
 *
 * @param <T>
 */
public class ColumnInputMappingTableCellEditor extends DefaultCellEditor {

	/**
	 * Generated serial version uid
	 */
	private static final long serialVersionUID = 2891092868179415358L;

	public enum EditorMode {
		COLUMN, INPUT
	}

	private EditorMode m_mode;

	private DataTableSpec m_spec;
	private ModuleInfo m_info;

	public ColumnInputMappingTableCellEditor(DataTableSpec spec) {
		super(new JComboBox<DataColumnSpec>(dataTableSpecToArray(spec)));
		m_mode = EditorMode.COLUMN;

		m_spec = spec;
		m_info = null;
	}

	public ColumnInputMappingTableCellEditor(ModuleInfo info) {
		super(new JComboBox<ModuleItem<?>>(moduleToArray(info)));
		m_mode = EditorMode.INPUT;

		m_spec = null;
		m_info = info;
	}
	
	@Override
	public Object getCellEditorValue() {
		Object value = super.getCellEditorValue();
		
		if (value instanceof DataColumnSpec) {
			value = ((DataColumnSpec) value).getName();
		} else if (value instanceof ModuleItem<?>) {
			value = ((ModuleItem<?>) value).getName();
		}
		
		return value;
	}
	
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		
		if (m_mode == EditorMode.COLUMN) {
			value = m_spec.getColumnSpec((String) value);
		} else if (m_mode == EditorMode.INPUT){
			value = m_info.getInput((String) value);
		}
		
		return super.getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	/*
	 * Converts a given DataTableSpec into an array of DataColumnSpecs by
	 * iterating through the DataTableSpec.
	 */
	private static DataColumnSpec[] dataTableSpecToArray(DataTableSpec spec) {
		DataColumnSpec[] specs = new DataColumnSpec[spec.getNumColumns()];

		int i = 0;
		for (DataColumnSpec s : spec) {
			specs[i++] = s;
		}

		return specs;
	}

	/*
	 * Converts a given Iterable<ModuleItem<?>> into an array of ModuleItems by
	 * iterating through the Iterable.
	 */
	private static ModuleItem<?>[] moduleToArray(
			ModuleInfo items) {
		if (items == null) {
			return new ModuleItem<?>[]{};
		}
		
		ArrayList<ModuleItem<?>> list = new ArrayList<ModuleItem<?>>();

		for (ModuleItem<?> item : items.inputs()) {
			list.add(item);
		}

		return list.toArray(new ModuleItem[] {});
	}

}