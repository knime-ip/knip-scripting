package org.knime.knip.scripting.node;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * Class containing all SettingsModels for {@link ScriptingNodeDialog} and
 * {@link ScriptingNodeModel}.
 *
 * @author Jonathan Hale (University of Konstanz)
 *
 */
public class ScriptingNodeSettings {

	public static final String SM_KEY_CODE = "Code";
	public static final String SM_KEY_LANGUAGE = "ScriptLanguage";
	public static final String SM_KEY_INPUT_MAPPING = "ColumnInputMappings";

	/* contains the language to execute the script code with */
	private final SettingsModelString m_scriptLanguageModel = createScriptLanguageSettingsModel();

	/* contains the scripts code */
	private final SettingsModelString m_codeModel = createCodeSettingsModel();

	/* contains the column to input mappings */
	private final SettingsModelStringArray m_columnInputMappingSettingsModel = createColumnInputMappingSettingsModel();

	private final Map<String, SettingsModel> m_otherSettings = new HashMap<String, SettingsModel>();

	/**
	 * Create Code SettingsModel with some default example code.
	 *
	 * @return SettignsModel for the script code
	 */
	public static SettingsModelString createCodeSettingsModel() {
		return new SettingsModelString(SM_KEY_CODE, fileAsString(
				"platform:/plugin/org.knime.knip.scripting.node/res/DefaultScript.txt"));
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

	// ---- access to models ----

	/**
	 * @return model with key {@link #SM_KEY_LANGUAGE}.
	 */
	public SettingsModelString scriptLanguageNameModel() {
		return m_scriptLanguageModel;
	}

	/**
	 * @return model with key {@link #SM_KEY_CODE}.
	 */
	public SettingsModelString scriptCodeModel() {
		return m_codeModel;
	}

	/**
	 * @return model with key {@link #SM_KEY_INPUT_MAPPING}.
	 */
	public SettingsModelStringArray columnInputMappingModel() {
		return m_columnInputMappingSettingsModel;
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

	public Map<String, SettingsModel> otherSettings() {
		return m_otherSettings;
	}

	// ---- loading / saving ----

	/**
	 * Save settings to <code>settings</code>. "other settings" are <b>not
	 * loaded!</b>
	 *
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void saveSettingsTo(final NodeSettingsWO settings) {
		m_scriptLanguageModel.saveSettingsTo(settings);
		m_codeModel.saveSettingsTo(settings);
		m_columnInputMappingSettingsModel.saveSettingsTo(settings);
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
		m_scriptLanguageModel.loadSettingsFrom(settings);
		m_codeModel.loadSettingsFrom(settings);
		m_columnInputMappingSettingsModel.loadSettingsFrom(settings);
	}
}
