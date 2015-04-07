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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.scijava.commands.adapter.OutputAdapter;
import org.knime.knip.scijava.commands.adapter.OutputAdapterService;
import org.knime.knip.scijava.commands.impl.DefaultKnimeExecutionService;
import org.knime.knip.scijava.commands.impl.KnimeInputDataTableService;
import org.knime.knip.scijava.commands.impl.KnimeOutputDataTableService;
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scijava.core.ResourceAwareClassLoader;
import org.knime.knip.scripting.matching.ColumnInputMappingKnimePreprocessor;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService;
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

	private final SettingsModelString m_codeModel = createCodeSettingsModel();

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
	private ColumnToModuleItemMapping m_cimService;

	final ScriptLanguage m_java;
	final JavaEngine m_javaEngine;

	/* Store the last used DataTableSpec here */
	private DataTableSpec m_inputSpec;

	/* Current compiled command and info */
	private Class<? extends Command> m_commandClass;
	private CommandInfo m_commandInfo;

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

	/* dependencies to be resolved for the custom class loader */
	private static final String[] DEFAULT_DEPENDENCIES = {
			"org.knime.knip.base", "org.knime.knip.core",
			"org.knime.knip.scijava" };

	protected ScriptingNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);

		log = getLogger();

		m_context = ScriptingGateway.get().getContext();

		/* add custom plugins */
		PluginService plugins = m_context.getService(PluginService.class);
		plugins.addPlugin(new PluginInfo<>(BlockingCommandJavaRunner.class,
				JavaRunner.class));
		plugins.addPlugin(new PluginInfo<>(
				DefaultColumnToModuleItemMappingService.class,
				ColumnToModuleItemMappingService.class));
		plugins.addPlugin(new PluginInfo<>(
				ColumnInputMappingKnimePreprocessor.class,
				PreprocessorPlugin.class));
		plugins.removePlugin(plugins.getPlugin(DisplayPostprocessor.class));

		/* manually load services */
		new ServiceHelper(m_context)
				.loadService(ColumnToModuleItemMappingService.class);

		m_context.inject(this);

		m_java = m_objectService.getObjects(JavaScriptLanguage.class).get(0);
		m_javaEngine = (JavaEngine) m_java.getScriptEngine();
	}

	/**
	 * Method to get the classpath for a specific eclipse project.
	 * 
	 * @param projectName
	 * @return
	 */
	protected static List<URL> getClasspathFor(String projectName) {
		ArrayList<URL> urls = new ArrayList<URL>();

		try {
			/* Add plugin binaries (.jar or bin/ folder) */
			final URL url = new URL("platform:/plugin/" + projectName + "/bin/");
			final File binFile = new File(FileLocator.resolve(url).getFile());
			urls.add(file2URL(binFile));
		} catch (Exception e) {
		}
		try {
			/* Add contents of lib folder */
			final URL libMvnUrl = new URL("platform:/plugin/" + projectName
					+ "/lib/mvn/");
			urls.addAll(getContentsOf(libMvnUrl));
		} catch (Exception e) {
		}
		try {
			/* Add contents of lib folder */
			final URL libUrl = new URL("platform:/plugin/" + projectName
					+ "/lib/");
			urls.addAll(getContentsOf(libUrl));
		} catch (Exception e) {
		}

		return urls;
	}

	private static Collection<? extends URL> getContentsOf(URL libUrl) {
		ArrayList<URL> urls = new ArrayList<URL>();
		try {
			final File libDir = new File(FileLocator.resolve(libUrl).getFile());
			if (libDir.exists() && libDir.isDirectory()) {
				File[] files = libDir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File file, String fname) {
						return fname.endsWith(".jar")
								|| fname.endsWith(".class");
					}
				});

				for (File f : files) {
					urls.add(file2URL(f));
				}
			}
		} catch (IOException e) {
			// Ignore. Some projects just don't have /lib/mvn/.
		}

		return urls;
	}

	protected static URL file2URL(File f) throws MalformedURLException {
		return new URL("file:" + f.getAbsolutePath());
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

		for (ModuleItem<?> output : m_commandInfo.outputs()) {
			OutputAdapter oa = outAdapters.getMatchingOutputAdapter(output
					.getType());

			if (oa != null) {
				columnSpecs.add(new DataColumnSpecCreator(output.getName(), oa
						.getDataCellType()).createSpec());
			}
		}

		// create SettingsModels
		for (ModuleItem<?> i : m_commandInfo.inputs()) {
			m_settingsService.createSettingsModel(i);
		}

		m_inputSpec = inSpecs[0];

		DataTableSpec outSpec = new DataTableSpec(
				columnSpecs.toArray(new DataColumnSpec[] {}));
		m_outSpec = new DataTableSpec[] { outSpec };

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
			/*
			 * TODO: we thought about getting currently loaded libraries/classes
			 * from the bungle header. Might want to investigate further some
			 * time..
			 * 
			 * BundleLoader loader = (BundleLoader) classLoader.getDelegate();
			 * 
			 * AbstractBundle bundle = loader.getBundle(); String[] classPath =
			 * bundle.getBundleData().getClassPath();
			 * 
			 * Dictionary<String, String> headers =
			 * bundle.getBundle().getHeaders();
			 */

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
//			m_cimService.loadSettingsFrom(
//					settings.getConfig(ScriptingNodeDialog.CFG_CIM_TABLE),
//					m_inputSpec, m_commandInfo);
		} catch (InvalidSettingsException e) {
			// this will just not work sometimes, if new compilation contains
			// new inputs etc
		}
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		// TODO: Always add settings models here too.
		m_codeModel.saveSettingsTo(settings);
		m_settingsService.saveSettingsTo(settings);

//		m_cimService.saveSettingsTo(settings
//				.addConfig(ScriptingNodeDialog.CFG_CIM_TABLE));
	}

	@Override
	protected void reset() {

	}
}
