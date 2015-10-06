package org.knime.knip.scripting.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.swing.JOptionPane;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeLogger;
import org.knime.knip.scijava.commands.adapter.InputAdapter;
import org.knime.knip.scijava.commands.adapter.InputAdapterService;
import org.scijava.Context;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Parameter;

/**
 * Listener for the ScriptingNode dialog user interface.
 * 
 * Requires to be injected by a Scijava {@link Context}.
 * 
 * @author Jonathan Hale
 *
 */
public class ScriptingNodeDialogListener
		implements ActionListener, MouseListener {

	private final ScriptingNodeDialogPane m_gui;

	@Parameter
	Context context;
	@Parameter
	private InputAdapterService m_inputAdapters;

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
	public ScriptingNodeDialogListener(ScriptingNodeDialogPane gui,
			NodeLogger logger) {
		m_gui = gui;
		m_logger = logger;
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
				String memberName = Character.toLowerCase(columnName.charAt(0))
						+ columnName.substring(1);

				int i = 0;
				String chosen = memberName;
				while (m_gui.columnInputMatchingTable().getModuleInfo()
						.getInput(chosen) != null) {
					chosen = memberName + i;
					++i;
				}
				memberName = chosen;

				// get the Name of the first createable type
				@SuppressWarnings("rawtypes")
				final Iterator<InputAdapter> itor = m_inputAdapters
						.getMatchingInputAdapters(cspec.getType()).iterator();
				String typeName;
				if (itor.hasNext()) {
					typeName = itor.next().getType().getName();
				} else {
					// no adapter found, error out
					JOptionPane.showMessageDialog(null,
							"The column you selected has a datatype which\n"
									+ "currently cannot be used in Scripts.",
							"No matching adapter", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// find position for inserting @Parameter declaration
				final int pos = m_gui.codeEditor().getCode().indexOf('{') + 1;
				final String parameterCode = "\n\n\t@Parameter(type = ItemIO.INPUT)\n\tprivate "
						+ typeName + " " + memberName + ";\n";

				m_gui.codeEditor().getEditorPane().insert(parameterCode, pos);
				m_gui.codeEditor().updateModel();

				// add a mapping for the newly created parameter
				m_gui.columnInputMatchingTable().getModel().addItem(columnName,
						memberName);
			}
		}
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
