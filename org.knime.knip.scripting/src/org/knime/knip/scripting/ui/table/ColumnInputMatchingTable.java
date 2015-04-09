package org.knime.knip.scripting.ui.table;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.scijava.Context;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

/**
 * A JTable for matching Columns to ModuleInputs.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public class ColumnInputMatchingTable extends JTable {

	/**
	 */
	private static final long serialVersionUID = -5405520683074706886L;

	private JComboBox<DataColumnSpec> columnComboBox;
	private JComboBox<ModuleItem<?>> inputsComboBox;

	private DataTableSpec m_spec;
	private ModuleInfo m_info;

	/**
	 * Constructor
	 * 
	 * @param spec
	 *            data table spec to get additional information for column names
	 *            from
	 * @param info
	 *            module info to get additional information for module input
	 *            names from
	 * @param context
	 *            ScijavaContext to get Services from
	 */
	public ColumnInputMatchingTable(DataTableSpec spec, ModuleInfo info,
			Context context) {
		super(new ColumnInputMatchingTableModel(spec, info, context));

		context.inject(this);
		
		m_spec = spec;
		m_info = info;

		updateModel(spec, info);
	}

	/**
	 * Set the cell editors as the created Components.
	 * 
	 * @see #createComponents()
	 */
	private void setCellEditors() {
		TableColumnModel model = getColumnModel();
		
		model.getColumn(ColumnInputMatchingTableModel.COLUMN).setCellEditor(
				new ColumnInputMappingTableCellEditor(m_spec));
		
		model.getColumn(ColumnInputMatchingTableModel.ACTIVE).setCellEditor(
				new DefaultCellEditor(new JCheckBox()));
		
		model.getColumn(ColumnInputMatchingTableModel.INPUT).setCellEditor(
				new ColumnInputMappingTableCellEditor(m_info));
		
	}

	/**
	 * Update this Tables model to represent a given DataTableSpec and
	 * ModuleInfo.
	 * 
	 * @param spec
	 *            the DataTableSpec.
	 */
	public void updateModel(DataTableSpec spec, ModuleInfo info) {
		((ColumnInputMatchingTableModel) getModel()).updateModel(spec, info);

		m_spec = spec;
		m_info = info;
		
		setCellEditors();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ColumnInputMatchingTableModel getModel() {
		return (ColumnInputMatchingTableModel) super.getModel();
	}
}
