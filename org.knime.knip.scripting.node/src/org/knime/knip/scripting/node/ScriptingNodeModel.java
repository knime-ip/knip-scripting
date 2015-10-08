package org.knime.knip.scripting.node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.FileUtil;
import org.knime.knip.scijava.commands.DefaultKnimeExecutionService;
import org.knime.knip.scijava.commands.KnimeInputDataTableService;
import org.knime.knip.scijava.commands.KnimeOutputDataTableService;
import org.knime.knip.scijava.commands.adapter.OutputAdapter;
import org.knime.knip.scijava.commands.adapter.OutputAdapterService;
import org.knime.knip.scijava.commands.mapping.ColumnToModuleItemMapping;
import org.knime.knip.scijava.commands.mapping.ColumnToModuleItemMappingService;
import org.knime.knip.scijava.commands.mapping.ColumnToModuleItemMappingUtil;
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scijava.core.TempClassLoader;
import org.knime.knip.scripting.base.ScriptingGateway;
import org.scijava.Context;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugins.scripting.java.JavaService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * NodeModel of the ScriptingNode
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public class ScriptingNodeModel extends NodeModel {

	/*
	 * This node uses own node ids for every instance during the nodes existance
	 * (node ids are not saved to file). The next id to be given to a newly
	 * created (not just created by the user!) node is stored in MAX_NODE_ID.
	 * 
	 * The node id is required so that Dialog and Model can both work with the
	 * same Scijava Context.
	 */
	private static int MAX_NODE_ID = 0;

	private final ScriptingNodeSettings m_settings = new ScriptingNodeSettings();

	/* scijava context stuff */
	final Context m_context;

	@Parameter
	private ObjectService m_objectService;

	/* java services run compiled java scripts and commands */
	@Parameter
	private JavaService m_javaRunner;
	/* service responsible for running modules */
	@Parameter
	private ModuleService m_moduleService;
	/* Service for passing DataTables as input to commands */
	@Parameter
	private KnimeInputDataTableService m_inService;

	/* Service for creating DataTables from command outputs */
	@Parameter
	private KnimeOutputDataTableService m_outService;

	/*
	 * Service which holds a KNIME execution context for whatever may need it.
	 */
	@Parameter
	private DefaultKnimeExecutionService m_execService;

	/* Service managing SettingsModels */
	@Parameter
	private NodeSettingsService m_settingsService;

	/* Service for mapping column names to module inputs */
	@Parameter
	private ColumnToModuleItemMappingService m_cimService;

	/* Service providing access to ScriptLanguages plugins */
	@Parameter
	private ScriptService m_scriptService;

	/* Current compiled command and its command info */
	private ScriptInfo m_moduleInfo;

	private final File m_tempDir;

	private final Writer m_errorWriter = new StringWriter();
	private final Writer m_outputWriter = new StringWriter();

	/**
	 * Constructor. Should only be called by {@link ScriptingNodeFactory}.
	 *
	 * @see ScriptingNodeFactory
	 */
	protected ScriptingNodeModel() {
		super(1, 1);

		// get a new Node specific Scijava Context which will be shared with the
		// Node dialog.
		m_settings.setNodeId(MAX_NODE_ID++);
		m_context = ScriptingGateway.get().getContext(m_settings.getNodeId());

		// populate @Parameter members
		m_context.inject(this);

		File dir = null;
		try {
			dir = FileUtil
					.createTempDir("ScriptingNode" + m_settings.getNodeId());
		} catch (IOException e) {
			getLogger().error(
					"Could not create temporary directory for Scripting Node.");
		}
		m_tempDir = dir;

		NodeLogger.addKNIMEConsoleWriter(m_errorWriter, NodeLogger.LEVEL.WARN,
				NodeLogger.LEVEL.ERROR);
		NodeLogger.addKNIMEConsoleWriter(m_outputWriter, NodeLogger.LEVEL.INFO,
				NodeLogger.LEVEL.DEBUG);
	}

	/* DataTableSpec of the output data table, created from module outputs */
	private DataTableSpec[] m_outSpec;

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		// get the Service for output adapters, which convert module items to
		// Knime DataCells
		final OutputAdapterService outAdapters = m_context
				.getService(OutputAdapterService.class);

		// list to contain the output DataColumnSpecs
		final List<DataColumnSpec> columnSpecs = new ArrayList<DataColumnSpec>();

		try {
			m_moduleInfo = compile(getCurrentLanguage());
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw new InvalidSettingsException("Code did not compile!", e);
		}

		// create a output data table spec for every module output that can be
		// adapted.
		for (final ModuleItem<?> output : m_moduleInfo.outputs()) {
			final OutputAdapter<?, ? extends DataCell> oa = outAdapters
					.getMatchingOutputAdapter(output.getType());

			if (oa != null) {
				// there is a adapter to convert the contents of "output",
				// a column will be created which will contain its contents
				columnSpecs.add(new DataColumnSpecCreator(output.getName(),
						DataType.getType(oa.getOutputType())).createSpec());
			}
		}
		// create output table specs from column specs
		final DataTableSpec outSpec = new DataTableSpec(
				columnSpecs.toArray(new DataColumnSpec[] {}));
		m_outSpec = new DataTableSpec[] { outSpec };

		// create SettingsModels for autogenerated Settings
		for (final ModuleItem<?> i : m_moduleInfo.inputs()) {
			m_settingsService.createSettingsModel(i);
		}

		return m_outSpec;
	}

	@SuppressWarnings("unchecked")
	public ScriptInfo compile(ScriptLanguage language) {
		writeScriptToFile(language);

		// create script module for execution
		final ScriptInfo info = new ScriptInfo(m_context,
				new File(m_tempDir, "script." + language.getExtensions().get(0))
						.getAbsolutePath(),
				new StringReader(m_settings.getScriptCode()));

		return info;
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

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		final ScriptLanguage language = getCurrentLanguage();

		m_moduleInfo = compile(language);
		// fail if the module was not compilen in configure()
		if (m_moduleInfo == null) {
			throw new Exception("Code did not compile!");
		}

		ScriptModule module = m_moduleInfo.createModule();

		m_context.inject(module);

		// use the currently selected language to execute the script
		module.setLanguage(language);

		// map stdout and stderr to the UI
		module.setOutputWriter(m_outputWriter);
		module.setErrorWriter(m_errorWriter);

		final BufferedDataTable inTable = inData[0];

		final BufferedDataContainer container = exec
				.createDataContainer(m_outSpec[0]);

		/* provide the KNIME data via Scijava services */
		m_inService.setInputDataTable(inTable);
		m_outService.setOutputContainer(container);
		m_execService.setExecutionContex(exec);

		/* compile an run script for all rows */
		try (final TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			while (m_inService.hasNext()) {
				// check if user canceled execution of node
				exec.checkCanceled();
				
				resetModule(module);

				m_inService.next();
				m_moduleService.run(module, true).get();
				m_outService.appendRow();
			}
		} catch (final Throwable e) {
			e.printStackTrace();
			System.out.println(e);
		}

		/* reset knime context services */
		container.close();
		m_inService.setInputDataTable(null);
		m_outService.setOutputContainer(null);
		m_outService.setOutputDataRow(null);

		return new BufferedDataTable[] { container.getTable() };
	}

	private void resetModule(ScriptModule m) {
		final ScriptEngine scriptEngine = m.getEngine();
		
		for (final String input : m.getInputs().keySet()) {
			m.setResolved(input, false);
		}
		for (final String output : m.getOutputs().keySet()) {
			m.setResolved(output, false);
			scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).remove(output);
		}
		//scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).clear();
		
	}

	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec)
					throws IOException, CanceledExecutionException {
		/* nothing to do */
	}

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec)
					throws IOException, CanceledExecutionException {
		/* nothing to do */
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		/* nothing to do */
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_settings.loadSettingsFrom(settings);

		try (final TempClassLoader tempCl = new TempClassLoader(
				ScriptingGateway.get().createUrlClassLoader())) {
			ScriptLanguage lang = m_scriptService
					.getLanguageByName(m_settings.getScriptLanguageName());

			if (lang == null) {
				getLogger()
						.error("Language " + m_settings.getScriptLanguageName()
								+ " could not be found.");
				return;
			}

			writeScriptToFile(lang);

			try {
				m_moduleInfo = compile(getCurrentLanguage());
			} catch (NullPointerException e) {
				e.printStackTrace();
			}

			if (m_moduleInfo == null) {
				getLogger().info(
						"Code did not compile, failed to load all settings.");
				return;
			}

			// Create settings models for module inputs which do not have a
			// ColumnToModuleInputMapping that maps to them
			for (final ModuleItem<?> i : m_moduleInfo.inputs()) {
				final String inputName = i.getName();
				boolean needsSettings = true;

				// try to find a mapping
				final ColumnToModuleItemMapping mapping = m_cimService
						.getMappingForModuleItemName(inputName);
				if (mapping != null) {
					// possibly found an active mapping.
					needsSettings = !mapping.isActive();
				}

				if (needsSettings) {
					m_settingsService.createSettingsModel(i);
				}
			}

			try {
				m_settingsService.loadSettingsFrom(settings);
			} catch (final InvalidSettingsException e) {
				// this will just not work sometimes, if new compilation
				// contains new inputs etc
			}

			// load column input mappings
			m_cimService.clear();
			ColumnToModuleItemMappingUtil.fillColumnToModuleItemMappingService(
					m_settings.getColumnInputMapping(), m_cimService);
		}
	}

	/*
	 * Write script code from the current settings to a file in the temp
	 * directory.
	 */
	private void writeScriptToFile(ScriptLanguage lang) {
		try {
			File scriptFile = new File(m_tempDir,
					"script." + lang.getExtensions().get(0));

			Writer w = new FileWriter(scriptFile);
			w.write(m_settings.getScriptCode());
			w.close();
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		ColumnToModuleItemMappingUtil.fillStringArraySettingsModel(m_cimService,
				m_settings.columnInputMappingModel());

		m_settings.saveSettingsTo(settings);
		
		m_settingsService.saveSettingsTo(settings);
	}

	@Override
	protected void reset() {
		// unused
	}

}
