package org.knime.knip.scripting.ui.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

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
public class ColumnInputMappingTableCellEditor implements TableCellEditor,
		ActionListener {

	public enum EditorMode {
		COLUMN, INPUT
	}

	private EditorMode m_mode;

	private JComboBox<? extends Object> m_comboBox;
	private Object m_oldValue = null;

	private ArrayList<CellEditorListener> m_listeners = new ArrayList<CellEditorListener>();

	private DataTableSpec m_spec;
	private ModuleInfo m_info;

	public ColumnInputMappingTableCellEditor(DataTableSpec spec) {
		m_mode = EditorMode.COLUMN;

		m_spec = spec;
		m_info = null;

		m_comboBox = new JComboBox<DataColumnSpec>(dataTableSpecToArray(spec));
		m_comboBox.addActionListener(this);
	}

	public ColumnInputMappingTableCellEditor(ModuleInfo info) {
		m_mode = EditorMode.INPUT;

		m_spec = null;
		m_info = info;

		m_comboBox = new JComboBox<ModuleItem<?>>(
				moduleToArray(info));
		m_comboBox.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (m_comboBox.getSelectedIndex() == -1) {
			cancelCellEditing();
		} else {
			stopCellEditing();
		}
	}

	@Override
	public Object getCellEditorValue() {
		Object item = m_comboBox.getSelectedItem();

		if (item == null) {
			item = m_oldValue;
		} else {
			// try to get the name from the selected item
			if (item instanceof ModuleItem) {
				item = ((ModuleItem<?>) item).getName();
			} else if (item instanceof DataColumnSpec) {
				item = ((DataColumnSpec) item).getName();
			}
		}
		return item;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean stopCellEditing() {
		m_oldValue = null;
		
		ChangeEvent e = new ChangeEvent(this);
		for (CellEditorListener l : m_listeners) {
			l.editingStopped(e);
		}
		
		return true;
	}

	@Override
	public void cancelCellEditing() {
		ChangeEvent e = new ChangeEvent(this);
		for (CellEditorListener l : m_listeners) {
			l.editingCanceled(e);
		}
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
		m_listeners.add(l);
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		m_listeners.remove(l);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		// convert the string into an actual columnspec or moduleitem
		if (value instanceof String) {
			if (m_mode == EditorMode.COLUMN) {
				value = m_spec.getColumnSpec((String) value);
			} else if (m_mode == EditorMode.INPUT) {
				value = m_info.getInput((String) value);
			}
		}
		m_comboBox.setSelectedItem(value);
		m_comboBox.invalidate();
		return m_comboBox;
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