package org.knime.scijava.scripting.node.settings;

/**
 * Enum for column creation mode settings values.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public enum ColumnCreationMode {
	NEW_TABLE("New Table"), APPEND_COLUMNS("Append Columns");

	/**
	 * Constructor.
	 */
	ColumnCreationMode(final String value) {
		m_value = value;
	}

	/**
	 * Get a {@link ColumnCreationMode} enum value whose {@link #toString()}
	 * method returns <code>name</code>
	 *
	 * @param name
	 * @throws IllegalArgumentException
	 *             if name did not match any of the values.
	 */
	public static ColumnCreationMode fromString(final String name) {
		if (NEW_TABLE.toString().equals(name)) {
			return NEW_TABLE;
		}
		if (APPEND_COLUMNS.toString().equals(name)) {
			return APPEND_COLUMNS;
		}

		throw new IllegalArgumentException(
				"ColumnCreationMode enum does not contain a value with name \""
						+ name + "\"");
	}

	@Override
	public String toString() {
		return m_value;
	}

	private final String m_value;
}