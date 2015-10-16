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
package org.knime.knip.scripting.node;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.scijava.commands.DefaultKNIMEScijavaContext;
import org.knime.knip.scijava.commands.KNIMEScijavaContext;
import org.knime.knip.scijava.commands.mapping.ColumnModuleItemMapping;
import org.knime.knip.scijava.commands.mapping.ColumnToModuleItemMappingUtil;
import org.knime.knip.scijava.commands.widget.KnimeSwingInputPanel;
import org.knime.knip.scijava.core.TempClassLoader;
import org.knime.knip.scripting.base.CompileHelper;
import org.knime.knip.scripting.base.CompileProductHelper;
import org.knime.knip.scripting.base.ScriptingGateway;
import org.knime.knip.scripting.settings.ColumnCreationMode;
import org.knime.knip.scripting.ui.ScriptingNodeDialogListener;
import org.knime.knip.scripting.ui.ScriptingNodeDialogPane;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;
import org.scijava.ui.swing.widget.SwingInputHarvester;

import net.imagej.ui.swing.script.SyntaxHighlighter;

/**
 * Dialog for the Scripting Node.
 *
 * @author <a href="mailto:jonathan.hale@uni-konstanz.de">Jonathan Hale</a>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 *
 * @see ScriptingNodeModel
 * @see ScriptingNodeFactory
 */
public class ScriptingNodeDialog extends NodeDialogPane {

	private final ScriptingNodeSettings m_settings = new ScriptingNodeSettings();

	/* script output and error writers */
	Writer m_errorWriter;
	Writer m_outputWriter;

	/* panel generated from current script */
	private final JPanel m_autogenPanel = new JPanel(new GridBagLayout());

	/* Scijava context */
	private final Context m_context;
	private final KNIMEScijavaContext m_knimeContext;

	@Parameter
	private ObjectService m_objectService;
	@Parameter
	private CommandService m_commandService;
	@Parameter
	private ScriptService m_scriptService;
	@Parameter
	private PluginService m_pluginService;

	/* "cache" for data and compilation results */
	private CompileProductHelper m_compilationResult;

	private final ScriptingNodeDialogPane m_gui;
	/* listener for events generated by the components of m_gui */
	private ScriptingNodeDialogListener m_listener;

	private CompileHelper m_compiler = null;

	/** autogenerated dialog components for loading and saving settings */
	private List<DialogComponent> m_generatedDialogComponents = Collections
			.emptyList();

	/**
	 * Default constructor
	 */
	public ScriptingNodeDialog() {
		m_gui = new ScriptingNodeDialogPane(getLogger(), m_settings);

		m_errorWriter = new StringWriter();
		m_outputWriter = new StringWriter();

		NodeLogger.addKNIMEConsoleWriter(m_errorWriter, NodeLogger.LEVEL.WARN,
				NodeLogger.LEVEL.ERROR);
		NodeLogger.addKNIMEConsoleWriter(m_outputWriter, NodeLogger.LEVEL.INFO,
				NodeLogger.LEVEL.DEBUG);

		m_context = ScriptingGateway.get().createContext();
		m_context.inject(this);
		m_knimeContext = new DefaultKNIMEScijavaContext();
		m_knimeContext.setContext(m_context);
		m_knimeContext.nodeSettings()
				.setSettingsModels(m_settings.otherSettings());

		// This is required for the compiler to find classes on classpath
		// (scijava-common for example)
		try (final TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			try {
				m_compiler = new CompileHelper(m_context);
			} catch (final IOException e) {
				// caused only by temporary directory not createable
			}

			// Initialize syntax hilighting plugins, so that the editor can
			// automatically use them
			final AbstractTokenMakerFactory tokenMakerFactory = (AbstractTokenMakerFactory) TokenMakerFactory
					.getDefaultInstance();
			for (final PluginInfo<SyntaxHighlighter> info : m_pluginService
					.getPluginsOfType(SyntaxHighlighter.class)) {
				try {
					tokenMakerFactory.putMapping("text/" + info.getName(),
							info.getClassName());
				} catch (final Throwable t) {
					getLogger().warn("Could not register " + info.getName(), t);
				}
			}

			m_gui.setContext(m_context);
			m_listener = new ScriptingNodeDialogListener(m_gui, getLogger(),
					m_settings);
			m_listener.setContext(m_context);
			m_gui.addListener(m_listener);
		}

		/*
		 * detect Scijava ScriptLanguage plugins and add to the combobox for the
		 * user to select
		 */
		try (TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {

			final Set<String> languageSet = new LinkedHashSet<>();
			for (final ScriptLanguage lang : m_context
					.getService(ScriptService.class).getIndex()) {
				languageSet.add(lang.toString());
			}
			final String[] languages = languageSet.stream().toArray(size -> {
				return new String[size];
			});

			if (languages.length != 0) {
				m_gui.languageSelection()
						.setModel(new DefaultComboBoxModel<String>(languages));
			} /* else, stays String[]{"Java"} */

			m_gui.languageSelection()
					.setSelectedItem(m_settings.getScriptLanguageName());
			m_gui.languageSelection().addItemListener((event) -> {
				/*
				 * Update settings and script language, if language is selected
				 * via the combobox.
				 */
				m_settings.setScriptLanguageName(
						(String) m_gui.languageSelection().getSelectedItem());
				updateScriptLanguage();
			});

			updateScriptLanguage();
		}

		/* create tabs */
		addTabAt(0, "Script Editor", m_gui.editorPane());
		addTabAt(1, "Script Dialog", m_autogenPanel);
		addTabAt(2, "Output Table", createOutputTablePane());

		getPanel().revalidate();
	}

	private JPanel createOutputTablePane() {
		JPanel outTablePane = new JPanel();
		outTablePane.setLayout(new BorderLayout());

		JPanel contents = new JPanel();
		contents.setLayout(new BoxLayout(contents, BoxLayout.PAGE_AXIS));

		/* Column creation mode */
		final DialogComponentStringSelection colCreationModeComp = new DialogComponentStringSelection(
				m_settings.columnCreationModeModel(), "Column Creation Mode",
				ColumnCreationMode.NEW_TABLE.toString(),
				ColumnCreationMode.APPEND_COLUMNS.toString());

		m_gui.dialogComponents().add(colCreationModeComp);

		JPanel comp = colCreationModeComp.getComponentPanel();
		contents.add(comp);

		/* Column suffix */
		final DialogComponentString colSuffixComp = new DialogComponentString(
				m_settings.columnSuffixModel(), "Column Suffix");

		m_gui.dialogComponents().add(colSuffixComp);
		// FIXME: Hack to have component initially disabled, if necessary.
		colSuffixComp.setEnabled(m_settings.columnSuffixModel().isEnabled());

		comp = colSuffixComp.getComponentPanel();
		contents.add(comp);

		outTablePane.add(contents, BorderLayout.NORTH);

		return outTablePane;
	}

	private void updateScriptLanguage() {
		final ScriptLanguage language = m_scriptService.getLanguageByName(
				(String) m_gui.languageSelection().getSelectedItem());

		// Hack to set language of the EditorPane: TODO Fix in imagej-ui-swing!
		m_gui.codeEditor().getEditorPane()
				.setFileName(new File("." + language.getExtensions().get(0)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		try (final TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			ColumnToModuleItemMappingUtil.fillStringArraySettingsModel(
					m_knimeContext.inputMapping(),
					m_settings.columnInputMappingModel());

			for (final DialogComponent c : m_gui.dialogComponents()) {
				c.saveSettingsTo(settings);
			}

			m_settings.saveSettingsTo(settings);

			m_compilationResult = m_compiler.compile(m_settings.getScriptCode(),
					m_scriptService.getLanguageByName(
							m_settings.getScriptLanguageName()));

			// update autogen panel
			createAutogenPanel();

			// save settings for autogenerated components
			m_knimeContext.nodeSettings().saveSettingsTo(settings);
		} catch (final Throwable t) {
			t.printStackTrace();
			throw new InvalidSettingsException(t);
		}

		// save settings for autogenerated components
		for (final DialogComponent dialogComponent : m_generatedDialogComponents) {
			dialogComponent.saveSettingsTo(settings);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		try (final TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			try {
				m_settings.loadSettingsFrom(settings);
			} catch (Throwable t) {

			}
			// DEBUG CODE //
			if (ScriptingNodeDialogPane.DEBUG_UI) {
				// rebuild panel to reflect possible changes
				// 'hot code replaced'
				m_gui.rebuildDialog();
			}

			m_knimeContext.inputMapping().clear();
			ColumnToModuleItemMappingUtil.fillColumnToModuleItemMappingService(
					m_settings.getColumnInputMapping(),
					m_knimeContext.inputMapping());

			// load Settings for common DialogComponents
			for (final DialogComponent c : m_gui.dialogComponents()) {
				c.loadSettingsFrom(settings, specs);
			}

			m_compilationResult = m_compiler.compile(m_settings.getScriptCode(),
					m_scriptService.getLanguageByName(
							m_settings.getScriptLanguageName()));

			if (m_compilationResult == null) {
				return;
			}

			m_gui.columnList().update(specs[0]);
			m_gui.columnInputMatchingTable().updateModel(specs[0],
					m_compilationResult.getModuleInfo());

			// recreate autogen panel
			createAutogenPanel();

		} catch (final Throwable t) {
			t.printStackTrace();
			throw new NotConfigurableException("Failed to load settings", t);
		}

		// load settings for autogenerated components
		for (final DialogComponent dialogComponent : m_generatedDialogComponents) {
			dialogComponent.loadSettingsFrom(settings, specs);
		}
	}

	/*
	 * Generate the contents of m_autogenPanel from the compiled module
	 */
	private void createAutogenPanel() {
		m_autogenPanel.removeAll();

		if (m_compilationResult == null) {
			return;
		}

		final SwingInputHarvester builder = new SwingInputHarvester();
		builder.setContext(m_context);

		final KnimeSwingInputPanel inputPanel = new KnimeSwingInputPanel();

		try {

			final Module module = m_compilationResult
					.createModule(m_scriptService.getLanguageByName(
							m_settings.getScriptLanguageName()));
			for (final ModuleItem<?> input : m_compilationResult.inputs()) {
				final String inputName = input.getName();
				final ColumnModuleItemMapping mapping = m_knimeContext
						.inputMapping().getMappingForModuleItemName(inputName);
				// If this input is filled by a column mapping don't generate UI
				// for it
				final boolean noUI = (mapping != null) && mapping.isActive();
				module.setResolved(inputName, noUI);
			}
			builder.buildPanel(inputPanel, module);
		} catch (final ModuleException e) {
			e.printStackTrace();
		}
		m_autogenPanel.add(inputPanel.getComponent());

		m_generatedDialogComponents = new ArrayList<>(
				inputPanel.createDialogComponents());
	}

	/**
	 * @return The currently set language for the node
	 */
	protected ScriptLanguage getCurrentLanguage() {
		final String languageName = m_settings.getScriptLanguageName();
		final ScriptLanguage language = m_scriptService
				.getLanguageByName(languageName);
		if (language == null) {
			throw new NullPointerException("Could not load language "
					+ languageName + " for Scripting Node.");
		}
		return language;
	}

}
