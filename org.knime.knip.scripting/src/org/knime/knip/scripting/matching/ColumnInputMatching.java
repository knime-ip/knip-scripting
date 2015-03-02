package org.knime.knip.scripting.matching;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

/**
 * A entry or row in a ColumnFieldMatchingTable.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public class ColumnInputMatching {
	/** Number of columns per entry */
	public final static int COLUMNS = 3;

	/** Index of the "active" column */
	public final static int COL_ACTIVE = 0;
	/** Index of the "column" column */
	public final static int COL_COLUMN = 1;
	/** Index of the "input" column */
	public final static int COL_INPUT = 2;

	/**
	 * Values of this ColumnFieldMatchingTableEntry.
	 */
	public Object[] m_values;

	/**
	 * Column names
	 */
	public final static String[] COLUMN_NAMES = { "active", "column", "input" };

	public final static Class<?>[] COLUMN_CLASSES = { Boolean.class,
			String.class, ModuleItem.class };

	/**
	 * Constructor
	 * 
	 * @param active
	 *            whether this row is active/should be used.
	 * @param column
	 *            which column to match to the input.
	 * @param input
	 *            the input the column is matched to.
	 */
	public ColumnInputMatching(Boolean active, String column,
			ModuleItem<?> input) {
		m_values = new Object[COLUMNS];

		m_values[COL_ACTIVE] = active;
		m_values[COL_COLUMN] = column;
		m_values[COL_INPUT] = input;
	}

	/**
	 * Set this Column input matching to be (in-)active.
	 * @param b
	 */
	public void setActive(Boolean b) {
		m_values[COL_ACTIVE] = b;
	}

	/**
	 * Set the name of the column which the input is matched to.
	 * @param c
	 */
	public void setColumn(String c) {
		m_values[COL_COLUMN] = c;
	}

	/**
	 * Set input which is matched to the column.
	 * @param i
	 */
	public void setInput(ModuleItem<?> i) {
		m_values[COL_INPUT] = i;
	}

	/**
	 * @return whether this matching is active.
	 */
	public Boolean getActive() {
		return (Boolean) m_values[COL_ACTIVE];
	}

	/**
	 * @return the column the input is matched to.
	 */
	public String getColumnName() {
		if (m_values[COL_COLUMN] instanceof DataColumnSpec) {
			return ((DataColumnSpec) m_values[COL_COLUMN]).getName();
		}
		return (String) m_values[COL_COLUMN];
	}
	
	/**
	 * Get DataCell for row and DataTableSpec.
	 * @param r DataRow to find the Cell in.
	 * @param spec DataTable spec to help with finding column index.
	 * @return the DataCell or null, if there was no such column in spec.
	 */
	public DataCell getDataCell(DataRow r, DataTableSpec spec) {
		int index = spec.findColumnIndex(((DataColumnSpec) m_values[COL_COLUMN]).getName());
		return (index >= 0 && index < r.getNumCells()) ? r.getCell(index) : null;
	}

	/**
	 * @return the input which is matched to the column.
	 */
	public ModuleItem<?> getInput() {
		return (ModuleItem<?>) m_values[COL_INPUT];
	}

	public static final String CFG_ACTIVE = "Active";
	public static final String CFG_COLUMN = "Column";
	public static final String CFG_INPUT = "Input";

	public void loadSettingsForDialog(final Config config,
			DataTableSpec tableSpec, ModuleInfo moduleInfo)
			throws InvalidSettingsException {
		setActive(config.getBoolean(CFG_ACTIVE));
		setColumn(config.getString(CFG_COLUMN));
		setInput(moduleInfo.getInput(config.getString(CFG_INPUT)));
	}

	public void saveSettingsForDialog(final Config config) {
		config.addBoolean(CFG_ACTIVE, getActive());
		config.addString(CFG_COLUMN, getColumnName());
		config.addString(CFG_INPUT, getInput().getName());
	}
}