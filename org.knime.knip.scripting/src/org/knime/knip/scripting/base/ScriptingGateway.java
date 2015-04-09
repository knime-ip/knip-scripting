package org.knime.knip.scripting.base;

import java.util.Arrays;
import java.util.List;

import org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader;
import org.knime.knip.scijava.commands.KnimeExecutionService;
import org.knime.knip.scijava.commands.adapter.InputAdapterService;
import org.knime.knip.scijava.commands.adapter.OutputAdapterService;
import org.knime.knip.scijava.commands.impl.KnimeInputDataTableService;
import org.knime.knip.scijava.commands.impl.KnimeOutputDataTableService;
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scijava.commands.widget.DialogWidgetService;
import org.knime.knip.scijava.core.ResourceAwareClassLoader;
import org.knime.knip.scripting.matching.ColumnInputMappingKnimePreprocessor;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService;
import org.knime.knip.scripting.matching.DefaultColumnToModuleItemMappingService;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayPostprocessor;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugins.scripting.java.DefaultJavaService;
import org.scijava.plugins.scripting.java.JavaRunner;
import org.scijava.script.ScriptService;
import org.scijava.service.Service;
import org.scijava.widget.DefaultWidgetService;

/**
 * ScriptingGateway creates the scijava context for the Scripting nodes for
 * KNIME Image Processing.
 * 
 * @author Jonathan Hale (University of Konstanz)
 *
 */
public class ScriptingGateway {

	protected static ScriptingGateway m_instance = null;

	protected ResourceAwareClassLoader m_classLoader = null;

	protected Context m_context = null;

	/**
	 * Constructor, creates Scijava Context
	 */
	protected ScriptingGateway() {
		List<Class<? extends Service>> requiredServices = Arrays
				.<Class<? extends Service>> asList(ScriptService.class,
						DefaultJavaService.class,
						KnimeInputDataTableService.class,
						KnimeOutputDataTableService.class,
						KnimeExecutionService.class, NodeSettingsService.class,
						ObjectService.class, DefaultWidgetService.class,
						DialogWidgetService.class, InputAdapterService.class,
						OutputAdapterService.class, CommandService.class);

		m_classLoader = new ResourceAwareClassLoader(
				(DefaultClassLoader) getClass().getClassLoader());

		m_context = new Context(requiredServices, new PluginIndex(
				new DefaultPluginFinder(m_classLoader)));
		
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
	}

	/**
	 * Get the Gateway instance.
	 * 
	 * @return
	 */
	public static ScriptingGateway get() {
		if (m_instance == null) {
			m_instance = new ScriptingGateway();
		}

		return m_instance;
	}

	/**
	 * Get context of this Gateway.
	 * 
	 * @return
	 */
	public Context getContext() {
		return m_context;
	}

	/**
	 * Get class loader used by this Gateways context.
	 * 
	 * @return
	 */
	public ResourceAwareClassLoader getClassLoader() {
		return m_classLoader;
	}

}
