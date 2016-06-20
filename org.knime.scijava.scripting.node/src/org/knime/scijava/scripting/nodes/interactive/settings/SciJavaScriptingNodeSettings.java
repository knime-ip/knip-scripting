package org.knime.scijava.scripting.nodes.interactive.settings;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.scijava.commands.settings.NodeSettingsService;
import org.knime.scijava.scripting.nodes.interactive.SciJavaScriptingNodeModel;
import org.knime.scijava.scripting.nodes.interactive.ui.SciJavaScriptingNodeDialog;
import org.knime.scijava.scripting.util.ScriptUtils;

/**
 * Class containing all SettingsModels for {@link SciJavaScriptingNodeDialog}
 * and {@link SciJavaScriptingNodeModel}.
 *
 * @author Jonathan Hale (University of Konstanz)
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */
public class SciJavaScriptingNodeSettings {

    /**
     * Constants for settings model keys
     */
    public static final String SM_KEY_CODE = "Code";
    public static final String SM_KEY_LANGUAGE = "ScriptLanguage";
    public static final String SM_KEY_INPUT_MAPPING = "ColumnInputMappings";
    public static final String SM_KEY_COLUMN_CREATION_MODE =
            "ColumnCreationMode";
    public static final String SM_KEY_COLUMN_SUFFIX = "ColumnSuffix";
    public static final String SM_KEY_OTHER_SETTINGS = "OtherSettings";
    public static final String SM_KEY_EDITMODE = "EditorMode";

    /* contains the mode (code / dialog ) of the node */
    private final SettingsModelString m_editModeModel = createEditModeModel();

    /* contains the language to execute the script code with */
    private final SettingsModelString m_scriptLanguageModel =
            createScriptLanguageSettingsModel();

    /* contains the scripts code */
    private final SettingsModelString m_codeModel = createCodeSettingsModel();

    /* contains the column to input mappings */
    private final SettingsModelStringArray m_columnInputMappingSettingsModel =
            createColumnInputMappingSettingsModel();

    /* contains the column creation mode */
    private final SettingsModelString m_columnCreationModeModel =
            createColumnCreationModeModel();

    /* contains the column suffix */
    private final SettingsModelString m_columnSuffixModel =
            createColumnSuffixModel(m_columnCreationModeModel);

    private final List<SettingsModel> m_dialogSettingsModels;
    private final List<SettingsModel> m_codeEditSettingsModels;

    public SciJavaScriptingNodeSettings() {

        m_dialogSettingsModels = new ArrayList<>();
        m_dialogSettingsModels.add(m_columnInputMappingSettingsModel);
        m_dialogSettingsModels.add(m_columnCreationModeModel);
        m_dialogSettingsModels.add(m_columnSuffixModel);

        m_codeEditSettingsModels = new ArrayList<>();
        m_codeEditSettingsModels.add(m_scriptLanguageModel);
        m_codeEditSettingsModels.add(m_codeModel);
    }

    /**
     * @return SettingsModel to store if the node dialog is in edit mode or not.
     */
    public static SettingsModelString createEditModeModel() {
        return new SettingsModelString(SM_KEY_EDITMODE,
                ScriptDialogMode.CODE_EDIT.toString());
    }

    /**
     * Create Code SettingsModel with some default example code.
     *
     * @return SettignsModel for the script code
     */
    public static SettingsModelString createCodeSettingsModel() {
        return new SettingsModelString(SM_KEY_CODE, ScriptUtils.fileAsString(
                "platform:/plugin/org.knime.scijava.scripting.nodes/res/DefaultScript.txt"));
    }

    /**
     * Create column to input mapping settings model.
     *
     * @return SettingsModel for the column to input mappings
     */
    public static SettingsModelString createScriptLanguageSettingsModel() {
        return new SettingsModelString(SM_KEY_LANGUAGE, "Java");
    }

    /**
     * Create column to input mapping settings model.
     *
     * @return SettingsModel for the column to input mappings
     */
    public static SettingsModelStringArray
            createColumnInputMappingSettingsModel() {
        return new SettingsModelStringArray(SM_KEY_INPUT_MAPPING,
                new String[] {});
    }

    /**
     * Create column creation mode SettingsModel with some default
     * {@link ColumnCreationMode#NEW_TABLE}.
     *
     * @return SettignsModel for the column creation mode
     */
    public static SettingsModelString createColumnCreationModeModel() {
        return new SettingsModelString(SM_KEY_COLUMN_CREATION_MODE,
                ColumnCreationMode.NEW_TABLE.toString());
    }

    /**
     * Create column suffix SettingsModel with default <code>""</code>.
     *
     * @return SettingsModel for the column creation mode
     */
    public static SettingsModelString createColumnSuffixModel(
            final SettingsModelString columnCreationMode) {

        final SettingsModelString suffixModel =
                new SettingsModelString(SM_KEY_COLUMN_SUFFIX, "");

        // set inital state
        suffixModel.setEnabled(ColumnCreationMode.APPEND_COLUMNS.toString()
                .equals(columnCreationMode.getStringValue()));

        // disable suffixModel if columnCreationMode is not APPEND_COLUMNS
        columnCreationMode.addChangeListener(e -> suffixModel
                .setEnabled(ColumnCreationMode.APPEND_COLUMNS.toString()
                        .equals(columnCreationMode.getStringValue())));

        return suffixModel;
    }

    // ---- getters -----

    /**
     * @return value of setting with key {@link #SM_KEY_LANGUAGE}.
     */
    public String getScriptLanguageName() {
        return m_scriptLanguageModel.getStringValue();
    }

    /**
     * @return value of setting with key {@link #SM_KEY_CODE}.
     */
    public String getScriptCode() {
        return m_codeModel.getStringValue();
    }

    /**
     * @return value of setting with key {@link #SM_KEY_INPUT_MAPPING}.
     */
    public String[] getColumnInputMapping() {
        return m_columnInputMappingSettingsModel.getStringArrayValue();
    }

    /**
     * @return value of setting with key {@link #SM_KEY_COLUMN_CREATION_MODE}.
     */
    public ColumnCreationMode getColumnCreationMode() {
        return ColumnCreationMode
                .fromString(m_columnCreationModeModel.getStringValue());
    }

    /**
     * @return value of setting with key {@link #SM_KEY_COLUMN_SUFFIX}.
     */
    public String getColumnSuffix() {
        return m_columnSuffixModel.getStringValue();
    }

    // ---- access to models ----

    /**
     * @return model with key {@link #SM_KEY_LANGUAGE}.
     */
    public SettingsModelString getScriptLanguageNameModel() {
        return m_scriptLanguageModel;
    }

    /**
     * @return model with key {@link #SM_KEY_CODE}.
     */
    public SettingsModelString getScriptCodeModel() {
        return m_codeModel;
    }

    /**
     * @return model with key {@link #SM_KEY_INPUT_MAPPING}.
     */
    public SettingsModelStringArray getColumnInputMappingModel() {
        return m_columnInputMappingSettingsModel;
    }

    /**
     * @return model with key {@link #SM_KEY_COLUMN_CREATION_MODE}.
     */
    public SettingsModelString getColumnCreationModeModel() {
        return m_columnCreationModeModel;
    }

    /**
     * @return model with key {@link #SM_KEY_COLUMN_SUFFIX}.
     */
    public SettingsModelString getColumnSuffixModel() {
        return m_columnSuffixModel;
    }

    // ---- setters ----

    /**
     * @param name
     *            value to set for setting with key {@link #SM_KEY_LANGUAGE}.
     */
    public void setScriptLanguageName(final String name) {
        m_scriptLanguageModel.setStringValue(name);
    }

    /**
     * @param code
     *            value to set for setting with key {@link #SM_KEY_CODE}.
     */
    public void setScriptCode(final String code) {
        m_codeModel.setStringValue(code);
    }

    /**
     * @param mapping
     *            value to set for setting with key
     *            {@link #SM_KEY_INPUT_MAPPING}.
     */
    public void setColumnInputMapping(final String[] mapping) {
        m_columnInputMappingSettingsModel.setStringArrayValue(mapping);
    }

    /**
     * @mode value to set for settings with key
     *       {@link #SM_KEY_COLUMN_CREATION_MODE}.
     */
    public void setColumnCreationMode(final ColumnCreationMode mode) {
        m_columnCreationModeModel.setStringValue(mode.toString());
    }

    /**
     * @mode value to set for settings with key {@link #SM_KEY_COLUMN_SUFFIX}.
     */
    public void setColumnSuffix(final String suffix) {
        m_columnSuffixModel.setStringValue(suffix);
    }

    // ---- loading / saving / validating ----

    /**
     * Save settings from the SettingsModels and the Services to the settings
     * object.
     *
     * @param settings
     *            the Settings
     * @param service
     *            the service
     */
    public void saveSettingsTo(final NodeSettingsWO settings,
            final NodeSettingsService service) {

        m_editModeModel.saveSettingsTo(settings);
        if (getMode() == ScriptDialogMode.CODE_EDIT) {
            for (final SettingsModel model : m_codeEditSettingsModels) {
                model.saveSettingsTo(settings);
            }
        } else {
            for (final SettingsModel model : m_dialogSettingsModels) {
                model.saveSettingsTo(settings);
            }
            for (final SettingsModel model : m_codeEditSettingsModels) {
                model.saveSettingsTo(settings);
            }
            service.saveSettingsTo(
                    settings.addNodeSettings(SM_KEY_OTHER_SETTINGS));
        }
    }

    /**
     * Load settings into the Settingsmodels and the NodeSettingsService.
     *
     * @param settings
     *            the settings
     * @param settingsService
     *            the settingsService
     * @param tolerant
     *            if the loading fails on errors or not
     * @throws InvalidSettingsException
     *             if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final NodeSettingsService settingsService, final boolean tolerant)
            throws InvalidSettingsException {
        // in Editmode, load only the coding settings
        m_editModeModel.loadSettingsFrom(settings);

        if (getMode() == ScriptDialogMode.CODE_EDIT) {
            for (final SettingsModel model : m_codeEditSettingsModels) {
                model.loadSettingsFrom(settings);
            }
        } else {
            // in the dialog mode load all settings
            for (final SettingsModel model : m_dialogSettingsModels) {
                model.loadSettingsFrom(settings);
            }
            for (final SettingsModel model : m_codeEditSettingsModels) {
                model.loadSettingsFrom(settings);
            }
            settingsService.loadSettingsFrom(
                    settings.getNodeSettings(SM_KEY_OTHER_SETTINGS), tolerant);
        }

    }

    public ScriptDialogMode getMode() {
        return ScriptDialogMode.fromString(m_editModeModel.getStringValue());
    }

    /**
     * Validate the Settingsmodels.
     *
     * @param settings
     *            the settings
     * @throws InvalidSettingsException
     *             if the settings are invalid
     */
    public void validateSettings(final NodeSettingsRO settings,
            final NodeSettingsService settingsService)
            throws InvalidSettingsException {

        m_editModeModel.validateSettings(settings);
        m_editModeModel.loadSettingsFrom(settings); // slightly HACKY!!
        if (getMode() == ScriptDialogMode.CODE_EDIT) {
            for (final SettingsModel model : m_codeEditSettingsModels) {
                model.validateSettings(settings);
            }
        } else {
            for (final SettingsModel model : m_dialogSettingsModels) {
                model.validateSettings(settings);
            }
            settingsService.validateSettings(
                    settings.getNodeSettings(SM_KEY_OTHER_SETTINGS));
        }
    }

    public void setMode(final ScriptDialogMode codeEdit) {
        m_editModeModel.setStringValue(codeEdit.toString());
    }
}
