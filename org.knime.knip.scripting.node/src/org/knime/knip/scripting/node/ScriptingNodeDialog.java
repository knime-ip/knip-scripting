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

import java.awt.GridBagLayout;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
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
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scijava.core.TempClassLoader;
import org.knime.knip.scripting.base.ScriptingGateway;
import org.knime.knip.scripting.matching.ColumnToModuleItemMapping;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService;
import org.knime.knip.scripting.matching.Util;
import org.knime.knip.scripting.ui.ScriptingNodeDialogListener;
import org.knime.knip.scripting.ui.ScriptingNodeDialogPane;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugins.scripting.java.JavaEngine;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptLanguageIndex;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.ui.swing.widget.SwingInputPanel;

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
	private ScriptService m_scriptService;
	@Parameter
	private PluginService m_pluginService;

	private ScriptEngine m_scriptEngine;

	/* "cache" for data and compilation results */
	private Module m_lastCompiledModule;

	/* whether the delayed constructor has been called */
	private boolean m_constructed = false;

	private final ScriptingNodeDialogPane m_gui;
	/* listener for events generated by the components of m_gui */
	private ScriptingNodeDialogListener m_listener;

	/**
	 * Default constructor
	 */
	public ScriptingNodeDialog() {
		m_gui = new ScriptingNodeDialogPane(getLogger());

		/* create tabs */
		super.addTabAt(0, "Script Editor", m_gui.editorPane());
		super.addTabAt(1, "<Autogenerated Tab>", m_autogenPanel);

		m_errorWriter = new StringWriter();
		m_outputWriter = new StringWriter();

		NodeLogger.addKNIMEConsoleWriter(m_errorWriter, NodeLogger.LEVEL.WARN,
				NodeLogger.LEVEL.ERROR);
		NodeLogger.addKNIMEConsoleWriter(m_outputWriter, NodeLogger.LEVEL.INFO,
				NodeLogger.LEVEL.DEBUG);

		m_gui.setSettings(m_settings);
		m_gui.buildDialog();

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

		m_context = ScriptingGateway.get().getContext(m_settings.getNodeId());

		// This is required for the compiler to find classes on classpath
		// (scijava-common for example)
		try (final TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {

			m_context.inject(this);

			// Get syntax hilighting plugins
			AbstractTokenMakerFactory tokenMakerFactory = (AbstractTokenMakerFactory) TokenMakerFactory
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

			m_listener = new ScriptingNodeDialogListener(m_gui, getLogger());
			m_context.inject(m_listener);
			m_gui.setContext(m_context);
			m_gui.addListener(m_listener);
		}

		/*
		 * detect Scijava ScriptLanguage plugins and add to the combobox for the
		 * user to select
		 */
		try (TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			final ScriptLanguageIndex index = m_context
					.getService(ScriptService.class).getIndex();

			String[] languages = index.parallelStream().map((lang) -> {
				return lang.toString();
			}).toArray((length) -> {
				return new String[length];
			});

			if (languages.length != 0) {
				m_gui.languageSelection()
						.setModel(new DefaultComboBoxModel<String>(languages));
			} /* else, stays String[]{"Java"} */

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

	}

	private void updateScriptLanguage() {
		final ScriptLanguage language = m_scriptService.getLanguageByName(
				(String) m_gui.languageSelection().getSelectedItem());
		m_scriptEngine = language.getScriptEngine();

		// hack to set language of the EditorPane: TODO Fix in imagej-ui-swing!
		m_gui.codeEditor().getEditorPane()
				.setFileName(new File("." + language.getExtensions().get(0)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		try {
			Util.fillStringArraySettingsModel(m_cimService,
					m_settings.columnInputMappingModel());

			for (final DialogComponent c : m_gui.dialogComponents()) {
				c.saveSettingsTo(settings);
			}

			m_settings.saveSettingsTo(settings);

			// save settings for autogenerated components
			m_settingsService.saveSettingsTo(settings);

			m_lastCompiledModule = compile();

			// update autogen panel
			createAutogenPanel();
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		try {
			m_settings.loadSettingsFrom(settings);
			ScriptingNodeDialogDelayed(); // try to call the delayed constructor
		} catch (final Throwable e) {
			m_constructed = false;
			e.printStackTrace();
			throw new NotConfigurableException(e.getMessage());
		}

		try {
			// DEBUG CODE //
			if (ScriptingNodeDialogPane.DEBUG_UI) {
				// rebuild panel to reflect possible changes
				// 'hot code replaced'
				m_gui.rebuildDialog();
			}

			m_cimService.clear();
			Util.fillColumnToModuleItemMappingService(
					m_settings.getColumnInputMapping(), m_cimService);

			// load Settings for common DialogComponents
			for (final DialogComponent c : m_gui.dialogComponents()) {
				c.loadSettingsFrom(settings, specs);
			}

			// keep data for later use
			m_lastCompiledModule = compile();

			if (m_lastCompiledModule == null) {
				return;
			}

			m_gui.columnList().update(specs[0]);
			m_gui.columnInputMatchingTable().updateModel(specs[0],
					m_lastCompiledModule.getInfo());

			// recreate autogen panel
			createAutogenPanel();

		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}

		try {
			// load settings for autogenerated components
			m_settingsService.loadSettingsFrom(settings);
		} catch (final InvalidSettingsException e) {
			// can happen frequently when inputs are added/removed
			e.printStackTrace(); // TODO
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
			for (final ModuleItem<?> input : m_lastCompiledModule.getInfo()
					.inputs()) {
				final String inputName = input.getName();
				final ColumnToModuleItemMapping mapping = m_cimService
						.getMappingForModuleItemName(inputName);
				// is this input filled by a column mapping? Otherwise generate
				// UI
				final boolean noUI = (mapping != null) && mapping.isActive();
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
		try {
			Module module = null;
			
			if (m_scriptEngine instanceof JavaEngine) {
				// create a precompiled module for better performance.
				module = ScriptingNodeModel.compile(m_scriptService,
						m_settings.getScriptCode(),
						m_settings.getScriptLanguageName()).createModule();
			} else {
				// create script module for execution
				final ScriptInfo info = new ScriptInfo(m_context, null,
						new StringReader(m_settings.getScriptCode()));
				final ScriptModule scriptModule = info.createModule();
				// use the currently selected language to execute the script
				scriptModule.setLanguage(m_scriptService
						.getLanguageByName(m_settings.getScriptLanguageName()));
				
				// map stdout and stderr to the UI
				scriptModule.setOutputWriter(m_outputWriter);
				scriptModule.setErrorWriter(m_errorWriter);
				
				module = scriptModule;
			}
			m_context.inject(module);

			return module;
		} catch (final ScriptException | ModuleException e) {
			e.printStackTrace();
		}
		return null;
	}

}
