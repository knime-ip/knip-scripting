package org.knime.knip.scripting.base;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.knip.scijava.commands.KnimeExecutionService;
import org.knime.knip.scijava.commands.KnimeInputDataTableService;
import org.knime.knip.scijava.commands.KnimeOutputDataTableService;
import org.knime.knip.scijava.commands.adapter.InputAdapterService;
import org.knime.knip.scijava.commands.adapter.OutputAdapterService;
import org.knime.knip.scijava.commands.mapping.ColumnToModuleItemMappingService;
import org.knime.knip.scijava.commands.settings.NodeSettingsService;
import org.knime.knip.scijava.commands.widget.KnimeWidgetService;
import org.knime.knip.scijava.core.ResourceAwareClassLoader;
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

	/** {@link Context} for id */
	protected ArrayList<WeakReference<Context>> m_contexts = new ArrayList<WeakReference<Context>>();

	/** a list of services which need to be present in newly created contexts */
	protected static List<Class<? extends Service>> requiredServices = Arrays
			.<Class<? extends Service>> asList(ScriptService.class,
					DefaultJavaService.class, KnimeInputDataTableService.class,
					KnimeOutputDataTableService.class, PrefService.class,
					KnimeExecutionService.class, NodeSettingsService.class,
					ObjectService.class, DefaultWidgetService.class,
					KnimeWidgetService.class, InputAdapterService.class,
					UIService.class, OutputAdapterService.class,
					CommandService.class, LanguageSupportService.class,
					ScriptHeaderService.class,
					ColumnToModuleItemMappingService.class);

	/**
	 * Constructor. Only to be called from {@link #get()}.
	 */
	protected ScriptingGateway() {

		m_classLoader = new ResourceAwareClassLoader(getClass()
				.getClassLoader(), getClass());

		m_pluginIndex = new PluginIndex(new DefaultPluginFinder(m_classLoader));
	}

	/**
	 * Return a new {@link Context} with the required Services and custom
	 * plugins.
	 *
	 * @return the created context
	 */
	protected Context createNewContext() {
		final Context context = new Context(requiredServices, m_pluginIndex);

		/* Make sure custom plugins have been added */
		final PluginService plugins = context.getService(PluginService.class);
		plugins.removePlugin(plugins.getPlugin(DisplayPostprocessor.class));

		return context;
	}

	/**
	 * Get the Gateway instance.
	 *
	 * @return the singletons instance
	 */
	public static ScriptingGateway get() {
		if (m_instance == null) {
			m_instance = new ScriptingGateway();
		}

		return m_instance;
	}

	/**
	 * Get a scijava {@link Context} for the given id. The
	 * {@link ScriptingGateway} does keep a strong reference to the Context, the
	 * returned reference needs to be referenced by the caller, otherwise a new
	 * Context will be created every call.
	 *
	 * @param id
	 *            ID of the context to get
	 * @return if id is valid (positive integer), a context will be returned
	 *         which may have been newly created, if none existed for id yet.
	 *         Otherwise returns null
	 */
	public Context getContext(final int id) {
		if (id < 0) {
			// invalid id
			return null;
		}

		Context c = null;

		// check if index is in bounds
		if (id < m_contexts.size()) {
			c = m_contexts.get(id).get();
		} else {
			// we will need to expand m_contexts size

			// expand capacity first for faster adding later
			m_contexts.ensureCapacity(id + 1);

			// expand size of m_contexts with null objects
			for (int i = (id - m_contexts.size() + 1); i > 0; --i) {
				m_contexts.add(null);
			}
		}

		if (c == null) {
			// context for this id does not exist yet. Create a new one:
			c = createNewContext();
			// and set the ids context
			m_contexts.set(id, new WeakReference<>(c));
		}

		return c;
	}

	/**
	 * Get the {@link ResourceAwareClassLoader} used by this Gateways contexts.
	 *
	 * @return class loader for the contexts
	 */
	public ResourceAwareClassLoader getClassLoader() {
		return m_classLoader;
	}

	/**
	 * Create a {@link URLClassLoader} which contains scijava plugins and
	 * services.
	 * 
	 * @return the class laoder
	 */
	public ClassLoader createUrlClassLoader() {
		return new URLClassLoader(m_classLoader.getBundleUrls().toArray(
				new URL[] {}), getClassLoader());
	}

}
