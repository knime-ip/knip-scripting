package org.knime.knip.scripting.node;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.FileUtil;
import org.knime.knip.scijava.commands.adapter.OutputAdapter;
import org.knime.knip.scijava.commands.adapter.OutputAdapterService;
import org.knime.knip.scijava.commands.impl.DefaultKnimeExecutionService;
import org.knime.knip.scijava.commands.impl.KnimeInputDataTableService;
import org.knime.knip.scijava.commands.impl.KnimeOutputDataTableService;
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scripting.base.ScriptingGateway;
import org.knime.knip.scripting.matching.ColumnToModuleItemMapping;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService;
import org.knime.knip.scripting.matching.Util;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugins.scripting.java.JavaEngine;
import org.scijava.plugins.scripting.java.JavaService;
import org.scijava.script.ScriptLanguage;
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

	/* Service for passing DataTables as input to commands */
	@Parameter
	private KnimeInputDataTableService m_inService;

	/* Service for creating DataTables from command outputs */
	@Parameter
	private KnimeOutputDataTableService m_outService;

	/* Service which holds a KNIME execution context for whatever may need it. */
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
	private Class<? extends Command> m_commandClass;
	private CommandInfo m_commandInfo;

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
	}

	/* DataTableSpec of the output data table, created from module outputs */
	private DataTableSpec[] m_outSpec;

	@SuppressWarnings({ "rawtypes" })
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		// get the Service for output adapters, which convert module items to
		// Knime DataCells
		final OutputAdapterService outAdapters = m_context
				.getService(OutputAdapterService.class);

		// list to contain the output DataColumnSpecs
		final List<DataColumnSpec> columnSpecs = new ArrayList<DataColumnSpec>();

		if (m_commandClass == null) {
			throw new InvalidSettingsException("Code did not compile!");
		}

		// create a output data table spec for every module output that can be
		// adapted.
		for (final ModuleItem<?> output : m_commandInfo.outputs()) {
			final OutputAdapter oa = outAdapters
					.getMatchingOutputAdapter(output.getType());

			if (oa != null) {
				// there is a adapter to convert the contents of "output",
				// a column will be created which will contain its contents
				columnSpecs.add(new DataColumnSpecCreator(output.getName(), oa
						.getDataCellType()).createSpec());
			}
		}
		// create output table specs from column specs
		final DataTableSpec outSpec = new DataTableSpec(
				columnSpecs.toArray(new DataColumnSpec[] {}));
		m_outSpec = new DataTableSpec[] { outSpec };

		// create SettingsModels for autogenerated Settings
		for (final ModuleItem<?> i : m_commandInfo.inputs()) {
			m_settingsService.createSettingsModel(i);
		}

		return m_outSpec;
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends Command> compile(ScriptService scriptService,
			final String code, final String languageName)
			throws ScriptException, NullPointerException {

		// This is required for the compiler to find classes on classpath
		// (scijava-common for example)
		ClassLoader backup = Thread.currentThread().getContextClassLoader();

		Thread.currentThread().setContextClassLoader(
				ScriptingGateway.get().createUrlClassLoader());
		try {
			final ScriptLanguage language = scriptService
					.getLanguageByName(languageName);
			if (language == null) {
				throw new NullPointerException("Could not load language "
						+ languageName + " for Scripting node.");
			}

			ScriptEngine engine = language.getScriptEngine();

			if (engine instanceof JavaEngine) {
				return (Class<? extends Command>) ((JavaEngine) engine)
						.compile(code);
			}
			return (Class<? extends Command>) engine.eval(code);
		} finally {
			Thread.currentThread().setContextClassLoader(backup); //TODO maybe remove sometime?
		}
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		// fail if the module was not compilen in configure()
		if (m_commandClass == null) {
			throw new Exception("Code did not compile!");
		}

		final BufferedDataTable inTable = inData[0];

		final BufferedDataContainer container = exec
				.createDataContainer(m_outSpec[0]);

		/* provide the KNIME data via Scijava services */
		m_inService.setInputDataTable(inTable);
		m_outService.setOutputContainer(container);
		m_execService.setExecutionContex(exec);

		final ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				ScriptingGateway.get().getClassLoader());

		/* compile an run script for all rows */
		try {
			while (m_inService.hasNext()) {
				// check if user canceled execution of node
				exec.checkCanceled();

				m_inService.next();
				m_javaRunner.run(m_commandClass);
				m_outService.appendRow();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.out.println(e);
		} finally {
			Thread.currentThread().setContextClassLoader(previousClassLoader);
		}

		/* reset knime context services */
		container.close();
		m_inService.setInputDataTable(null);
		m_outService.setOutputContainer(null);
		m_outService.setOutputDataRow(null);

		return new BufferedDataTable[] { container.getTable() };
	}

	@Override
	protected void loadInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		/* nothing to do */
	}

	@Override
	protected void saveInternals(final File nodeInternDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		/* nothing to do */
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		// try {
		// // check if the code compiles
		// if (m_scriptEngine instanceof JavaEngine) {
		// m_commandClass = compile(
		// m_scriptService,
		// settings.getString(ScriptingNodeSettings.SM_KEY_CODE),
		// settings.getString(ScriptingNodeSettings.SM_KEY_LANGUAGE));
		// }
		// } catch (final ScriptException e) {
		// m_commandClass = null;
		// }
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_settings.loadSettingsFrom(settings);
		
		// FIXME: Ugly workaround to getting script plugins loaded.
		Thread.currentThread().setContextClassLoader(ScriptingGateway.get().createUrlClassLoader());

		ScriptLanguage lang = m_scriptService.getLanguageByName(m_settings
				.getScriptLanguageName());

		if (lang == null) {
			getLogger().error(
					"Language " + m_settings.getScriptLanguageName()
							+ " could not be found.");
			return;
		}

		try {
			File tempDir = FileUtil.createTempDir("ScriptingNode"
					+ m_settings.getNodeId());
			File scriptFile = new File(tempDir, "script."
					+ lang.getExtensions().get(0));

			Writer w = new FileWriter(scriptFile);
			w.write(m_settings.getScriptCode());
			w.close();
		} catch (IOException exc) {
			exc.printStackTrace();
		}

		// // compile to work with script-dependent settings
		// try {
		// // check if the code compiles
		// if (m_scriptEngine instanceof JavaEngine) {
		// m_commandClass = compile(m_scriptService,
		// m_settings.getScriptCode(),
		// m_settings.getScriptLanguageName());
		// }
		// } catch (final ScriptException e) {
		// m_commandClass = null;
		// e.printStackTrace(); // TODO
		// return;
		// }

		if (m_commandClass == null) {
			return;
		}

		m_commandInfo = new CommandInfo(m_commandClass,
				m_commandClass.getAnnotation(Plugin.class));

		// Create settings models for module inputs which do not have a
		// ColumnToModuleInputMapping that maps to them
		for (final ModuleItem<?> i : m_commandInfo.inputs()) {
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
			// this will just not work sometimes, if new compilation contains
			// new inputs etc
		}

		// load column input mappings
		m_cimService.clear();
		Util.fillColumnToModuleItemMappingService(
				m_settings.getColumnInputMapping(), m_cimService);
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		Util.fillStringArraySettingsModel(m_cimService,
				m_settings.columnInputMappingModel());

		m_settings.saveSettingsTo(settings);
	}

	@Override
	protected void reset() {
		// unused
	}

}
