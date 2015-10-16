package org.knime.knip.scripting.ui.table;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import org.knime.core.data.DataTableSpec;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.module.ModuleInfo;

/**
 * A JTable for matching Columns to ModuleInputs.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public class ColumnInputMatchingTable extends JTable implements Contextual {

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
			final ModuleInfo info) {
		super(new ColumnInputMatchingTableModel());

		m_spec = spec;
		m_info = info;

		updateModel(spec, info);
	}

	/**
	 * Set the cell editors as the created Components.
	 */
	private void setCellEditors() {
		final TableColumnModel model = getColumnModel();

		model.getColumn(ColumnInputMatchingTableModel.COLUMN)
				.setCellEditor(new ColumnInputMappingTableCellEditor(m_spec));

		model.getColumn(ColumnInputMatchingTableModel.ACTIVE)
				.setCellEditor(new DefaultCellEditor(new JCheckBox()));

		model.getColumn(ColumnInputMatchingTableModel.INPUT)
				.setCellEditor(new ColumnInputMappingTableCellEditor(m_info));

	}

	/**
	 * Update this Tables model to represent a given {@link DataTableSpec} and
	 * {@link ModuleInfo}.
	 *
	 * @param spec
	 *            the DataTableSpec.
	 * @param info
	 *            the module info
	 */
	public void updateModel(final DataTableSpec spec, final ModuleInfo info) {
		getModel().updateModel();

		m_spec = spec;
		m_info = info;

		setCellEditors();
	}

	/**
	 * Update this Tables model to represent a given {@link ModuleInfo}, keeping
	 * the current {@link DataTableSpec}.
	 *
	 * @param info
	 *            the module info
	 */
	public void updateModel(final ModuleInfo info) {
		getModel().updateModel();

		m_info = info;

		setCellEditors();
	}

	/**
	 * @return currently set DataTableSpec info.
	 * @see #updateModel(DataTableSpec, ModuleInfo)
	 */
	public DataTableSpec getDataTableSpec() {
		return m_spec;
	}

	/**
	 * @return currently set module info.
	 * @see #updateModel(DataTableSpec, ModuleInfo)
	 */
	public ModuleInfo getModuleInfo() {
		return m_info;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ColumnInputMatchingTableModel getModel() {
		return (ColumnInputMatchingTableModel) super.getModel();
	}

	// --- Contextual methods ---
	// For convenience only. CIMTable does not keep a context itself.

	@Override
	public void setContext(final Context context) {
		getModel().setContext(context);
	}

	@Override
	public Context context() {
		return getModel().context();
	}

	@Override
	public Context getContext() {
		return getModel().getContext();
	}
}
