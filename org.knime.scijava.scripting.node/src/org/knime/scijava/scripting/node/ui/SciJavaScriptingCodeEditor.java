package org.knime.scijava.scripting.node.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnSelectionList;
import org.scijava.Context;
import org.scijava.Contextual;

/**
 * User interface components of the ScriptingNode dialog.
 *
 * @author Jonathan Hale
 *
 */
public class SciJavaScriptingCodeEditor implements Contextual {

	/* containers */
	private final List<DialogComponent> m_dialogComponents = new ArrayList<>();

	/* UI Components */

	// Labels
	private final JLabel LBL_HEADER = new JLabel("Script Editor");
	private final JLabel LBL_LANG = new JLabel("Language:");

	// Code Editor
	private CodeEditorDialogComponent m_codeEditor;

	// tab panels
	private final JPanel m_editorPanel = new JPanel(new GridBagLayout());

	// Language selection Combobox
	private final JComboBox<String> m_langSelection = new JComboBox<>(
			new String[] { "Java" });

	// column selection list for generating inputs with column matchings
	private final ColumnSelectionList m_columnList = new ColumnSelectionList();
	private JPanel m_columnListPanel;

	// stores the code
	private final SettingsModelString m_codeModel;

	/**
	 * Constructor
	 *
	 * @param logger
	 *            NodeLogger to output messages to
	 * @param codeModel
	 *            the SettingsModel storing the code
	 */
	public SciJavaScriptingCodeEditor(final NodeLogger logger,
			final SettingsModelString codeModel) {
		/* one time setup of some components */
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

		final GridBagConstraints gbc_lbl_header = createGBC(0, 0, 1, 1, WEST,
				FILL_NONE, new Insets(0, 3, 0, 0));
		final GridBagConstraints gbc_lbl_lang = createGBC(2, 0, 1, 1, EAST,
				FILL_NONE, new Insets(0, 0, 0, 3));
		final GridBagConstraints gbc_ls = createGBC(3, 0, 2, 1, EAST, FILL_HORI,
				new Insets(3, 3, 3, 3));

		final GridBagConstraints gbc_ep = createGBC(0, 1, 5, 1,
				FIRST_LINE_START, FILL_BOTH, 1.0, 1.0, new Insets(0, 3, 0, 3));

		m_editorPanel.add(m_langSelection, gbc_ls);

		m_codeEditor = new CodeEditorDialogComponent(m_codeModel);
		addDialogComponent(m_editorPanel, m_codeEditor, gbc_ep);

		// create column selection code
		m_columnList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_columnList.setUserSelectionAllowed(true);

		m_columnListPanel = new JPanel(new GridBagLayout());
		m_columnListPanel.add(
				new JScrollPane(m_columnList,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS),
				createGBC(0, 1, 1, 1, FIRST_LINE_START, FILL_BOTH, 1.0, 1.0));

		m_columnListPanel.setPreferredSize(new Dimension(250, 300));
		/*
		 * Labels
		 */
		m_editorPanel.add(LBL_HEADER, gbc_lbl_header);
		m_editorPanel.add(LBL_LANG, gbc_lbl_lang);

	}

	/**
	 * @return The main "Script" panel used for all components
	 */
	public Component getEditorPane() {
		return m_editorPanel;
	}

	/**
	 * @return The code editor
	 */
	public CodeEditorDialogComponent getCodeEditor() {
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
	 * @return The list which displays all columns of the table attached to the
	 *         input port.
	 */
	public JPanel getColumnListPanel() {
		return m_columnListPanel;
	}

	/**
	 * Add a listener to all components.
	 *
	 * @param listener
	 *            The listener
	 */
	public <T extends MouseListener> void addListener(final T listener) {
		m_columnList.addMouseListener(listener);
	}

	/**
	 * Remove a listener from all components.
	 *
	 * @param listener
	 *            The listener
	 */
	public <T extends MouseListener> void removeListener(final T listener) {
		m_columnList.removeMouseListener(listener);
	}

	// --- Contextual methods ---

	private Context m_context;

	@Override
	public void setContext(final Context context) {
		m_codeEditor.setContext(context);
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

	public ColumnSelectionList getColumnList() {
		return m_columnList;
	}

}
