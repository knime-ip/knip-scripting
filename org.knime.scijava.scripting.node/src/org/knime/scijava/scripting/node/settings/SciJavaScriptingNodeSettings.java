package org.knime.scijava.scripting.node.settings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.scijava.commands.settings.NodeSettingsService;
import org.knime.scijava.scripting.node.SciJavaScriptingNodeModel;
import org.knime.scijava.scripting.node.ui.SciJavaScriptingNodeDialog;

/**
 * Class containing all SettingsModels for {@link SciJavaScriptingNodeDialog}
 * and {@link SciJavaScriptingNodeModel}.
 *
 * @author Jonathan Hale (University of Konstanz)
 *
 */
public class SciJavaScriptingNodeSettings {

	/**
	 * Constants for settings model keys
	 */
	public static final String SM_KEY_CODE = "Code";
	public static final String SM_KEY_LANGUAGE = "ScriptLanguage";
	public static final String SM_KEY_INPUT_MAPPING = "ColumnInputMappings";
	public static final String SM_KEY_COLUMN_CREATION_MODE = "ColumnCreationMode";
	public static final String SM_KEY_COLUMN_SUFFIX = "ColumnSuffix";
	public static final String SM_KEY_OTHER_SETTINGS = "OtherSettings";

	/* contains the language to execute the script code with */
	private final SettingsModelString m_scriptLanguageModel = createScriptLanguageSettingsModel();

	/* contains the scripts code */
	private final SettingsModelString m_codeModel = createCodeSettingsModel();

	/* contains the column to input mappings */
	private final SettingsModelStringArray m_columnInputMappingSettingsModel = createColumnInputMappingSettingsModel();

	/* contains the column creation mode */
	private final SettingsModelString m_columnCreationModeModel = createColumnCreationModeModel();

	/* contains the column suffix */
	private final SettingsModelString m_columnSuffixModel = createColumnSuffixModel(
			m_columnCreationModeModel);

	/* contains other settings which will be passed to a NodeSettingsService */
	private final Map<String, SettingsModel> m_otherSettings = new HashMap<>();

	private final List<SettingsModel> m_settingsModels;

	public SciJavaScriptingNodeSettings() {
		m_settingsModels = new ArrayList<>(5);
		m_settingsModels.add(m_scriptLanguageModel);
		m_settingsModels.add(m_codeModel);
		m_settingsModels.add(m_columnInputMappingSettingsModel);
		m_settingsModels.add(m_columnCreationModeModel);
		m_settingsModels.add(m_columnSuffixModel);
	}

	/**
	 * Create Code SettingsModel with some default example code.
	 *
	 * @return SettignsModel for the script code
	 */
	public static SettingsModelString createCodeSettingsModel() {
		return new SettingsModelString(SM_KEY_CODE, fileAsString(
				"platform:/plugin/org.knime.scijava.scripting.node/res/DefaultScript.txt"));
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
	public static SettingsModelStringArray createColumnInputMappingSettingsModel() {
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

		final SettingsModelString suffixModel = new SettingsModelString(
				SM_KEY_COLUMN_SUFFIX, "") {
			@Override
			protected void validateSettingsForModel(
					final NodeSettingsRO settings)
							throws InvalidSettingsException {
				super.validateSettingsForModel(settings);

				// don't accept an empty suffix
				if (settings.getString(SM_KEY_COLUMN_SUFFIX).equals("")
						&& columnCreationMode.getStringValue().equals(
								ColumnCreationMode.APPEND_COLUMNS.toString())) {
					throw new InvalidSettingsException(
							"Suffix can not be empty when 'Append Columns' is selected!");
				}
			}
		};

		// set inital state
		suffixModel.setEnabled(ColumnCreationMode.APPEND_COLUMNS.toString()
				.equals(columnCreationMode.getStringValue()));

		// disable suffixModel if columnCreationMode is not APPEND_COLUMNS
		columnCreationMode.addChangeListener((e) -> {
			suffixModel.setEnabled(ColumnCreationMode.APPEND_COLUMNS.toString()
					.equals(columnCreationMode.getStringValue()));
		});

		return suffixModel;
	}

	/**
	 * Get the entire contents of an URL as String.
	 *
	 * @param path
	 *            url to the file to get the contents of
	 * @return contents of path as url
	 */
	protected static String fileAsString(final String path) {
		try {
			final URL resolvedUrl = FileLocator.resolve(new URL(path));
			final byte[] bytes = Files.readAllBytes(Paths
					.get(new URI(resolvedUrl.toString().replace(" ", "%20"))));
			return new String(bytes, Charset.defaultCharset());
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return "";
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

	/**
	 * @return map of settings to pass to a {@link NodeSettingsService}.
	 */
	public Map<String, SettingsModel> otherSettings() {
		return m_otherSettings;
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
	 * Save settings to <code>settings</code>. "other settings" are <b>not
	 * loaded!</b>
	 *
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void saveSettingsTo(final NodeSettingsWO settings) {
		for (final SettingsModel model : m_settingsModels) {
			model.saveSettingsTo(settings);
		}
		// SettingsModelUtils.saveSettingsModelsMap(settings.addNodeSettings(SM_KEY_OTHER_SETTINGS),
		// m_otherSettings);
	}

	/**
	 * Save settings to <code>settings</code>. "other settings" are <b>not
	 * saved!</b>
	 *
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void loadSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel model : m_settingsModels) {
			model.loadSettingsFrom(settings);
		}
		// m_otherSettings =
		// SettingsModelUtils.loadSettingsModelsMap(settings.getNodeSettings(SM_KEY_OTHER_SETTINGS));
	}

	public void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		for (final SettingsModel model : m_settingsModels) {
			model.validateSettings(settings);
		}
	}
}
