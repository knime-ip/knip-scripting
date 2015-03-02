package org.knime.knip.scripting.base;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.knime.knip.scijava.bridge.KnimeExecutionService;
import org.knime.knip.scijava.bridge.adapter.InputAdapterService;
import org.knime.knip.scijava.bridge.adapter.OutputAdapter;
import org.knime.knip.scijava.bridge.adapter.OutputAdapterService;
import org.knime.knip.scijava.bridge.impl.DefaultKnimeExecutionService;
import org.knime.knip.scijava.bridge.impl.KnimeInputDataTableService;
import org.knime.knip.scijava.bridge.impl.KnimeOutputDataTableService;
import org.knime.knip.scijava.bridge.settings.NodeSettingsService;
import org.knime.knip.scijava.bridge.widget.DialogWidgetService;
import org.knime.knip.scripting.matching.ColumnInputMatchingKnimePreprocessor;
import org.knime.knip.scripting.matching.ColumnInputMatchingList;
import org.knime.knip.scripting.matching.ColumnInputMatchingsService;
import org.knime.knip.scripting.matching.DefaultColumnInputMatchingsService;
import org.knime.knip.scripting.util.ClassLoaderManager;
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
import org.scijava.script.ScriptService;
import org.scijava.service.ServiceHelper;
import org.scijava.ui.DefaultUIService;
import org.scijava.widget.DefaultWidgetService;

/**
 * NodeModel of the ScriptingNode
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public class ScriptingNodeModel extends NodeModel {

	private final SettingsModelString m_codeModel = createCodeSettingsModel();

	private final NodeLogger logger;

	/* scijava context stuff */
	final Context m_context;
	
	@Parameter
	private ObjectService m_objectService;

	@Parameter
	private JavaService m_javaRunner;
	
	@Parameter
	private KnimeInputDataTableService m_inService;
	
	@Parameter
	private KnimeOutputDataTableService m_outService;
	
	@Parameter
	private DefaultKnimeExecutionService m_execService;
	
	@Parameter
	private NodeSettingsService m_settingsService;
	
	@Parameter
	private ColumnInputMatchingsService m_cimService;
	
	
	final ScriptLanguage m_java;
	final JavaEngine m_javaEngine;
	final ClassLoaderManager m_clManager;
	
	/* config cache */
	private DataTableSpec m_inputSpec;
	
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

		+ "public class MyClass implements Command {\n"
		+ "		@Parameter(type = ItemIO.BOTH)\n"
		+ "		private String string;\n\n"

		+ " 	@Parameter(type = ItemIO.BOTH)\n"
		+ " 	private Integer integer;\n\n"

		+ "		public void run() {\n"
		+ "			string = \"Here's some custom output!: \" + string;\n"
		+ "			integer = integer * integer;\n"
		+ "			return;\n"
		+ "		}\n"
		+ "}\n");
	}

	private static final String[] DEFAULT_DEPENDENCIES = {
			"org.knime.knip.base", "org.knime.knip.core",
			"org.knime.knip.scijava" };

	/**
	 * Create a Scijava Context
	 * @return
	 */
	public static Context createContext() {
		return new Context(ScriptService.class, JavaService.class,
				KnimeInputDataTableService.class,
				KnimeOutputDataTableService.class,
				KnimeExecutionService.class, NodeSettingsService.class,
				ObjectService.class, DefaultWidgetService.class, DialogWidgetService.class,
				InputAdapterService.class, OutputAdapterService.class, DefaultUIService.class); //TODO DefaultUIService requires loads of stuffs
	}
	
	public static ClassLoaderManager createClassLoaderManager() {
		/* create classLoaderManager */

		/* add libraries to its class path */
		ArrayList<URL> urls = new ArrayList<URL>();

		for (String project : DEFAULT_DEPENDENCIES) {
			urls.addAll(getClasspathFor(project));
		}

		URL[] urlArray = urls.toArray(new URL[]{});
		
		ClassLoaderManager clManager = new ClassLoaderManager(urlArray);
		
		clManager.resetClassLoader();
		
		return clManager;
	}
	
	protected ScriptingNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);

		logger = getLogger();

		
		m_context = createContext();
		
		/* add custom plugins */
		PluginService plugins = m_context.getService(PluginService.class);
		plugins.addPlugin(new PluginInfo<>(BlockingCommandJavaRunner.class,
				JavaRunner.class));
		plugins.addPlugin(new PluginInfo<>(DefaultColumnInputMatchingsService.class, 
				ColumnInputMatchingsService.class));
		plugins.addPlugin(new PluginInfo<>(ColumnInputMatchingKnimePreprocessor.class,
				PreprocessorPlugin.class));
		plugins.removePlugin(plugins.getPlugin(DisplayPostprocessor.class));
		
		/* manually load services */
		new ServiceHelper(m_context).loadService(ColumnInputMatchingsService.class);
		
		m_context.inject(this);
		
		m_java = m_objectService.getObjects(JavaScriptLanguage.class).get(0);
		m_javaEngine = (JavaEngine) m_java.getScriptEngine();		
		
		m_clManager = createClassLoaderManager();
	}

	protected static List<URL> getClasspathFor(String projectName) {
		ArrayList<URL> urls = new ArrayList<URL>();

		try {
			/* Add plugin binaries (.jar or bin/ folder) */
			final URL url = new URL("platform:/plugin/" + projectName + "/bin/");
			final File binFile = new File(FileLocator.resolve(url).getFile());
//			logger.debug("[LOAD] " + binFile.getAbsolutePath());
			urls.add(file2URL(binFile));
		} catch (Exception e) {
//			logger.error("Could not form URL for project \"" + projectName
//					+ "\"");
		}
		try {
			/* Add contents of lib folder */
			final URL libMvnUrl = new URL("platform:/plugin/" + projectName
					+ "/lib/mvn/");
			urls.addAll(getContentsOf(libMvnUrl));
		} catch (Exception e) {
//			logger.error("Could not form URL lib/mvn folder for project \""
//					+ projectName + "\"");
		}
		try {
			/* Add contents of lib folder */
			final URL libUrl = new URL("platform:/plugin/" + projectName
					+ "/lib/");
			urls.addAll(getContentsOf(libUrl));
		} catch (Exception e) {
//			logger.error("Could not form URL for lib folder in project \""
//					+ projectName + "\"");
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
//					logger.debug("[LOAD] " + f.getAbsolutePath());
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
		for(ModuleItem<?> i : m_commandInfo.inputs()) {
			m_settingsService.createSettingsModel(i);
		}

		m_inputSpec = inSpecs[0];
		
		DataTableSpec outSpec = new DataTableSpec(
				columnSpecs.toArray(new DataColumnSpec[] {}));
		m_outSpec = new DataTableSpec[] { outSpec };

		m_clManager.resetClassLoader();
		return m_outSpec;
	}
	
	@SuppressWarnings("unchecked")
	private Class<? extends Command> compile() {
		m_clManager.setURLClassLoader();
	
		try {
			return (Class<? extends Command>) m_javaEngine
					.compile(m_codeModel.getStringValue());
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

		m_inService.setInputDataTable(inTable);
		m_outService.setOutputContainer(container);
		m_execService.setExecutionContex(exec);

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

		container.close();
//		m_inService.setInputDataTable(null);
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
		// TODO: Doesn't really make sense until m_codeModel is loaded and compiled: m_settingsService.validateSettings(settings);
	}
	
	ColumnInputMatchingList m_columnInputMatchings = new ColumnInputMatchingList();

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: Always add settings models here too.
		m_codeModel.loadSettingsFrom(settings);
		
		m_commandClass = compile();
		
		if (m_commandClass == null) {
			System.out.println("Compile Error!");
		} else {
			m_commandInfo = new CommandInfo(m_commandClass, m_commandClass.getAnnotation(Plugin.class));
		}
		
		try {
			m_settingsService.loadSettingsFrom(settings);
			m_columnInputMatchings.loadSettingsForDialog(settings.getConfig(ScriptingNodeDialog.CFG_CIM_TABLE), m_inputSpec, m_commandInfo);
		} catch (InvalidSettingsException e) {
			//this will just not work sometimes, if new compilation contains new inputs etc
		}
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		// TODO: Always add settings models here too.
		m_codeModel.saveSettingsTo(settings);
		m_settingsService.saveSettingsTo(settings);
		
		m_columnInputMatchings.saveSettingsForDialog(settings.addConfig(ScriptingNodeDialog.CFG_CIM_TABLE)); //TODO temp
	}

	@Override
	protected void reset() {
		
	}
}
