package org.knime.scijava.scripting.nodes.interactive.settings;

/**
 * @author gabriel
 *
 */
public enum ScriptDialogMode {

    CODE_EDIT("Edit Code"), SETTINGS_EDIT("Edit Settings");

    private final String m_name;

    /**
     * Constructor.
     */
    ScriptDialogMode(final String name) {
        m_name = name;
    }

    /**
     * Get a {@link ScriptDialogMode} enum value whose {@link #getName()()}
     * method return <code>name</code>
     *
     * @param name
     *            the name of the option
     * @throws IllegalArgumentException
     *             if name did not match any of the values.
     */
    public static ScriptDialogMode fromString(final String name) {
        if (CODE_EDIT.toString().equals(name)) {
            return CODE_EDIT;
        }
        if (SETTINGS_EDIT.toString().equals(name)) {
            return SETTINGS_EDIT;
        }

        throw new IllegalArgumentException(
                "No ScriptDialogMode with name \"" + name + "\"");
    }

    /**
     * @return the name of the mode
     */
    @Override
    public String toString() {
        return m_name;
    }

}
