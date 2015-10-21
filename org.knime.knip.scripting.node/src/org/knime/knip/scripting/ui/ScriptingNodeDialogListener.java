package org.knime.knip.scripting.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.JOptionPane;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeLogger;
import org.knime.knip.scijava.commands.adapter.InputAdapter;
import org.knime.knip.scijava.commands.adapter.InputAdapterService;
import org.knime.knip.scripting.node.ScriptingNodeSettings;
import org.knime.knip.scripting.parameters.ParameterCodeGenerator;
import org.knime.knip.scripting.parameters.ParameterCodeGeneratorService;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;

/**
 * Listener for the ScriptingNode dialog user interface.
 *
 * Requires to be injected by a Scijava {@link Context}.
 *
 * @author Jonathan Hale
 *
 */
public class ScriptingNodeDialogListener extends AbstractContextual
		implements ActionListener, MouseListener {

	private final ScriptingNodeDialogPane m_gui;

	private final ScriptingNodeSettings m_settings;

	@Parameter
	private Context context;
	@Parameter
	private InputAdapterService m_inputAdapters;
	@Parameter
	private ScriptService m_scriptService;
	@Parameter
	private ParameterCodeGeneratorService m_parameterGenerators;

	// ActionCommands for removing and adding column/input matchings
	public final static String CMD_ADD = "add";
	public final static String CMD_REM = "rem";

	private final NodeLogger m_logger;

	private NodeLogger getLogger() {
		return m_logger;
	}

	/**
	 * Constructor
	 *
	 * @param gui
	 *            Pane to apply actions to
	 * @param logger
	 *            NodeLogger to output messages to
	 */
	public ScriptingNodeDialogListener(final ScriptingNodeDialogPane gui,
			final NodeLogger logger, final ScriptingNodeSettings settings) {
		m_gui = gui;
		m_logger = logger;
		m_settings = settings;
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		/*
		 * Create a column input mapping on column list double click
		 */
		if (e.getSource() == m_gui.columnList()) {
			// check for doubleclick
			if (e.getClickCount() == 2) {
				insertParameterCodeSnippetForColumn(
						m_gui.columnList().locationToIndex(e.getPoint()));
			}
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
			final Object o = m_gui.columnList().getModel().getElementAt(index);

			if (o instanceof DataColumnSpec) {
				// better safe than sorry, should always be the case,
				// though
				final DataColumnSpec cspec = (DataColumnSpec) o;

				final String columnName = cspec.getName();
				String memberName = cleanupMemberName(columnName);

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

				final ScriptLanguage currentLanguage = m_scriptService
						.getLanguageByName(m_settings.getScriptLanguageName());
				final ParameterCodeGenerator generator = m_parameterGenerators
						.getGeneratorForLanguage(currentLanguage);

				if (generator == null) {
					m_logger.error(
							"No way of generating input parameter code for language \""
									+ m_settings.getScriptLanguageName()
									+ "\".");
				}

				// find position for inserting @Parameter declaration
				final String code = m_gui.codeEditor().getCode();
				final int pos = generator.getPosition(code);

				final String parameterCode = generator
						.generateInputParameter(code, memberName, type);

				m_gui.codeEditor().getEditorPane().insert(parameterCode, pos);
				m_gui.codeEditor().updateModel();

				// add a mapping for the newly created parameter
				m_gui.columnInputMatchingTable().getModel().addItem(columnName,
						memberName);
			}
		}
	}

	/**
	 * Cleans the columnname from illegal characters to make it usable as a
	 * parameter.
	 * 
	 * @param columnName the name of the column
	 * @return name of the parameter 
	 */
	private String cleanupMemberName(final String columnName) {
		String memberName = removeIntegers(
				Character.toLowerCase(columnName.charAt(0))
						+ columnName.substring(1));

		int i = 0;
		String chosen = memberName;
		while (m_gui.columnInputMatchingTable().getModuleInfo()
				.getInput(chosen) != null) {
			chosen = memberName + i;
			++i;
		}
		memberName = chosen;
		return memberName;
	}

	private String removeIntegers(final String memberName) {
		String ret = memberName;
		while (Character.isDigit(ret.charAt(ret.length() - 1))) {
			ret = ret.substring(0, ret.length() - 1);
		}

		return ret;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getActionCommand().equals(CMD_ADD)) {
			addColumnInputMapping();
		} else if (e.getActionCommand().equals(CMD_REM)) {
			removeColumnInputMapping();
		}
	}

	/**
	 * Add a new column to input mapping to the ColumnInputMappingTable.
	 */
	protected void addColumnInputMapping() {
		ModuleItem<?> i = null;
		try {
			i = m_gui.columnInputMatchingTable().getModuleInfo().inputs()
					.iterator().next();
		} catch (final NoSuchElementException exc) {
			getLogger().error("No input found.");
			return;
		}

		final DataColumnSpec cs = m_gui.columnInputMatchingTable()
				.getDataTableSpec().iterator().next();

		if (cs == null) {
			getLogger().error("No column found.");
			return;
		}

		m_gui.columnInputMatchingTable().getModel().addItem(cs.getName(),
				i.getName());
	}

	/**
	 * Remove the currently selected column to input mapping from the
	 * ColumnInputMappingTable.
	 */
	protected void removeColumnInputMapping() {
		final int row = m_gui.columnInputMatchingTable().getSelectedRow();
		m_gui.columnInputMatchingTable().getModel().removeItem(row);
	}
}
