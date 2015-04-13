package org.knime.knip.scripting.base;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
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
import org.knime.core.node.NodeLogger;
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
import org.knime.knip.scripting.matching.ColumnInputMappingKnimePreprocessor;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService;
import org.knime.knip.scripting.matching.Util;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService.ColumnToModuleItemMapping;
import org.knime.knip.scripting.matching.DefaultColumnToModuleItemMappingService;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.display.DisplayPostprocessor;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugins.scripting.java.JavaEngine;
import org.scijava.plugins.scripting.java.JavaRunner;
import org.scijava.plugins.scripting.java.JavaScriptLanguage;
import org.scijava.plugins.scripting.java.JavaService;
import org.scijava.script.ScriptLanguage;
import org.scijava.service.ServiceHelper;

/**
 * NodeModel of the ScriptingNode
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public class ScriptingNodeModel extends NodeModel {

	private static int MAX_NODE_ID = 0;

	private final SettingsModelString m_codeModel = createCodeSettingsModel();

	private final SettingsModelStringArray m_columnInputMappingSettignsModel = createColumnInputMappingSettingsModel();

	private final SettingsModelInteger m_nodeId = createIDSettingsModel();

	private final NodeLogger log;

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

	/*
	 * Service for converting module inputs into KNIME datatable cells vice
	 * versa
	 */
	@Parameter
	private ColumnToModuleItemMappingService m_cimService;

	final ScriptLanguage m_java;
	final JavaEngine m_javaEngine;

	/* Store the last used DataTableSpec here */
	private DataTableSpec m_inputSpec;

	/* Current compiled command and info */
	private Class<? extends Command> m_commandClass;
	private CommandInfo m_commandInfo;

	/**
	 * Create column to input mapping settings model.
	 * 
	 * @return
	 */
	public static SettingsModelStringArray createColumnInputMappingSettingsModel() {
		return new SettingsModelStringArray("ColumnInputMappings",
				new String[] {});
	}

	/**
	 * Create Code SettingsModel with some default example code.
	 * 
	 * @return
	 */
	public static SettingsModelString createCodeSettingsModel() {
		return new SettingsModelString("Code",
		/* default value */
		"package script;\n\n"

		+ "import org.scijava.plugin.Parameter;\n"
				+ "import org.scijava.plugin.Plugin;\n"
				+ "import org.scijava.ItemIO;\n"
				+ "import org.scijava.command.Command;\n\n"

				+ "@Plugin(type = Command.class)\n"
				+ "public class MyClass implements Command {\n"
				+ "		@Parameter(type = ItemIO.BOTH)\n"
				+ "		private String string;\n\n"

				+ " 	@Parameter(type = ItemIO.BOTH)\n"
				+ " 	private Integer integer;\n\n"

				+ "		public void run() {\n"
				+ "			string = \"Here's some custom output!: \" + string;\n"
				+ "			integer = integer * integer;\n" + "			return;\n"
				+ "		}\n" + "}\n");
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

	protected ScriptingNodeModel() {
		super(1, 1);

		log = getLogger();

		m_nodeId.setIntValue(MAX_NODE_ID++);
		m_context = ScriptingGateway.get().getContext(m_nodeId.getIntValue());

		m_context.inject(this);

		m_java = m_objectService.getObjects(JavaScriptLanguage.class).get(0);
		m_javaEngine = (JavaEngine) m_java.getScriptEngine();
	}

	private DataTableSpec[] m_outSpec;

	@SuppressWarnings("unchecked")
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		OutputAdapterService outAdapters = m_context
				.getService(OutputAdapterService.class);

		if (m_commandClass == null) {
			throw new InvalidSettingsException("Code could not be compiled!");
		}

		List<DataColumnSpec> columnSpecs = new ArrayList<DataColumnSpec>();

		// create a output data table spec for every module output that can be
		// adapted.
		for (ModuleItem<?> output : m_commandInfo.outputs()) {
			OutputAdapter oa = outAdapters.getMatchingOutputAdapter(output
					.getType());

			if (oa != null) {
				columnSpecs.add(new DataColumnSpecCreator(output.getName(), oa
						.getDataCellType()).createSpec());
			}
		}
		// create output table specs from column specs
		DataTableSpec outSpec = new DataTableSpec(
				columnSpecs.toArray(new DataColumnSpec[] {}));
		m_outSpec = new DataTableSpec[] { outSpec };

		// create SettingsModels for autogenerated Settings
		for (ModuleItem<?> i : m_commandInfo.inputs()) {
			m_settingsService.createSettingsModel(i);
		}

		// shortcut to specs
		m_inputSpec = inSpecs[0];

		return m_outSpec;
	}

	@SuppressWarnings("unchecked")
	public static Class<? extends Command> compile(JavaEngine engine,
			String code) {
		ResourceAwareClassLoader racl = ScriptingGateway.get().getClassLoader();

		Thread.currentThread().setContextClassLoader(
				new URLClassLoader(racl.getFileURLs().toArray(new URL[] {}),
						racl));

		try {
			return (Class<? extends Command>) engine.compile(code);
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		final BufferedDataTable inTable = inData[0];

		BufferedDataContainer container = exec
				.createDataContainer(m_outSpec[0]);

		/* provide the KNIME data via Scijava services */
		m_inService.setInputDataTable(inTable);
		m_outService.setOutputContainer(container);
		m_execService.setExecutionContex(exec);

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				ScriptingGateway.get().getClassLoader());

		/* compile an run script for all rows */
		try {
			while (m_inService.hasNext()) {
				m_inService.next();
				m_javaRunner.run(m_commandClass);
				m_outService.appendRow();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}

		Thread.currentThread().setContextClassLoader(cl);

		/* reset knime context services */
		container.close();
		// m_inService.setInputDataTable(null); // TODO: why not?
		m_outService.setOutputContainer(null);
		m_outService.setOutputDataRow(null);

		return new BufferedDataTable[] { container.getTable() };
	}

	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: Always add settings models here too.
		m_codeModel.validateSettings(settings);
		// TODO: Doesn't really make sense until m_codeModel is loaded and
		// compiled: m_settingsService.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: Always add settings models here too.
		m_codeModel.loadSettingsFrom(settings);

		m_commandClass = compile(m_javaEngine, m_codeModel.getStringValue());

		if (m_commandClass == null) {
			log.error("Compile Error!");
		} else {
			m_commandInfo = new CommandInfo(m_commandClass,
					m_commandClass.getAnnotation(Plugin.class));
		}

		try {
			m_settingsService.loadSettingsFrom(settings);
		} catch (InvalidSettingsException e) {
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
	protected void saveSettingsTo(NodeSettingsWO settings) {
		// TODO: Always add settings models here too. TODO: Better: Add a List!
		// the node id only needs to be stored for the node dialog.
		m_nodeId.saveSettingsTo(settings);

		m_codeModel.saveSettingsTo(settings);
		m_settingsService.saveSettingsTo(settings);

		Util.fillStringArraySettingsModel(m_cimService,
				m_columnInputMappingSettignsModel);
		m_columnInputMappingSettignsModel.saveSettingsTo(settings);

	}

	@Override
	protected void reset() {

	}

}
