package org.knime.knip.scripting.base;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
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

	public static final String SM_KEY_ID = "NodeId";
	public static final String SM_KEY_CODE = "Code";
	public static final String SM_KEY_LANGUAGE = "ScriptLanguage";
	public static final String SM_KEY_INPUT_MAPPING = "ColumnInputMappings";

	/* contains the nodeID. Only used during runtime. */
	private final SettingsModelInteger m_nodeId = createIDSettingsModel();

	/* contains the language to execute the script code with */
	private final SettingsModelString m_scriptLanguageModel = createScriptLanguageSettingsModel();

	/* contains the scripts code */
	private final SettingsModelString m_codeModel = createCodeSettingsModel();

	/* contains the column to input mappings */
	private final SettingsModelStringArray m_columnInputMappingSettingsModel = createColumnInputMappingSettingsModel();

	/**
	 * Create a SettingsModel for the unique ID of this NodeModel.
	 *
	 * @return the SettingsModelInteger which refers to the ID of this
	 *         NodeModel.
	 */
	public static SettingsModelInteger createIDSettingsModel() {
		return new SettingsModelInteger(SM_KEY_ID, -1);
	}

	/**
	 * Create Code SettingsModel with some default example code.
	 *
	 * @return SettignsModel for the script code
	 */
	public static SettingsModelString createCodeSettingsModel() {
		return new SettingsModelString(
				SM_KEY_CODE,
				fileAsString("platform:/plugin/org.knime.knip.scripting.base/res/DefaultScript.txt"));
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
		byte[] encoded;
		try {
			encoded = Files.readAllBytes(Paths.get(FileLocator.resolve(
					new URL(path)).toURI()));
			return new String(encoded, Charset.defaultCharset());
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	// ---- getters -----

	/**
	 * @return value of setting with key {@link #SM_KEY_ID}.
	 */
	public int getNodeId() {
		return m_nodeId.getIntValue();
	}

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
	 * @return model with key {@link #SM_KEY_ID}.
	 */
	public SettingsModelInteger nodeIdModel() {
		return m_nodeId;
	}

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
	 * @param id
	 *            value to set for setting with key {@link #SM_KEY_ID}.
	 */
	public void setNodeId(int id) {
		m_nodeId.setIntValue(id);
	}

	/**
	 * @param name
	 *            value to set for setting with key {@link #SM_KEY_LANGUAGE}.
	 */
	public void setScriptLanguageName(String name) {
		m_scriptLanguageModel.setStringValue(name);
	}

	/**
	 * @param code
	 *            value to set for setting with key {@link #SM_KEY_CODE}.
	 */
	public void setScriptCode(String code) {
		m_codeModel.setStringValue(code);
	}

	/**
	 * @param mapping
	 *            value to set for setting with key
	 *            {@link #SM_KEY_INPUT_MAPPING}.
	 */
	public void setColumnInputMapping(String[] mapping) {
		m_columnInputMappingSettingsModel.setStringArrayValue(mapping);
	}

	// ---- loading / saving ----

	public void saveSettingsTo(NodeSettingsWO settings) {
		m_nodeId.saveSettingsTo(settings);
		m_scriptLanguageModel.saveSettingsTo(settings);
		m_codeModel.saveSettingsTo(settings);
		m_columnInputMappingSettingsModel.saveSettingsTo(settings);
	}

	public void loadSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_nodeId.loadSettingsFrom(settings);
		m_scriptLanguageModel.loadSettingsFrom(settings);
		m_codeModel.loadSettingsFrom(settings);
		m_columnInputMappingSettingsModel.loadSettingsFrom(settings);
	}
}
