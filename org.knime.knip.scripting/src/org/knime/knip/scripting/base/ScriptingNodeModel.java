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
import org.knime.knip.scijava.bridge.KnimeExecutionService;
import org.knime.knip.scijava.bridge.adapter.InputAdapterService;
import org.knime.knip.scijava.bridge.adapter.OutputAdapter;
import org.knime.knip.scijava.bridge.adapter.OutputAdapterService;
import org.knime.knip.scijava.bridge.impl.DefaultKnimeExecutionService;
import org.knime.knip.scijava.bridge.impl.KnimeInputDataTableService;
import org.knime.knip.scijava.bridge.impl.KnimeOutputDataTableService;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugins.scripting.java.JavaEngine;
import org.scijava.plugins.scripting.java.JavaRunner;
import org.scijava.plugins.scripting.java.JavaScriptLanguage;
import org.scijava.plugins.scripting.java.JavaService;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptService;
import org.scijava.widget.WidgetService;

public class ScriptingNodeModel extends NodeModel {

	private final SettingsModelString m_codeModel = createCodeSettingsModel();

	private NodeLogger logger;

	/* scijava context stuff */
	final Context m_context;
	final ObjectService m_objectService;
	final ScriptLanguage m_java;
	final JavaEngine m_javaEngine;
	final JavaService m_javaRunner;

	final ClassLoaderManager m_clManager;

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
				+ "			string = \"Here's some custom output!:\" + string;\n"
				+ "			integer = integer * integer;\n" + "			return;\n"
				+ "		}\n" + "}\n");
	}

	private static final String[] DEFAULT_DEPENDENCIES = {
			"org.knime.knip.base", "org.knime.knip.core",
			"org.knime.knip.scijava" };

	protected ScriptingNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);

		logger = getLogger();

		m_context = new Context(ScriptService.class, JavaService.class,
				KnimeInputDataTableService.class,
				KnimeOutputDataTableService.class,
				KnimeExecutionService.class,
				ObjectService.class, WidgetService.class,
				InputAdapterService.class, OutputAdapterService.class);
		
		/* add custom CommandRunner Plugin */
		PluginService plugins = m_context.getService(PluginService.class);
		plugins.addPlugin(new PluginInfo<>(BlockingCommandJavaRunner.class,
						JavaRunner.class));
		
		m_objectService = m_context.getService(ObjectService.class);
		m_java = m_objectService.getObjects(JavaScriptLanguage.class).get(0);
		m_javaEngine = (JavaEngine) m_java.getScriptEngine();		
		m_javaRunner = m_context.getService(JavaService.class);
		
		/* create classLoaderManager */

		/* add libraries to its class path */
		ArrayList<URL> urls = new ArrayList<URL>();

		for (String project : DEFAULT_DEPENDENCIES) {
			urls.addAll(getClasspathFor(project));
		}

		URL[] urlArray = urls.toArray(new URL[] {});
		
		m_clManager = new ClassLoaderManager(urlArray);
		
		m_clManager.resetClassLoader();
	}

	private static class ClassLoaderManager {
		ClassLoader m_classLoader;

		URLClassLoader m_urlClassLoader;

		public ClassLoaderManager(URL[] urls) {
			setClassLoader(urls);
		}

		public void setClassLoader(URL[] urls) {
			m_classLoader = Thread.currentThread().getContextClassLoader();
			m_urlClassLoader = new URLClassLoader(urls, getClass()
					.getClassLoader());

			setURLClassLoader();
		}

		public void setURLClassLoader() {
			Thread.currentThread().setContextClassLoader(m_urlClassLoader);
		}

		public void resetClassLoader() {
			Thread.currentThread().setContextClassLoader(m_classLoader);
		}
	}

	protected List<URL> getClasspathFor(String projectName) {
		ArrayList<URL> urls = new ArrayList<URL>();

		try {
			/* Add plugin binaries (.jar or bin/ folder) */
			final URL url = new URL("platform:/plugin/" + projectName + "/bin/");
			final File binFile = new File(FileLocator.resolve(url).getFile());
			logger.debug("[LOAD] " + binFile.getAbsolutePath());
			urls.add(file2URL(binFile));
		} catch (Exception e) {
			logger.error("Could not form URL for project \"" + projectName
					+ "\"");
		}
		try {
			/* Add contents of lib folder */
			final URL libMvnUrl = new URL("platform:/plugin/" + projectName
					+ "/lib/mvn/");
			urls.addAll(getContentsOf(libMvnUrl));
		} catch (Exception e) {
			logger.error("Could not form URL lib/mvn folder for project \""
					+ projectName + "\"");
		}
		try {
			/* Add contents of lib folder */
			final URL libUrl = new URL("platform:/plugin/" + projectName
					+ "/lib/");
			urls.addAll(getContentsOf(libUrl));
		} catch (Exception e) {
			logger.error("Could not form URL for lib folder in project \""
					+ projectName + "\"");
		}

		return urls;
	}

	private Collection<? extends URL> getContentsOf(URL libUrl) {
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
					logger.debug("[LOAD] " + f.getAbsolutePath());
					urls.add(file2URL(f));
				}
			}
		} catch (IOException e) {
			// Ignore. Some projects just don't have /lib/mvn/.
		}

		return urls;
	}

	protected URL file2URL(File f) throws MalformedURLException {
		return new URL("file:" + f.getAbsolutePath());
	}

	private DataTableSpec[] m_outSpec;

	@SuppressWarnings("unchecked")
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		Class<? extends Command> commandClass = null;

		m_clManager.setURLClassLoader();

		try {
			commandClass = (Class<? extends Command>) m_javaEngine
					.compile(m_codeModel.getStringValue());
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		final CommandInfo info = new CommandInfo(commandClass,
				commandClass.getAnnotation(Plugin.class));

		OutputAdapterService outAdapters = m_context
				.getService(OutputAdapterService.class);

		List<DataColumnSpec> columnSpecs = new ArrayList<DataColumnSpec>();

		for (ModuleItem output : info.outputs()) {
			OutputAdapter oa = outAdapters.getMatchingOutputAdapter(output
					.getType());

			if (oa != null) {
				columnSpecs.add(new DataColumnSpecCreator(output.getName(), oa
						.getDataCellType()).createSpec());
			}
		}

		DataTableSpec outSpec = new DataTableSpec(
				columnSpecs.toArray(new DataColumnSpec[] {}));
		m_outSpec = new DataTableSpec[] { outSpec };

		m_clManager.resetClassLoader();
		return m_outSpec;
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		final BufferedDataTable inTable = inData[0];

		BufferedDataContainer container = exec
				.createDataContainer(m_outSpec[0]);

		/* get Input and Ouput Services and set input and output */
		final KnimeInputDataTableService inService = m_context
				.getService(KnimeInputDataTableService.class);
		final KnimeOutputDataTableService outService = m_context
				.getService(KnimeOutputDataTableService.class);
		final DefaultKnimeExecutionService execService = m_context
				.getService(DefaultKnimeExecutionService.class);

		inService.setInputDataTable(inTable);
		outService.setOutputContainer(container);
		execService.setExecutionContex(exec);

		m_clManager.setURLClassLoader();

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
			Class<?> o = m_javaEngine.compile(m_codeModel.getStringValue());

			while (inService.hasNext()) {
				inService.next();
				m_javaRunner.run(o);
				outService.appendRow();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		} finally {
			m_clManager.resetClassLoader();
		}

		container.close();
		outService.setOutputContainer(null);
		outService.setOutputDataRow(null);

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
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// TODO: Always add settings models here too.
		m_codeModel.loadSettingsFrom(settings);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		// TODO: Always add settings models here too.
		m_codeModel.saveSettingsTo(settings);
	}

	@Override
	protected void reset() {
		
	}
}
