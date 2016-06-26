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
package org.knime.scijava.scripting.nodes.interactive.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.scijava.commands.settings.NodeDialogSettingsService;
import org.knime.scijava.commands.simplemapping.SimpleColumnMappingService;
import org.knime.scijava.core.TempClassLoader;
import org.knime.scijava.scripting.base.CompileHelper;
import org.knime.scijava.scripting.base.CompileProductHelper;
import org.knime.scijava.scripting.base.ScriptingGateway;
import org.knime.scijava.scripting.nodes.interactive.SciJavaScriptingNodeFactory;
import org.knime.scijava.scripting.nodes.interactive.SciJavaScriptingNodeModel;
import org.knime.scijava.scripting.nodes.interactive.settings.ColumnCreationMode;
import org.knime.scijava.scripting.nodes.interactive.settings.SciJavaScriptingNodeSettings;
import org.knime.scijava.scripting.nodes.interactive.settings.ScriptDialogMode;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleRunner;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.module.process.ServicePreprocessor;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptLanguage;
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
 * @see SciJavaScriptingNodeModel
 * @see SciJavaScriptingNodeFactory
 */
public class SciJavaScriptingNodeDialog extends NodeDialogPane {

    private final SciJavaScriptingNodeSettings m_settings =
            new SciJavaScriptingNodeSettings();

    /* script output and error writers */
    StringWriter m_errorWriter;
    StringWriter m_outputWriter;

    /* panel generated from current script */
    private final JPanel m_autogenPanel = new JPanel(new GridBagLayout());
    private JPanel m_innerPanel;
    private JPanel m_codeEditPanel;

    private JButton m_modeSwitchButton;

    /* Scijava context */
    private final Context m_context;

    @Parameter
    private ObjectService m_objectService;
    @Parameter
    private CommandService m_commandService;
    @Parameter
    private ScriptService m_scriptService;
    @Parameter
    private PluginService m_pluginService;
    @Parameter
    private NodeDialogSettingsService m_dialogSettingsService;
    @Parameter
    private SimpleColumnMappingService m_simpleColumnMappingService;

    /* "cache" for data and compilation results */
    private CompileProductHelper m_compileProduct;

    private SciJavaScriptingCodeEditor m_codeEditor;
    /* listener for events generated by the components of m_gui */
    private SciJavaScriptingNodeDialogListener m_listener;

    private CompileHelper m_compiler = null;

    private SwingInputPanel m_inputPanel;
    private JComponent m_component = new JPanel();
    private JComponent m_outputTablePanel;
    private JPanel m_errorPanel;

    /* preprocessor plugins needed to successfully display dialog components */
    private final List<PreprocessorPlugin> m_preprocessPlugins;

    private Module m_module;

    // Cache of the current code, to prevent unnecessary caching
    private String m_oldCode;

    public SciJavaScriptingNodeDialog(final Context scijavaContext)
            throws NotConfigurableException {

        m_errorWriter = new StringWriter();
        m_outputWriter = new StringWriter();

        m_context = scijavaContext;
        m_context.inject(this);

        m_preprocessPlugins = createPreprocessorList();
        // This is required for the compiler to find classes on classpath
        try (final TempClassLoader tempCl = new TempClassLoader(
                ScriptingGateway.get().createUrlClassLoader())) {
            try {
                m_compiler = new CompileHelper(m_context, m_errorWriter,
                        m_outputWriter);
            } catch (final IOException e) {
                throw new NotConfigurableException(
                        "Unable to create tmp directory to complile scripts: "
                                + e);
            }

            // Initialize syntax hilighting plugins, so that the editor can
            // automatically use them
            final AbstractTokenMakerFactory tokenMakerFactory =
                    (AbstractTokenMakerFactory) TokenMakerFactory
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
        }

        // needs to be created
        createCodeEditor();
    }

    /**
     * creates the main component
     *
     */
    private void createComponent() {
        m_component = new JPanel(new BorderLayout());
        m_innerPanel = new JPanel(new BorderLayout());

        // Create mode switching button
        final String initialText =
                m_settings.getMode() == ScriptDialogMode.CODE_EDIT
                        ? "Switch to Dialog" : "Switch to Code";
        m_modeSwitchButton = new JButton(initialText);
        m_modeSwitchButton.addActionListener(new ModeSwitchButtonListener());
        m_component.add(m_modeSwitchButton, BorderLayout.NORTH);

        if (m_settings.getMode() == ScriptDialogMode.CODE_EDIT) {
            m_innerPanel.add(createCodeEditorPanel(), BorderLayout.CENTER);
        } else if (m_settings.getMode() == ScriptDialogMode.SETTINGS_EDIT) {
            m_innerPanel.add(createDialogPanel(false), BorderLayout.CENTER);
        } else {
            throw new IllegalStateException("The mode: '"
                    + m_settings.getMode().toString() + "' is not supported");
        }
        m_component.add(m_innerPanel, BorderLayout.CENTER);
    }

    private JPanel createCodeEditorPanel() {

        m_codeEditPanel = new JPanel(new BorderLayout());

        final JPanel columnListPane = m_codeEditor.getColumnListPanel();
        columnListPane.setBorder(new TitledBorder("Column Selection"));
        m_codeEditPanel.add(columnListPane, BorderLayout.WEST);

        m_codeEditPanel.add(m_codeEditor.getEditorPane(), BorderLayout.CENTER);

        return m_codeEditPanel;
    }

    /**
     * creates the dialog pane
     *
     * @param clean
     *            whether the settings should be cleared (code has been
     *            recompiled)
     */
    private JPanel createDialogPanel(final boolean clean) {

        if (m_errorPanel != null) {
            m_component.remove(m_errorPanel);
            m_oldCode = ""; // ensure that we always recompile after an error
        }

        // only recompile if code changed or compileProduct null
        if (!m_settings.getScriptCode().equals(m_oldCode)
                || m_compileProduct == null) {
            m_oldCode = m_settings.getScriptCode();
            try {
                m_compileProduct = recompile(m_settings.getScriptCode(),
                        m_settings.getScriptLanguageName());
            } catch (final InvalidSettingsException e) {
                // code did not compile show error instead
                m_dialogSettingsService.clear();
                m_simpleColumnMappingService.clear();
                return createErrorPanel(e);
            }
        }

        // when switching from code clear settings
        if (clean) {
            m_dialogSettingsService.clear();
            m_simpleColumnMappingService.clear();
        }

        // Build dialog panel
        m_autogenPanel.removeAll();
        m_inputPanel = new SwingInputPanel();
        final SwingInputHarvester builder = new SwingInputHarvester();
        builder.setContext(m_context);

        try {
            m_module = m_compileProduct.createModule(getCurrentLanguage());
            // fill in services
            final ModuleRunner runner = new ModuleRunner(m_context, m_module,
                    m_preprocessPlugins, null);
            runner.preProcess();

            builder.buildPanel(m_inputPanel, m_module);
        } catch (final Throwable e) {
            return createErrorPanel(e);
        }

        m_autogenPanel.add(m_inputPanel.getComponent());
        m_autogenPanel.add(m_outputTablePanel);
        return m_autogenPanel;
    }

    private JPanel createErrorPanel(final Throwable e) {
        if (m_errorPanel != null) {
            m_component.remove(m_errorPanel);
        }
        m_errorPanel = new JPanel();
        m_errorPanel.setLayout(new BoxLayout(m_errorPanel, BoxLayout.Y_AXIS));
        m_errorPanel.add(
                new JLabel("Can't create dialog, compilation failed! " + e));
        getLogger().error("Can't create dialog, compilation failed!", e);

        final String error = m_errorWriter.toString();
        m_errorPanel.add(new JTextArea(error));
        // clear writer
        m_errorWriter.getBuffer().setLength(0);

        return m_errorPanel;

    }

    private void createCodeEditor() {
        m_codeEditor =
                new SciJavaScriptingCodeEditor(m_settings.getScriptCodeModel());
        m_codeEditor.setContext(m_context);
        m_listener = new SciJavaScriptingNodeDialogListener(m_codeEditor,
                getLogger(), this, m_settings);
        m_listener.setContext(m_context);
        m_codeEditor.addListener(m_listener);
    }

    /**
     * @return the panel that
     */
    private JPanel createOutputTablePane() {
        final JPanel outTablePane = new JPanel();
        outTablePane.setLayout(new BorderLayout());

        outTablePane.setBorder(new TitledBorder("Outout table settings"));

        final JPanel contents = new JPanel();
        contents.setLayout(new BoxLayout(contents, BoxLayout.PAGE_AXIS));

        final SettingsModelString columnCreationModeModel =
                m_settings.getColumnCreationModeModel();
        /* Column creation mode */
        final DialogComponentStringSelection colCreationModeComp =
                new DialogComponentStringSelection(columnCreationModeModel,
                        "Column Creation Mode",
                        ColumnCreationMode.NEW_TABLE.toString(),
                        ColumnCreationMode.APPEND_COLUMNS.toString());

        m_codeEditor.dialogComponents().add(colCreationModeComp);

        JPanel comp = colCreationModeComp.getComponentPanel();
        contents.add(comp);

        final SettingsModelString columnSuffixModel =
                m_settings.getColumnSuffixModel();
        /* Column suffix */
        final DialogComponentString colSuffixComp =
                new DialogComponentString(columnSuffixModel, "Column Suffix");

        // Ensure that component is correctly initialized.
        columnSuffixModel.setEnabled(columnCreationModeModel.getStringValue()
                .equals(ColumnCreationMode.APPEND_COLUMNS.toString()));

        m_codeEditor.dialogComponents().add(colSuffixComp);

        comp = colSuffixComp.getComponentPanel();
        contents.add(comp);

        outTablePane.add(contents, BorderLayout.NORTH);

        return outTablePane;
    }

    private void updateScriptLanguage() {
        final ScriptLanguage language = m_scriptService.getLanguageByName(
                (String) m_codeEditor.languageSelection().getSelectedItem());

        try (final TempClassLoader tempCl = new TempClassLoader(
                ScriptingGateway.get().createUrlClassLoader())) {
            // Hack to set language of the EditorPane: TODO Fix in
            // imagej-ui-swing!
            m_codeEditor.getCodeEditor().getEditorPane().setFileName(
                    new File("." + language.getExtensions().get(0)));
        }
    }

    private CompileProductHelper recompile(final String code,
            final String scriptLanguageName) throws InvalidSettingsException {
        try (final TempClassLoader tempCl = new TempClassLoader(
                ScriptingGateway.get().createUrlClassLoader())) {
            return m_compiler.compile(code,
                    m_scriptService.getLanguageByName(scriptLanguageName));
        } catch (final Exception e) {
            throw new InvalidSettingsException(e);
        }
    }

    @Override
    public void onOpen() {

        // create dialog component on open, to ensure that the settings are
        // already loaded.
        if (m_outputTablePanel == null) {
            m_outputTablePanel = createOutputTablePane();
            createComponent();
            addTab("Dialog", m_component);
            getPanel().revalidate();
            getPanel().repaint();
        }

        // Set language on opening to ensure all language plugins have been
        // loaded.
        final Set<String> languageSet = new LinkedHashSet<>();
        for (final ScriptLanguage lang : m_scriptService.getIndex()) {
            languageSet.add(lang.toString());
        }
        final String[] languages =
                languageSet.stream().toArray(size -> new String[size]);

        if (languages.length != 0) {
            m_codeEditor.languageSelection()
                    .setModel(new DefaultComboBoxModel<>(languages));
        } /* otherwise it stays {"Java"} */

        m_codeEditor.languageSelection()
                .setSelectedItem(m_settings.getScriptLanguageName());

        // Update settings and script language, if language is selected
        // via the combobox.
        m_codeEditor.languageSelection().addItemListener(event -> {
            m_settings.setScriptLanguageName((String) m_codeEditor
                    .languageSelection().getSelectedItem());
            updateScriptLanguage();
        });

        updateScriptLanguage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {

        // Code editor etc...
        for (final DialogComponent comp : m_codeEditor.dialogComponents()) {
            comp.saveSettingsTo(settings);
        }

        // update autogen panel
        if (m_settings.getMode() == ScriptDialogMode.SETTINGS_EDIT) {
            m_settings.setColumnInputMapping(
                    m_simpleColumnMappingService.serialize());
        }
        m_settings.saveSettingsTo(settings, m_dialogSettingsService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            m_settings.loadSettingsFrom(settings, m_dialogSettingsService,
                    true);
        } catch (final InvalidSettingsException e) {
            throw new NotConfigurableException("Can't create dialog : " + e);
        }

        if (m_settings.getMode() == ScriptDialogMode.SETTINGS_EDIT) {
            m_simpleColumnMappingService
                    .deserialize(m_settings.getColumnInputMapping());
        }

        for (final DialogComponent comp : m_codeEditor.dialogComponents()) {
            comp.loadSettingsFrom(settings, specs);
        }

        m_codeEditor.getColumnList().update(specs[0]);
    }

    /**
     * @return The currently set language for the node
     */
    protected ScriptLanguage getCurrentLanguage() {
        final String languageName = m_settings.getScriptLanguageName();
        final ScriptLanguage language =
                m_scriptService.getLanguageByName(languageName);
        if (language == null) {
            throw new NullPointerException("Could not load language "
                    + languageName + " for Scripting Node.");
        }
        return language;
    }

    /**
     * NB. Needed in the NodeDialogListener
     *
     * @return the module compiled from the code.
     */
    Module getModule() {
        return m_module;
    }

    /**
     * Listener for the mode switch button
     *
     * @author gabriel
     *
     */
    private final class ModeSwitchButtonListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {

            switch (m_settings.getMode()) {
            case CODE_EDIT: // switch to dialog
                // save code
                m_settings
                        .setScriptCode(m_codeEditor.getCodeEditor().getCode());
                m_innerPanel.removeAll();
                m_innerPanel.add(createDialogPanel(true), BorderLayout.CENTER);
                m_modeSwitchButton.setText("Switch to Code");
                m_settings.setMode(ScriptDialogMode.SETTINGS_EDIT);
                break;
            case SETTINGS_EDIT: // switch to code editor
                m_innerPanel.removeAll();
                m_innerPanel.add(createCodeEditorPanel(), BorderLayout.CENTER);
                m_modeSwitchButton.setText("Switch to Dialog");
                m_settings.setMode(ScriptDialogMode.CODE_EDIT);
                break;
            default:
                throw new IllegalArgumentException(
                        "Mode " + m_settings.getMode().toString()
                                + " is not supported");
            }
            getPanel().revalidate();
            getPanel().repaint();
        }
    }

    /**
     * We need to preprocess the the modules so that we don't try to don't try
     * to create dialog components for invalid inputs.
     *
     * @return a list of preprocessors that strip away inputs invalid for the
     *         GUI creation.
     */
    private List<PreprocessorPlugin> createPreprocessorList() {
        return Arrays.asList(ServicePreprocessor.class).stream().map(clazz -> {
            try {
                final PreprocessorPlugin tmp =
                        (PreprocessorPlugin) m_pluginService.getPlugin(clazz)
                                .createInstance();
                m_context.inject(tmp);
                return tmp;
            } catch (final InstantiableException exc) {
                getLogger().error(exc);
                return null;
            }
        }).collect(Collectors.toList());
    }
}
