/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.scripting.base;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.script.ScriptException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.util.ColumnSelectionList;
import org.knime.knip.scijava.commands.adapter.InputAdapterPlugin;
import org.knime.knip.scijava.commands.adapter.InputAdapterService;
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scripting.matching.ColumnToModuleItemMapping;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService;
import org.knime.knip.scripting.matching.Util;
import org.knime.knip.scripting.ui.CodeEditorDialogComponent;
import org.knime.knip.scripting.ui.table.ColumnInputMatchingTable;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugins.scripting.java.JavaEngine;
import org.scijava.plugins.scripting.java.JavaScriptLanguage;
import org.scijava.script.ScriptLanguage;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.ui.swing.widget.SwingInputPanel;

/**
 * Dialog for the Scripting Node.
 *
 * @author <a href="mailto:jonathan.hale@uni-konstanz.de">Jonathan Hale</a>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 *
 * @see ScriptingNodeModel
 * @see ScriptingNodeFactory
 */
public class ScriptingNodeDialog extends NodeDialogPane implements
		ActionListener, MouseListener {

	/* settings: SettingsModels/Config IDs etc */
	private final SettingsModelString m_codeModel = ScriptingNodeModel
			.createCodeSettingsModel();

	private final SettingsModelStringArray m_columnInputMappingSettignsModel = ScriptingNodeModel
			.createColumnInputMappingSettingsModel();

	private final SettingsModelInteger m_nodeId = ScriptingNodeModel
			.createIDSettingsModel();

	/* containers */
	private final ArrayList<DialogComponent> m_dialogComponents = new ArrayList<DialogComponent>();

	/* options for easy debugging */
	public final static boolean DEBUG_UI = false;

	// scijava context
	private Context m_context;

	@Parameter
	private ObjectService m_objectService;

	@Parameter
	private CommandService m_commandService;

	@Parameter
	private ColumnToModuleItemMappingService m_cimService;

	@Parameter
	private NodeSettingsService m_settingsService;

	@Parameter
	private InputAdapterService m_inputAdapters;

	private JavaScriptLanguage m_java;
	private JavaEngine m_javaEngine;

	/* UI Components */

	// Table containing column/input matching.
	private ColumnInputMatchingTable m_columnMatchingTable;

	// Labels
	private final JLabel LBL_HEADER = new JLabel("Script Editor");
	private final JLabel LBL_LANG = new JLabel("Language: ");
	private final JLabel LBL_COLUMN = new JLabel("Column:");
	private final JLabel LBL_CIM = new JLabel("Column/Input Matchings:");

	// ActionCommands for removing and adding column/input matchings
	private final static String CMD_ADD = "add";
	private final static String CMD_REM = "rem";

	// Code Editor
	CodeEditorDialogComponent m_codeEditor = null;

	// tab panels
	private final JPanel m_editorPanel = new JPanel(new GridBagLayout());
	private final JPanel m_autogenPanel = new JPanel(new GridBagLayout());

	// Language selection Combobox
	private final JComboBox<String> m_langSelection = new JComboBox<String>(
			new String[] { "Java" });

	// column selection list for generating inputs with column matchings
	private final ColumnSelectionList m_columnList = new ColumnSelectionList();

	/* "cache" for data and compilation results */
	private DataTableSpec m_lastDataTableSpec;
	private Module m_lastCompiledModule;

	/* whether the delayed constructor has been called */
	private boolean m_constructed = false;

	/**
	 * Default constructor
	 */
	public ScriptingNodeDialog() {
		/* one time setup of some components */
		LBL_COLUMN.setBorder(UIManager.getBorder("TableHeader.cellBorder"));

		/* create tabs */
		super.addTabAt(0, "Script Editor", m_editorPanel);
		super.addTabAt(1, "<Autogenerated Tab>", m_autogenPanel);

		// we will use the delayed constructor for everything else
	}

	/*
	 * We need to wait for id to be filled so that we can get the correct
	 * Context which is shared with the NodeModel. The constructor is called in
	 * loadSettingsFrom().
	 */
	private void ScriptingNodeDialogDelayed() {
		if (m_constructed) {
			// already called the constructor!
			return;
		}
		// now the constructor has been called.
		m_constructed = true;

		m_context = ScriptingGateway.get().getContext(m_nodeId.getIntValue());
		m_context.inject(this);

		m_columnMatchingTable = new ColumnInputMatchingTable(
				new DataTableSpec(), null, m_context);

		final ArrayList<String> languages = new ArrayList<String>();
		for (final ScriptLanguage s : m_objectService
				.getObjects(ScriptLanguage.class)) {
			if (languages.contains(s.getLanguageName())) {
				continue;
			}
			languages.add(s.getLanguageName());
		}

		m_langSelection.setModel(new DefaultComboBoxModel<String>(languages
				.toArray(new String[] {})));

		m_java = m_objectService.getObjects(JavaScriptLanguage.class).get(0);
		m_javaEngine = (JavaEngine) m_java.getScriptEngine();

		buildDialog();
	}

	/* utility functions for creating GridBagConstraints */
	private static final GridBagConstraints createGBC(final int x, final int y,
			final int w, final int h, final int anchor, final int fill) {
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
				fill, new Insets(0, 0, 0, 0), 0, 0);
	}

	private static final GridBagConstraints createGBC(final int x, final int y,
			final int w, final int h, final int anchor, final int fill,
			final double weightx, final double weighty) {
		return new GridBagConstraints(x, y, w, h, weightx, weighty, anchor,
				fill, new Insets(0, 0, 0, 0), 0, 0);
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
				FILL_NONE);
		final GridBagConstraints gbc_lbl_lang = createGBC(2, 0, 1, 1, EAST,
				FILL_NONE);
		final GridBagConstraints gbc_lbl_cim = createGBC(1, 2, 1, 1, EAST,
				FILL_NONE);

		final GridBagConstraints gbc_ls = createGBC(3, 0, 2, 1, EAST, FILL_HORI);
		gbc_ls.insets = new Insets(3, 3, 3, 0);

		final GridBagConstraints gbc_ep = createGBC(0, 1, 5, 1,
				FIRST_LINE_START, FILL_BOTH, 1.0, 1.0);
		gbc_ep.insets = new Insets(0, 3, 0, 0);
		final GridBagConstraints gbc_csl = createGBC(0, 2, 1, 2, WEST,
				FILL_VERT);
		final GridBagConstraints gbc_cim = createGBC(1, 3, 4, 1,
				FIRST_LINE_START, FILL_BOTH, 1.0, 0.0);

		final GridBagConstraints gbc_add = createGBC(3, 2, 1, 1, WEST,
				FILL_HORI);
		final GridBagConstraints gbc_rem = createGBC(4, 2, 1, 1, WEST,
				FILL_HORI);

		m_editorPanel.add(m_langSelection, gbc_ls);

		m_codeEditor = new CodeEditorDialogComponent(m_codeModel);
		addDialogComponent(m_editorPanel, m_codeEditor, gbc_ep);

		m_columnList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_columnList.setUserSelectionAllowed(true);
		m_columnList.addMouseListener(this);

		final JPanel columnSelectionPanel = new JPanel(new GridBagLayout());
		columnSelectionPanel.add(LBL_COLUMN,
				createGBC(0, 0, 1, 1, FIRST_LINE_START, FILL_HORI, 1.0, 0.0));
		columnSelectionPanel.add(m_columnList,
				createGBC(0, 1, 1, 1, FIRST_LINE_START, FILL_BOTH, 1.0, 1.0));
		// FIXME lazy way of adding the neat border. Add scrollpane to list
		// instead
		// columnSelectionPanel.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
		columnSelectionPanel.setPreferredSize(new Dimension(180, 0));
		final JScrollPane sp = new JScrollPane(columnSelectionPanel);
		m_editorPanel.add(sp, gbc_csl);

		final JScrollPane scrollPane = new JScrollPane(m_columnMatchingTable);
		m_columnMatchingTable.setFillsViewportHeight(true);
		// make sure cell editing stops before rows are removed
		m_columnMatchingTable.putClientProperty("terminateEditOnFocusLost",
				Boolean.TRUE);
		m_columnMatchingTable.setPreferredScrollableViewportSize(new Dimension(
				100, 150));
		m_editorPanel.add(scrollPane, gbc_cim);

		/*
		 * "Add column/input matching" button
		 */
		final JButton addBtn = new JButton("+");
		addBtn.setActionCommand(CMD_ADD);
		addBtn.addActionListener(this);
		addBtn.setToolTipText("Add column/input matching.");
		m_editorPanel.add(addBtn, gbc_add);

		/*
		 * "Remove column/input matching" button
		 */
		final JButton remBtn = new JButton("-");
		remBtn.setActionCommand(CMD_REM);
		remBtn.addActionListener(this);
		remBtn.setToolTipText("Remove selected column/input matching.");
		m_editorPanel.add(remBtn, gbc_rem);

		/*
		 * Labels
		 */
		m_editorPanel.add(LBL_HEADER, gbc_lbl_header);
		m_editorPanel.add(LBL_LANG, gbc_lbl_lang);
		m_editorPanel.add(LBL_CIM, gbc_lbl_cim);

		// / DEBUG CODE ///
		if (DEBUG_UI) {
			applyDebugColors();
			getLogger().debug("Built Dialog!");
		}

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		for (final DialogComponent c : m_dialogComponents) {
			c.saveSettingsTo(settings);
		}

		// save settings for autogenerated components
		m_settingsService.saveSettingsTo(settings);

		Util.fillStringArraySettingsModel(m_cimService,
				m_columnInputMappingSettignsModel);
		m_columnInputMappingSettignsModel.saveSettingsTo(settings);

		m_lastCompiledModule = compile();

		// update autogen panel
		createAutogenPanel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		try {
			// load node id. Does not need to be saved!
			m_nodeId.loadSettingsFrom(settings);
			ScriptingNodeDialogDelayed(); // try to call the delayed constructor
		} catch (final Exception e) {
			/* ignore */
		}

		// / DEBUG CODE ///
		if (DEBUG_UI) {
			// rebuild panel to reflect possible changes
			// 'hot code replaced'
			m_editorPanel.removeAll();
			m_dialogComponents.clear();
			buildDialog();
		}

		try {
			m_columnInputMappingSettignsModel.loadSettingsFrom(settings);
			m_cimService.clear();
			Util.fillColumnToModuleItemMappingService(
					m_columnInputMappingSettignsModel, m_cimService);
		} catch (final InvalidSettingsException e) {
			e.printStackTrace();
		}

		// load Settings for common DialogComponents
		for (final DialogComponent c : m_dialogComponents) {
			c.loadSettingsFrom(settings, specs);
		}

		// keep data for later use
		m_lastDataTableSpec = specs[0];
		m_lastCompiledModule = compile();

		if (m_lastCompiledModule == null) {
			return;
		}

		m_columnList.update(specs[0]);
		m_columnMatchingTable.updateModel(specs[0],
				m_lastCompiledModule.getInfo());

		// recreate autogen panel
		createAutogenPanel();

		try {
			// load settings for autogenerated components
			m_settingsService.loadSettingsFrom(settings);
		} catch (final InvalidSettingsException e) {
			// can happen frequently when inputs are added/removed
		}
	}

	/*
	 * Generate the contents of m_autogenPanel from the compiled module
	 *
	 * pre-cond: m_lastCompiledModule != null
	 */
	private void createAutogenPanel() {
		m_autogenPanel.removeAll();

		if (m_lastCompiledModule == null) {
			return;
		}

		final SwingInputHarvester builder = new SwingInputHarvester();

		m_context.inject(builder);

		final SwingInputPanel inputPanel = builder.createInputPanel();

		try {
			for (final ModuleItem<?> i : m_lastCompiledModule.getInfo()
					.inputs()) {
				final String inputName = i.getName();

				boolean noUI = false;

				final ColumnToModuleItemMapping mapping = m_cimService
						.getMappingForModuleItemName(inputName);
				if (mapping != null) {
					noUI = mapping.isActive();
				} else {
					noUI = true;
				}

				// if there is a mapping for that input, we do not require UI.
				m_lastCompiledModule.setResolved(inputName, noUI);
			}
			builder.buildPanel(inputPanel, m_lastCompiledModule);
		} catch (final ModuleException e) {
			e.printStackTrace();
		}
		m_autogenPanel.add(inputPanel.getComponent());
	}

	/*
	 * Compile contents of m_codeModel into a Module.
	 */
	private Module compile() {
		Class<? extends Command> commandClass = null;
		try {
			commandClass = ScriptingNodeModel.compile(m_javaEngine,
					m_codeModel.getStringValue());
		} catch (final ScriptException e) {
			// if (m_codeEditor != null) {
			// RSyntaxTextArea textArea = m_codeEditor.getTextArea();
			// int startIndex;
			// try {
			// startIndex = textArea.getLineStartOffset(e.getLineNumber())
			// + e.getColumnNumber();
			// textArea.getHighlighter().addHighlight(startIndex,
			// startIndex + 3,
			// new SquiggleUnderlineHighlightPainter(Color.RED));
			// } catch (BadLocationException e1) {
			// }
			// }
			return null;
		}

		return m_commandService.getModuleService().createModule(
				new CommandInfo(commandClass, commandClass
						.getAnnotation(Plugin.class)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getActionCommand().equals(CMD_ADD)) {

			ModuleItem<?> i = null;
			try {
				i = m_lastCompiledModule.getInfo().inputs().iterator().next();
			} catch (final NoSuchElementException exc) {
				getLogger().error("No input found.");
				return;
			}

			final DataColumnSpec cs = m_lastDataTableSpec.iterator().next();

			if (cs == null) {
				getLogger().error("No column found.");
				return;
			}

			m_columnMatchingTable.getModel().addItem(cs.getName(), i.getName());
		}

		if (e.getActionCommand().equals(CMD_REM)) {
			final int row = m_columnMatchingTable.getSelectedRow();
			m_columnMatchingTable.getModel().removeItem(row);
		}
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		/*
		 * Create a column input mapping on column list double click
		 */
		if (e.getSource() == m_columnList) {
			// check for doubleclick
			if (e.getClickCount() == 2) {
				final int index = m_columnList.locationToIndex(e.getPoint());
				if (index >= 0) {
					final Object o = m_columnList.getModel()
							.getElementAt(index);

					if (o instanceof DataColumnSpec) {
						// better safe then sorry, should always be the case,
						// though
						final DataColumnSpec cspec = (DataColumnSpec) o;

						final String columnName = cspec.getName();
						String memberName = Character.toLowerCase(columnName
								.charAt(0)) + columnName.substring(1);

						int i = 0;
						String chosen = memberName;
						while (m_lastCompiledModule.getInfo().getInput(chosen) != null) {
							chosen = memberName + i;
							++i;
						}
						memberName = chosen;

						// get the Name of the first createable type
						@SuppressWarnings("rawtypes")
						final Iterator<InputAdapterPlugin> itor = m_inputAdapters
								.getMatchingInputAdapters(cspec.getType())
								.iterator();
						String typeName;
						if (itor.hasNext()) {
							typeName = itor.next().getType().getName();
						} else {
							// no adapter found, error out
							JOptionPane
									.showMessageDialog(
											null,
											"The column you selected has a datatype which cannot be used in Scripts.",
											"No matching adapter",
											JOptionPane.ERROR_MESSAGE);
							return;
						}

						// find position for inserting @Paramter declaration
						final int pos = m_codeEditor.getCode().indexOf('{') + 1;
						final String parameterCode = "\n\t@Parameter(type = ItemIO.INPUT)\n\tprivate "
								+ typeName + " " + memberName + ";\n";

						m_codeEditor.getTextArea().insert(parameterCode, pos);
						m_codeEditor.updateModel();

						// add a mapping for the newly created parameter
						m_columnMatchingTable.getModel().addItem(columnName,
								memberName);

						m_lastCompiledModule = compile();
					}
				}
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

}
