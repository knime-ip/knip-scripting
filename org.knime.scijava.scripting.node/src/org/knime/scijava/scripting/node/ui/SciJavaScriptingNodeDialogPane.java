package org.knime.scijava.scripting.node.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnSelectionList;
import org.knime.scijava.scripting.node.settings.SciJavaScriptingNodeSettings;
import org.knime.scijava.scripting.node.ui.table.ColumnInputMatchingTable;
import org.scijava.Context;
import org.scijava.Contextual;

/**
 * User inteface components of the ScriptingNode dialog.
 *
 * @author Jonathan Hale
 *
 */
public class SciJavaScriptingNodeDialogPane implements Contextual {

	/* containers */
	private final ArrayList<DialogComponent> m_dialogComponents = new ArrayList<DialogComponent>();

	/*
	 * options for easy debugging: WARNING: This is for debugging the UI layout
	 * ONLY. It causes loads of bugs related to components not being updated
	 * after pane is rebuilt.
	 */
	public final static boolean DEBUG_UI = false;

	/* UI Components */

	// Table containing column/input matching.
	private ColumnInputMatchingTable m_columnMatchingTable;

	// Labels
	private final JLabel LBL_HEADER = new JLabel("Script Editor");
	private final JLabel LBL_LANG = new JLabel("Language:");
	private final JLabel LBL_COLUMN = new JLabel("Column:");
	private final JLabel LBL_CIM = new JLabel("Column/Input Matchings:");

	// Buttons
	private final JButton m_addBtn = new JButton("+");
	private final JButton m_remBtn = new JButton("-");

	// Code Editor
	private CodeEditorDialogComponent m_codeEditor;

	// tab panels
	private final JPanel m_editorPanel = new JPanel(new GridBagLayout());

	// Language selection Combobox
	private final JComboBox<String> m_langSelection = new JComboBox<String>(
			new String[] { "Java" });

	// column selection list for generating inputs with column matchings
	private final ColumnSelectionList m_columnList = new ColumnSelectionList();

	private SettingsModelString m_codeModel;

	/**
	 * Constructor
	 *
	 * @param logger
	 *            NodeLogger to output messages to
	 * @param codeModel
	 *            the SettingsModel storing the code
	 */
	public SciJavaScriptingNodeDialogPane(final NodeLogger logger,
			final SettingsModelString codeModel) {
		/* one time setup of some components */
		LBL_COLUMN.setBorder(UIManager.getBorder("TableHeader.cellBorder"));

		m_codeModel = codeModel;

		buildDialog();
	}

	/* utility functions for creating GridBagConstraints */
	private static final GridBagConstraints createGBC(final int x, final int y,
			final int w, final int h, final int anchor, final int fill,
			final Insets insets) {
		float weightx = 1.0f;
		float weighty = 1.0f;

		if (fill == GridBagConstraints.NONE) {
			weightx = weighty = 0.0f;
		} else if (fill == GridBagConstraints.HORIZONTAL) {
			weighty = 0.0f;
		} else if (fill == GridBagConstraints.VERTICAL) {
			weightx = 0.0f;
		}

		return new GridBagConstraints(x, y, w, h, weightx, weighty, anchor,
				fill, insets, 0, 0);
	}

	private static final GridBagConstraints createGBC(final int x, final int y,
			final int w, final int h, final int anchor, final int fill,
			final double weightx, final double weighty) {
		return new GridBagConstraints(x, y, w, h, weightx, weighty, anchor,
				fill, new Insets(0, 0, 0, 0), 0, 0);
	}

	private static final GridBagConstraints createGBC(final int x, final int y,
			final int w, final int h, final int anchor, final int fill,
			final double weightx, final double weighty, final Insets insets) {
		return new GridBagConstraints(x, y, w, h, weightx, weighty, anchor,
				fill, insets, 0, 0);
	}

	/*
	 * Apply visible colors to some of the components. While debugging the ui
	 * layout this can come in handy, since it allows to see whether a component
	 * is filling out a cell of a GridBagLayout.
	 */
	private void applyDebugColors() {
		final Color PINKISH = new Color(100, 150, 100);
		final Color PEACHY = new Color(255, 200, 128);

		LBL_HEADER.setBackground(PINKISH);
		LBL_HEADER.setOpaque(true);
		LBL_LANG.setBackground(Color.cyan);
		LBL_LANG.setOpaque(true);
		LBL_CIM.setBackground(PEACHY);
		LBL_CIM.setOpaque(true);
	}

	/*
	 * Utility function to add a DialogComponent to a panel and the
	 * m_dialogComponents container for loading and saving.
	 */
	private void addDialogComponent(final JPanel panel,
			final DialogComponent comp, final Object constraints) {
		panel.add(comp.getComponentPanel(), constraints);
		m_dialogComponents.add(comp);
	}

	/*
	 * Add dialog components to tab panels.
	 */
	private void buildDialog() {
		final int FILL_BOTH = GridBagConstraints.BOTH;
		final int FILL_NONE = GridBagConstraints.NONE;
		final int FILL_HORI = GridBagConstraints.HORIZONTAL;
		final int FILL_VERT = GridBagConstraints.VERTICAL;
		final int FIRST_LINE_START = GridBagConstraints.FIRST_LINE_START;
		final int WEST = GridBagConstraints.WEST;
		final int EAST = GridBagConstraints.EAST;

		/*
		 * Script Editor Tab
		 */

		/*
		 * Default values: gridx = RELATIVE; gridy = RELATIVE; gridwidth = 1;
		 * gridheight = 1;
		 *
		 * weightx = 0; weighty = 0; anchor = CENTER; fill = NONE;
		 *
		 * insets = new Insets(0, 0, 0, 0); ipadx = 0; ipady = 0;
		 */

		m_columnMatchingTable = new ColumnInputMatchingTable(
				new DataTableSpec(), null);

		final GridBagConstraints gbc_lbl_header = createGBC(0, 0, 1, 1, WEST,
				FILL_NONE, new Insets(0, 3, 0, 0));
		final GridBagConstraints gbc_lbl_lang = createGBC(2, 0, 1, 1, EAST,
				FILL_NONE, new Insets(0, 0, 0, 3));
		final GridBagConstraints gbc_lbl_cim = createGBC(1, 2, 1, 1, EAST,
				FILL_NONE, new Insets(0, 3, 0, 0));

		final GridBagConstraints gbc_ls = createGBC(3, 0, 2, 1, EAST, FILL_HORI,
				new Insets(3, 3, 3, 3));

		final GridBagConstraints gbc_ep = createGBC(0, 1, 5, 1,
				FIRST_LINE_START, FILL_BOTH, 1.0, 1.0, new Insets(0, 3, 0, 3));
		final GridBagConstraints gbc_csl = createGBC(0, 2, 1, 2, WEST,
				FILL_VERT, new Insets(0, 3, 3, 0));
		final GridBagConstraints gbc_cim = createGBC(1, 3, 4, 1,
				FIRST_LINE_START, FILL_BOTH, 1.0, 0.0, new Insets(0, 3, 3, 3));

		final GridBagConstraints gbc_add = createGBC(3, 2, 1, 1, WEST,
				FILL_HORI, new Insets(3, 3, 3, 3));
		final GridBagConstraints gbc_rem = createGBC(4, 2, 1, 1, WEST,
				FILL_HORI, new Insets(3, 3, 3, 3));

		m_editorPanel.add(m_langSelection, gbc_ls);

		m_codeEditor = new CodeEditorDialogComponent(m_codeModel);
		addDialogComponent(m_editorPanel, m_codeEditor, gbc_ep);

		m_columnList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_columnList.setUserSelectionAllowed(true);

		final JPanel columnSelectionPanel = new JPanel(new GridBagLayout());
		columnSelectionPanel.add(LBL_COLUMN,
				createGBC(0, 0, 1, 1, FIRST_LINE_START, FILL_HORI, 1.0, 0.0));
		columnSelectionPanel.add(
				new JScrollPane(m_columnList,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
				createGBC(0, 1, 1, 1, FIRST_LINE_START, FILL_BOTH, 1.0, 1.0));
		columnSelectionPanel.setPreferredSize(new Dimension(180, 0));
		m_editorPanel.add(columnSelectionPanel, gbc_csl);

		final JScrollPane scrollPane = new JScrollPane(m_columnMatchingTable);
		m_columnMatchingTable.setFillsViewportHeight(true);
		// make sure cell editing stops before rows are removed
		m_columnMatchingTable.putClientProperty("terminateEditOnFocusLost",
				Boolean.TRUE);
		m_columnMatchingTable
				.setPreferredScrollableViewportSize(new Dimension(100, 150));
		m_editorPanel.add(scrollPane, gbc_cim);

		/* "Add column/input matching" button */
		m_addBtn.setActionCommand(SciJavaScriptingNodeDialogListener.CMD_ADD);
		m_addBtn.setToolTipText("Add column/input matching.");
		m_editorPanel.add(m_addBtn, gbc_add);

		/* "Remove column/input matching" button */
		m_remBtn.setActionCommand(SciJavaScriptingNodeDialogListener.CMD_REM);
		m_remBtn.setToolTipText("Remove selected column/input matching.");
		m_editorPanel.add(m_remBtn, gbc_rem);

		/*
		 * Labels
		 */
		m_editorPanel.add(LBL_HEADER, gbc_lbl_header);
		m_editorPanel.add(LBL_LANG, gbc_lbl_lang);
		m_editorPanel.add(LBL_CIM, gbc_lbl_cim);

		/// DEBUG CODE ///
		if (DEBUG_UI) {
			applyDebugColors();
		}
	}

	/**
	 * @return The main "Script" panel used for all components
	 */
	public Component editorPane() {
		return m_editorPanel;
	}

	/**
	 * @return The code editor
	 */
	public CodeEditorDialogComponent codeEditor() {
		return m_codeEditor;
	}

	/**
	 * @return The combobox used for selecting the nodes language
	 */
	public JComboBox<String> languageSelection() {
		return m_langSelection;
	}

	/**
	 * @return Collection of the dialog components whose settings need to be
	 *         saved.
	 */
	public Collection<DialogComponent> dialogComponents() {
		return m_dialogComponents;
	}

	/**
	 * @return The table which displays which columns fill which inputs of the
	 *         current module script
	 */
	public ColumnInputMatchingTable columnInputMatchingTable() {
		return m_columnMatchingTable;
	}

	/**
	 * Force rebuild the dialog. <strong>Should only be used for debug
	 * purposes.</strong>
	 */
	public void rebuildDialog() {
		m_editorPanel.removeAll();
		m_dialogComponents.clear();
		buildDialog();

		setContext(m_context); // required, since CodeEditor et al. will be
								// reconstructed
	}

	/**
	 * @return The list which displays all columns of the table attached to the
	 *         input port.
	 */
	public ColumnSelectionList columnList() {
		return m_columnList;
	}

	/**
	 * Add a listener to all components.
	 *
	 * @param listener
	 *            The listener
	 */
	public <T extends ActionListener & MouseListener> void addListener(
			final T listener) {
		m_columnList.addMouseListener(listener);
		m_addBtn.addActionListener(listener);
		m_remBtn.addActionListener(listener);
	}

	/**
	 * Remove a listener from all components.
	 *
	 * @param listener
	 *            The listener
	 */
	public <T extends ActionListener & MouseListener> void removeListener(
			final T listener) {
		m_columnList.removeMouseListener(listener);
		m_addBtn.removeActionListener(listener);
		m_remBtn.removeActionListener(listener);
	}

	// --- Contextual methods ---

	private Context m_context;

	@Override
	public void setContext(final Context context) {
		m_codeEditor.setContext(context);
		m_columnMatchingTable.setContext(context);

		m_context = context; // no injection needed.
	}

	@Override
	public Context context() {
		return m_context;
	}

	@Override
	public Context getContext() {
		return context();
	}

}
