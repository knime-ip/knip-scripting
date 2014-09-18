package org.knime.knip.scripting.matching;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
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
			DataColumnSpec.class, ModuleItem.class };

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
	public ColumnInputMatching(Boolean active, DataColumnSpec column,
			ModuleItem<?> input) {
		m_values = new Object[COLUMNS];

		m_values[COL_ACTIVE] = active;
		m_values[COL_COLUMN] = column;
		m_values[COL_INPUT] = input;
	}

	void setActive(Boolean b) {
		m_values[COL_ACTIVE] = b;
	}

	void setColumn(DataColumnSpec c) {
		m_values[COL_COLUMN] = c;
	}

	void setInput(ModuleItem<?> i) {
		m_values[COL_INPUT] = i;
	}

	Boolean getActive() {
		return (Boolean) m_values[COL_ACTIVE];
	}

	DataColumnSpec getColumn() {
		return (DataColumnSpec) m_values[COL_COLUMN];
	}

	ModuleItem<?> getInput() {
		return (ModuleItem<?>) m_values[COL_INPUT];
	}

	public static final String CFG_ACTIVE = "Active";
	public static final String CFG_COLUMN = "Column";
	public static final String CFG_INPUT = "Input";

	public void loadSettingsForDialog(final Config config,
			DataTableSpec tableSpec, ModuleInfo moduleInfo)
			throws InvalidSettingsException {
		setActive(config.getBoolean(CFG_ACTIVE));
		setColumn(tableSpec.getColumnSpec(config.getString(CFG_COLUMN)));
		setInput(moduleInfo.getInput(config.getString(CFG_INPUT)));
	}

	public void saveSettingsForDialog(final Config config) {
		config.addBoolean(CFG_ACTIVE, getActive());
		config.addString(CFG_COLUMN, getColumn().getName());
		config.addString(CFG_INPUT, getInput().getName());
	}
}