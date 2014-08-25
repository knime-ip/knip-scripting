package org.knime.knip.scripting.base;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.knip.scijava.bridge.adapter.InputAdapterService;
import org.knime.knip.scijava.bridge.adapter.OutputAdapterService;
import org.knime.knip.scijava.bridge.impl.KnimeInputDataTableService;
import org.knime.knip.scijava.bridge.impl.KnimeOutputDataTableService;
import org.scijava.Context;
import org.scijava.object.ObjectService;
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

	protected ScriptingNodeModel() {
		super(1, 1);

		logger = getLogger();
	}

	private final SettingsModelString m_codeModel = createCodeSettingsModel();

	private NodeLogger logger;

	/**
	 * Create Code SettingsModel with some default example code.
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
		+ " 	@Parameter(type = ItemIO.BOTH)\n"
		+ " 	private String string;\n\n"
		
		+ " 	@Parameter(type = ItemIO.BOTH)\n"
		+ " 	private Integer integer;\n\n"
		
		+ "		public void run() {\n"
		+ "			string = \"Here's some custom output!:\" + string;\n"
		+ "			integer = integer * integer;\n"
		+ "			return;\n"
		+ "		}\n"
		+ "}\n");
	}

	private static final String[] DEFAULT_DEPENDENCIES = {
			"org.knime.knip.base", "org.knime.knip.core",
			"org.knime.knip.scijava" };

	private static class ClassLoaderManager {
		ClassLoader m_classLoader;

		public ClassLoaderManager() {

		}

		public void setClassLoader(URL[] urls) {
			m_classLoader = Thread.currentThread().getContextClassLoader();

			Thread.currentThread().setContextClassLoader(
					new URLClassLoader(urls, getClass().getClassLoader()));
		}

		public void resetClassLoader() {
			Thread.currentThread().setContextClassLoader(m_classLoader);
		}
	}

	private class JavaBinFileFilter implements FilenameFilter {
		@Override
		public boolean accept(File file, String fname) {
			return fname.endsWith(".jar") || fname.endsWith(".class");
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

			/* Add contents of lib folder */
			final URL libMvnUrl = new URL("platform:/plugin/" + projectName
					+ "/lib/mvn/");
			urls.addAll(getContentsOf(libMvnUrl));

			/* Add contents of lib folder */
			final URL libUrl = new URL("platform:/plugin/" + projectName
					+ "/lib/");
			urls.addAll(getContentsOf(libUrl));

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Could not form URL for project \"" + projectName + "\"");
		}

		return urls;
	}

	private Collection<? extends URL> getContentsOf(URL libUrl) {
		ArrayList<URL> urls = new ArrayList<URL>();
		try {
			final File libDir = new File(FileLocator.resolve(libUrl).getFile());
			if (libDir.exists() && libDir.isDirectory()) {
				for (File f : libDir.listFiles(new JavaBinFileFilter())) {
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

	
	protected ScriptingNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}


	private DataTableSpec[] m_outSpec;
	
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {
		DataColumnSpecCreator screator = new DataColumnSpecCreator("OutString", StringCell.TYPE);
		DataColumnSpecCreator icreator = new DataColumnSpecCreator("OutInteger", IntCell.TYPE);
		DataTableSpec outSpec = new DataTableSpec(screator.createSpec(), icreator.createSpec());
		
		m_outSpec = new DataTableSpec[] { outSpec };

		return m_outSpec;
	}

	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {

		final BufferedDataTable inTable = inData[0];

		BufferedDataContainer container = exec.createDataContainer(m_outSpec[0]);
		
		/* * * Create Context * * */
		ClassLoaderManager clManager = new ClassLoaderManager();
		
		
		final Context context = new Context(ScriptService.class, JavaService.class,
				KnimeInputDataTableService.class,
				KnimeOutputDataTableService.class,
				ObjectService.class, 
				WidgetService.class,
				InputAdapterService.class, OutputAdapterService.class);

		/* add custom CommandRunner Plugin */
		context.getService(PluginService.class).addPlugin(
				new PluginInfo<>(BlockingCommandJavaRunner.class,
						JavaRunner.class));

		/* get required services */
		final ObjectService objectService = context
				.getService(ObjectService.class);
		final ScriptLanguage java = objectService.getObjects(
				JavaScriptLanguage.class).get(0);
		final JavaEngine javaEngine = (JavaEngine) java.getScriptEngine();
		final JavaService javaRunner = context.getService(JavaService.class);

		/* get Input and Ouput Services and set input and output */
		final KnimeInputDataTableService inService = context.getService(KnimeInputDataTableService.class);
		final KnimeOutputDataTableService outService = context.getService(KnimeOutputDataTableService.class);
		
		inService.setInputDataTable(inTable);
		outService.setOutputContainer(container);
		
		/* add libraries to class path */
		ArrayList<URL> urls = new ArrayList<URL>();
		
		for (String project : DEFAULT_DEPENDENCIES) {
			urls.addAll(getClasspathFor(project));
		}
		
		URL[] urlArray = urls.toArray(new URL[] {});
		clManager.setClassLoader(urlArray);
		
		/* compile an run script for all rows */
		try {
			/* 
			 * TODO: we thought about getting currently loaded libraries/classes
			 * from the bungle header. Might want to investigate further some
			 * time..
			 *
			 * BundleLoader loader = (BundleLoader) classLoader.getDelegate();
			 *
			 * AbstractBundle bundle = loader.getBundle();
			 * String[] classPath = bundle.getBundleData().getClassPath();
			 *
			 * Dictionary<String, String> headers =
			 * bundle.getBundle().getHeaders(); 
			 */
			
			if (inService.hasNext()) {
				inService.next();
				
				javaEngine.eval(m_codeModel.getStringValue());
				Class<?> o = Thread.currentThread().getContextClassLoader().loadClass("script.MyClass");
				
				outService.appendRow();
				
				while (inService.hasNext()) {
					inService.next();
					javaRunner.run(o);
					outService.appendRow();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		} finally {
			clManager.resetClassLoader();
		}

		container.close();

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
		m_codeModel.validateSettings(settings);
	}
	
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
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
