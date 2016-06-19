package org.knime.scijava.scripting.nodes.interactive.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.scijava.core.TempClassLoader;
import org.knime.scijava.scripting.base.ScriptingGateway;
import org.scijava.Context;

import net.imagej.ui.swing.script.EditorPane;

/**
 * DialogComponent which uses a {@link EditorPane} to edit Java Code.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public class CodeEditorDialogComponent extends DialogComponent
        implements ChangeListener {

    private final EditorPane m_textArea;
    private final SettingsModelString m_codeModel;

    /**
     * Constructor.
     *
     * @param sm
     *            {@link SettingsModelString} of the code to edit
     */
    public CodeEditorDialogComponent(final SettingsModelString sm) {
        super(sm);

        m_codeModel = sm;
        try (final TempClassLoader tempCl = new TempClassLoader(
                ScriptingGateway.get().createUrlClassLoader())) {
            m_textArea = new EditorPane();
        }
        m_textArea.setCodeFoldingEnabled(true);
        m_textArea.setAntiAliasingEnabled(true);
        final JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());
        panel.add(m_textArea.wrappedInScrollbars(),
                new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                        GridBagConstraints.FIRST_LINE_START,
                        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        // make tab key not jump to the next component
        panel.setFocusTraversalKeysEnabled(false);

        SwingUtilities.invokeLater(() -> m_textArea.setVisible(true));
    }

    @Override
    protected void updateComponent() {
        m_textArea.setText(m_codeModel.getStringValue());
    }

    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        updateModel();
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        /* nothing to do */
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_textArea.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        m_textArea.setToolTipText(text);
    }

    /**
     * Get the underlying {@link EditorPane}.
     *
     * @return the text area
     */
    public EditorPane getEditorPane() {
        return m_textArea;
    }

    @Override
    public void stateChanged(final ChangeEvent e) {
        if (e.getSource() == m_textArea) {
            updateModel();
        }
    }

    /**
     * Get the contained code of the editor as String.
     *
     * @return the code in the editor as {@link String}
     */
    public String getCode() {
        return m_textArea.getText();
    }

    /**
     * Update the underlying SettingsModel to contain the text of this editor
     * component.
     */
    public void updateModel() {
        m_codeModel.setStringValue(m_textArea.getText());
    }

    /**
     * Set the Scijava Context to use for the {@link EditorPane}.
     *
     * @param context
     *            The Scijava context
     */
    public void setContext(final Context context) {
        context.inject(m_textArea);
    }
}
