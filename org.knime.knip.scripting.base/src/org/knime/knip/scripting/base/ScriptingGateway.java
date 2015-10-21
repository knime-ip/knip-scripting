package org.knime.knip.scripting.base;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import org.knime.knip.scijava.commands.DefaultInputDataRowService;
import org.knime.knip.scijava.commands.DefaultOutputDataRowService;
import org.knime.knip.scijava.commands.KNIMEExecutionService;
import org.knime.knip.scijava.commands.adapter.InputAdapterService;
import org.knime.knip.scijava.commands.adapter.OutputAdapterService;
import org.knime.knip.scijava.commands.mapping.ColumnModuleItemMappingService;
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scijava.commands.widget.KNIMEWidgetService;
import org.knime.knip.scijava.core.ResourceAwareClassLoader;
import org.knime.knip.scijava.core.pluginindex.ReusablePluginIndex;
import org.knime.knip.scripting.parameters.ParameterCodeGeneratorService;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayPostprocessor;
import org.scijava.object.ObjectService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.plugin.PluginService;
import org.scijava.plugins.scripting.java.DefaultJavaService;
import org.scijava.prefs.PrefService;
import org.scijava.script.ScriptHeaderService;
import org.scijava.script.ScriptService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import org.scijava.widget.DefaultWidgetService;

import net.imagej.ui.swing.script.LanguageSupportService;

/**
 * ScriptingGateway is a singleton class which creates the scijava contexts for
 * Scripting nodes of KNIME Image Processing. This Gateway internally uses
 * {@link WeakReference} to determine if a context can be destroyed. Since node
 * model instances are created once and then only reclaimed by garbage collector
 * when workflow is closed or the node is destroyed, one reference will always
 * be held by the node model and the Context reference will stay valid. I the
 * created contexts are not stored safely, they will be recreated at every
 * {@link #getContext(int)} call.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public class ScriptingGateway {

	/** singleton instance */
	protected static ScriptingGateway m_instance = null;

	/** the gateways class loader */
	protected ResourceAwareClassLoader m_classLoader = null;

	/**
	 * the cached plugin index. Building the plugin index only needs to be done
	 * once.
	 */
	protected PluginIndex m_pluginIndex = null;

	/** a list of services which need to be present in newly created contexts */
	protected static List<Class<? extends Service>> requiredServices = Arrays
			.<Class<? extends Service>> asList(ScriptService.class,
					DefaultJavaService.class, DefaultInputDataRowService.class,
					DefaultOutputDataRowService.class, PrefService.class,
					KNIMEExecutionService.class, NodeSettingsService.class,
					ObjectService.class, DefaultWidgetService.class,
					KNIMEWidgetService.class, InputAdapterService.class,
					UIService.class, OutputAdapterService.class,
					CommandService.class, LanguageSupportService.class,
					ScriptHeaderService.class, ParameterCodeGeneratorService.class,
					ColumnModuleItemMappingService.class);

	/**
	 * Constructor. Only to be called from {@link #get()}.
	 */
	protected ScriptingGateway() {
		m_classLoader = new ResourceAwareClassLoader(
				getClass().getClassLoader(), getClass());

		m_pluginIndex = new ReusablePluginIndex(
				new DefaultPluginFinder(m_classLoader));
	}

	/**
	 * Get the Gateway instance.
	 *
	 * @return the singletons instance
	 */
	public static synchronized ScriptingGateway get() {
		if (m_instance == null) {
			m_instance = new ScriptingGateway();
		}

		return m_instance;
	}

	/**
	 * Create a new Scijava {@link Context} with Services required for the
	 * ScriptingNode.
	 * 
	 * @return the created context
	 */
	public Context createContext() {
		final Context context = new Context(requiredServices, m_pluginIndex);

		/* Make sure custom plugins have been added */
		final PluginService plugins = context.getService(PluginService.class);
		plugins.removePlugin(plugins.getPlugin(DisplayPostprocessor.class));

		return context;
	}

	/**
	 * Get the {@link ResourceAwareClassLoader} used by this Gateways contexts.
	 *
	 * @return class loader for the contexts
	 */
	public ResourceAwareClassLoader getClassLoader() {
		return m_classLoader;
	}

	private ClassLoader m_urlClassLoader = null;

	/**
	 * Create a {@link URLClassLoader} which contains scijava plugins and
	 * services.
	 * 
	 * @return the class laoder
	 */
	public ClassLoader createUrlClassLoader() {

		if (m_urlClassLoader == null) {
			m_urlClassLoader = new URLClassLoader(
					m_classLoader.getBundleUrls().toArray(new URL[] {}),
					new JoinClassLoader(getClassLoader(),
							Thread.currentThread().getContextClassLoader()));
		}
		return m_urlClassLoader;
	}

}
