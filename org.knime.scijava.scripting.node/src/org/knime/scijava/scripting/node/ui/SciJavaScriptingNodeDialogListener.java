package org.knime.scijava.scripting.node.ui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeLogger;
import org.knime.scijava.commands.adapter.InputAdapter;
import org.knime.scijava.commands.adapter.InputAdapterService;
import org.knime.scijava.scripting.node.settings.SciJavaScriptingNodeSettings;
import org.knime.scijava.scripting.parameters.ParameterCodeGenerator;
import org.knime.scijava.scripting.parameters.ParameterCodeGeneratorService;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptService;

/**
 * Listener for the ScriptingNode dialog user interface.
 *
 * Requires to be injected by a Scijava {@link Context}.
 *
 * @author Jonathan Hale
 *
 */
public class SciJavaScriptingNodeDialogListener extends AbstractContextual
		implements MouseListener {

	private final SciJavaScriptingCodeEditor m_editor;

	private final SciJavaScriptingNodeSettings m_settings;

	@Parameter
	private Context context;
	@Parameter
	private InputAdapterService m_inputAdapters;
	@Parameter
	private ScriptService m_scriptService;
	@Parameter
	private ParameterCodeGeneratorService m_parameterGenerators;

	private final NodeLogger m_logger;

	private final SciJavaScriptingNodeDialog m_dialog;

	/**
	 * Constructor
	 *
	 * @param logger
	 *            NodeLogger to output messages to
	 */
	public SciJavaScriptingNodeDialogListener(
			final SciJavaScriptingCodeEditor editor, final NodeLogger logger,
			final SciJavaScriptingNodeDialog dialog,
			final SciJavaScriptingNodeSettings settings) {
		m_editor = editor;
		m_logger = logger;
		m_dialog = dialog;
		m_settings = settings;
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		/*
		 * Create a column input mapping on column list double click
		 */
		if (e.getSource() == m_editor.getColumnList()
				&& e.getClickCount() == 2) {
			insertParameterCodeSnippetForColumn(
					m_editor.getColumnList().locationToIndex(e.getPoint()));
		}
	}

	/**
	 * Create a code snippet for a new input in the script which is
	 * automatically mapped to the column in the column selection list at the
	 * given index.
	 *
	 * @param index
	 *            Index of the column to create the code snippet for
	 */
	protected void insertParameterCodeSnippetForColumn(final int index) {
		if (index >= 0) {
			final Object o = m_editor.getColumnList().getModel()
					.getElementAt(index);

			final DataColumnSpec cspec = (DataColumnSpec) o;

			final String columnName = cspec.getName();
			final String memberName = cleanupMemberName(columnName);

			// get the Name of the first createable type
			@SuppressWarnings("rawtypes")
			final Iterator<InputAdapter> itor = m_inputAdapters
					.getMatchingInputAdapters(
							cspec.getType().getPreferredValueClass())
					.iterator();

			final Class<?> type;
			if (itor.hasNext()) {
				type = itor.next().getOutputType();
			} else {
				// no adapter found, error out
				JOptionPane.showMessageDialog(null,
						"The column you selected has a datatype which\n"
								+ "currently cannot be used in Scripts.",
						"No matching adapter", JOptionPane.ERROR_MESSAGE);
				return;
			}

			final ParameterCodeGenerator generator = m_parameterGenerators
					.getGeneratorForLanguage(m_dialog.getCurrentLanguage());

			if (generator == null) {
				m_logger.error(
						"No way of generating input parameter code for language \""
								+ m_settings.getScriptLanguageName() + "\".");
				return;
			}

			// find position for inserting @Parameter declaration
			final String code = m_editor.getCodeEditor().getCode();
			final int pos = generator.getPosition(code);

			final String parameterCode = generator.generateInputParameter(code,
					memberName, type);

			m_editor.getCodeEditor().getEditorPane().insert(parameterCode, pos);
			m_editor.getCodeEditor().updateModel();

		}
	}

	/**
	 * Cleans the columnname from illegal characters to make it usable as a
	 * parameter.
	 *
	 * @param columnName
	 *            the name of the column
	 * @return name of the parameter
	 */
	private String cleanupMemberName(final String columnName) {

		// lowercase first letter
		String name = Character.toLowerCase(columnName.charAt(0))
				+ columnName.substring(1);
		if (Character.isDigit(name.charAt(0))) {
			name = "_" + columnName.substring(0);
		}

		// replace all illegal characters with '_'
		// Characters considered illegal:
		// \ | # [ ] ( ) { } % + ? ~ / & . : ; ,
		name = name.replaceAll("\\\\|\\#|\\[|\\]|\\(|\\)|\\{|\\}|\\%|\\+|"
				+ "\\?|\\~|/|\\&|\\.|\\:|\\;|\\||\\||\\ ", "_");

		// ensure uniqueness
		String chosen = name;
		int i = 0;
		while (m_dialog.getModule().getInfo().getInput(chosen) != null) {
			chosen = name + i;
			++i;
		}
		return chosen;
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		// unsused
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		// unsused
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
		// unsused
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		// unsused
	}

}
