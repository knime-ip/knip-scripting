package org.knime.knip.scripting.base;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.eclipse.core.runtime.FileLocator;
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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.knip.scijava.commands.adapter.OutputAdapter;
import org.knime.knip.scijava.commands.adapter.OutputAdapterService;
import org.knime.knip.scijava.commands.impl.DefaultKnimeExecutionService;
import org.knime.knip.scijava.commands.impl.KnimeInputDataTableService;
import org.knime.knip.scijava.commands.impl.KnimeOutputDataTableService;
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scijava.core.ResourceAwareClassLoader;
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
import org.scijava.plugins.scripting.java.JavaScriptLanguage;
import org.scijava.plugins.scripting.java.JavaService;

/**
 * NodeModel of the ScriptingNode
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public class ScriptingNodeModel extends NodeModel {

	private static final String SM_KEY_CODE = "Code";

	/*
	 * This node uses own node ids for every instance during the nodes existance
	 * (node ids are not saved to file). The next id to be given to a newly
	 * created node is stored in MAX_NODE_ID.
	 *
	 * The node id is required so that Dialog and Model can both work with the
	 * same Scijava Context.
	 */
	private static int MAX_NODE_ID = 0;

	/* contains the scripts code */
	private final SettingsModelString m_codeModel = createCodeSettingsModel();

	/* contains the column to input mappings */
	private final SettingsModelStringArray m_columnInputMappingSettignsModel = createColumnInputMappingSettingsModel();

	/* contains the nodeID. Only used during runtime. */
	private final SettingsModelInteger m_nodeId = createIDSettingsModel();

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

	/* The class which compiles java code */
	final JavaEngine m_javaEngine;

	/* Current compiled command and its command info */
	private Class<? extends Command> m_commandClass;
	private CommandInfo m_commandInfo;

	/**
	 * Create column to input mapping settings model.
	 *
	 * @return SettingsModel for the column to input mappings
	 */
	public static SettingsModelStringArray createColumnInputMappingSettingsModel() {
		return new SettingsModelStringArray("ColumnInputMappings",
				new String[] {});
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
	 * Get the entire contents of an URL as String.
	 *
	 * @param path
	 *            url to the file to get the contents of
	 * @return contents of path as url
	 */
	private static String fileAsString(final String path) {
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

	/**
	 * Create a SettingsModel for the unique ID of this NodeModel.
	 *
	 * @return the SettingsModelInteger which refers to the ID of this
	 *         NodeModel.
	 */
	public static SettingsModelInteger createIDSettingsModel() {
		return new SettingsModelInteger("NodeId", -1);
	}

	/**
	 * Constructor. Should only be called by {@link ScriptingNodeFactory}.
	 *
	 * @see ScriptingNodeFactory
	 */
	protected ScriptingNodeModel() {
		super(1, 1);

		m_nodeId.setIntValue(MAX_NODE_ID++);
		m_context = ScriptingGateway.get().getContext(m_nodeId.getIntValue());

		// populate @Parameter members
		m_context.inject(this);

		final JavaScriptLanguage javaLanguage = m_objectService.getObjects(
				JavaScriptLanguage.class).get(0);
		m_javaEngine = (JavaEngine) javaLanguage.getScriptEngine();
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

		if (m_commandClass == null) {
			throw new InvalidSettingsException("Code did not compile!");
		}

		// list to contain the output DataColumnSpecs
		final List<DataColumnSpec> columnSpecs = new ArrayList<DataColumnSpec>();

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
	public static Class<? extends Command> compile(final JavaEngine engine,
			final String code) throws ScriptException {
		// the ResourceAwareClassLoader has access to required bundles of this
		// bundle
		final ResourceAwareClassLoader racl = ScriptingGateway.get()
				.getClassLoader();

		// This is required for the compiler to find classes on classpath
		// (scijava-common for example)
		Thread.currentThread().setContextClassLoader(
				new URLClassLoader(racl.getFileURLs().toArray(new URL[] {}),
						racl));

		return (Class<? extends Command>) engine.compile(code);
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

		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
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
		}

		Thread.currentThread().setContextClassLoader(cl);

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
		try {
			// check if the code compiles
			m_commandClass = compile(m_javaEngine,
					settings.getString(SM_KEY_CODE));
		} catch (final ScriptException e) {
			m_commandClass = null;
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_codeModel.loadSettingsFrom(settings);

		// compile to work with script-dependent settings
		try {
			m_commandClass = compile(m_javaEngine, m_codeModel.getStringValue());
		} catch (final ScriptException e) {
			m_commandClass = null;
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
		m_columnInputMappingSettignsModel.loadSettingsFrom(settings);
		m_cimService.clear();
		Util.fillColumnToModuleItemMappingService(
				m_columnInputMappingSettignsModel, m_cimService);
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		// store node ID for ScriptingNodeDialog
		m_nodeId.saveSettingsTo(settings);

		// store other settings
		m_codeModel.saveSettingsTo(settings);
		m_settingsService.saveSettingsTo(settings);

		Util.fillStringArraySettingsModel(m_cimService,
				m_columnInputMappingSettignsModel);
		m_columnInputMappingSettignsModel.saveSettingsTo(settings);
	}

	@Override
	protected void reset() {
		// unused
	}

}
