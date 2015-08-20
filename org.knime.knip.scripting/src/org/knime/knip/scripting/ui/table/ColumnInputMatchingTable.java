package org.knime.knip.scripting.ui.table;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import org.knime.core.data.DataTableSpec;
import org.scijava.Context;
import org.scijava.module.ModuleInfo;

/**
 * A JTable for matching Columns to ModuleInputs.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public class ColumnInputMatchingTable extends JTable {

	/**
	 */
	private static final long serialVersionUID = -5405520683074706886L;

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
	public ColumnInputMatchingTable(final DataTableSpec spec,
			final ModuleInfo info, final Context context) {
		super(new ColumnInputMatchingTableModel(context));

		context.inject(this);

		m_spec = spec;
		m_info = info;

		updateModel(spec, info);
	}

	/**
	 * Set the cell editors as the created Components.
	 */
	private void setCellEditors() {
		final TableColumnModel model = getColumnModel();

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
	public void updateModel(final DataTableSpec spec, final ModuleInfo info) {
		getModel().updateModel();

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
