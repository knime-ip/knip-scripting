package org.knime.knip.scripting.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

public class CodeEditorDialogComponent extends DialogComponent implements
		ChangeListener {
	RSyntaxTextArea m_textArea;
	SettingsModelString m_codeModel;

	// JavaCompletionProvider m_provider = new JavaCompletionProvider();

	public CodeEditorDialogComponent(SettingsModelString sm) {
		super(sm);

		m_codeModel = sm;

		m_textArea = new RSyntaxTextArea(20, 60);

		m_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		m_textArea.setCodeFoldingEnabled(true);
		m_textArea.setAntiAliasingEnabled(true);

		RTextScrollPane sp = new RTextScrollPane(m_textArea);

		JPanel panel = getComponentPanel();
		panel.setLayout(new GridBagLayout());
		panel.add(sp, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
				new Insets(0, 0, 0, 0), 0, 0));
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
		m_codeModel.setStringValue(m_textArea.getText());
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

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == m_textArea) {
			m_codeModel.setStringValue(m_textArea.getText());
			// m_provider.updateCompletions(m_codeModel.getStringValue());
		}
	}
}