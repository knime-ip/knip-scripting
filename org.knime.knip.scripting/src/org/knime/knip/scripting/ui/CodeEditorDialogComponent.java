package org.knime.knip.scripting.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imagej.ui.swing.script.EditorPane;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * DialogComponent which uses a {@link EditorPane} to edit Java Code.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public class CodeEditorDialogComponent extends DialogComponent implements
		ChangeListener {

	private final EditorPane m_textArea;
	private final SettingsModelString m_codeModel;

	// JavaCompletionProvider m_provider = new JavaCompletionProvider();

	/**
	 * Constructor.
	 * 
	 * @param sm
	 *            {@link SettingsModelString} of the code to edit
	 */
	public CodeEditorDialogComponent(SettingsModelString sm) {
		super(sm);

		m_codeModel = sm;

		m_textArea = new EditorPane();

		m_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		m_textArea.setCodeFoldingEnabled(true);
		m_textArea.setAntiAliasingEnabled(true);

		JPanel panel = getComponentPanel();
		panel.setLayout(new GridBagLayout());
		panel.add(m_textArea.wrappedInScrollbars(), new GridBagConstraints(0,
				0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		// make tab key not jump to the next component
		panel.setFocusTraversalKeysEnabled(false);

		// m_provider.updateCompletions(m_codeModel.getStringValue());
		// new AutoCompletion(m_provider).install(m_textArea);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				m_textArea.setVisible(true);
			}
		});
	}

	@Override
	protected void updateComponent() {
		m_textArea.setText(m_codeModel.getStringValue());
	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		updateModel();
	}

	@Override
	protected void checkConfigurabilityBeforeLoad(PortObjectSpec[] specs)
			throws NotConfigurableException {
	}

	@Override
	protected void setEnabledComponents(boolean enabled) {
		m_textArea.setEnabled(enabled);
	}

	@Override
	public void setToolTipText(String text) {
		m_textArea.setToolTipText(text);
	}

	/**
	 * Get the underlying {@link RSyntaxTextArea}.
	 * 
	 * @return the text area
	 */
	public RSyntaxTextArea getTextArea() {
		return m_textArea;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == m_textArea) {
			updateModel();
			// m_provider.updateCompletions(m_codeModel.getStringValue());
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
}