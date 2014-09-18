package org.knime.knip.scripting.ui.table;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.ColumnComboBoxRenderer;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

/**
 * A JTable for matching Columns to ModuleInputs.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public class ColumnInputMatchingTable extends JTable {

	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = -5405520683074706886L;

	private JComboBox<DataColumnSpec> columnComboBox;
	private JComboBox<ModuleItem<?>> inputsComboBox;

	public ColumnInputMatchingTable(DataTableSpec spec, ModuleInfo info) {
		super(new ColumnInputMatchingTableModel(spec, info));
		
		createComponents(spec, info);	
		setCellEditors();
	}
	
	private void createComponents(DataTableSpec spec, ModuleInfo info) {
		columnComboBox = new JComboBox<DataColumnSpec>(dataTableSpecToArray(spec));
		
		ModuleItem<?>[] data = new ModuleItem<?>[]{};
		if (info != null) {
			data = moduleInputsToArray(info.inputs());
		}
		inputsComboBox = new JComboBox<ModuleItem<?>>(data);
	}

	private void setCellEditors() {
		getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(columnComboBox));
		getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(inputsComboBox));
	}

	/**
	 * Update this Tables model to represent a given DataTableSpec.
	 * 
	 * @param spec
	 *            the DataTableSpec.
	 */
	public void updateModel(DataTableSpec spec, ModuleInfo info) {
		this.setModel(new ColumnInputMatchingTableModel(spec, info));
		
		createComponents(spec, info);
		setCellEditors();
	}
	
	@Override
	public ColumnInputMatchingTableModel getModel() {
		return (ColumnInputMatchingTableModel) super.getModel();
	}

	/*
	 * Converts a given DataTableSpec into an array of DataColumnSpecs by iterating
	 * through the DataTableSpec.
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
	 * Converts a given Iterable<ModuleItem<?>> into an array of ModuleItems by iterating
	 * through the Iterable.
	 */
	private static ModuleItem<?>[] moduleInputsToArray(Iterable<ModuleItem<?>> items) {
		ArrayList<ModuleItem<?>> list = new ArrayList<ModuleItem<?>>();
		
		for (ModuleItem<?> item : items) {
			list.add(item);
		}

		return list.toArray(new ModuleItem[]{});
	}

	/**
	 * TableCellRenderer for Tables containing a column with DataColumnSpecs.
	 * 
	 * @author Jonathan Hale (University of Konstanz)
	 *
	 */
	public class ColumnFieldMatchingTableCellRenderer extends DefaultTableCellRenderer {

		/**
		 * Generated serialVersionUID
		 */
		private static final long serialVersionUID = -5599066681704425666L;
		private DataTableSpec m_spec;

		public ColumnFieldMatchingTableCellRenderer(DataTableSpec spec) {
			super();
			m_spec = spec;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			if (value instanceof DataColumnSpec) {
				JComboBox<DataColumnSpec> cb = new JComboBox<DataColumnSpec>(
						dataTableSpecToArray(m_spec));

				cb.setRenderer(new ColumnComboBoxRenderer());
				cb.setSelectedItem(value);

				return cb;
			} else {
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}
	}
}
