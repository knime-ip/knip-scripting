package org.knime.scijava.scripting.base;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.OutputAdapterService;
import org.knime.scijava.commands.io.InputDataRowService;
import org.knime.scijava.commands.io.OutputDataRowService;
import org.knime.scijava.commands.settings.NodeDialogSettingsService;
import org.knime.scijava.commands.settings.NodeModelSettingsService;
import org.knime.scijava.commands.simplemapping.SimpleColumnMappingService;
import org.knime.scijava.commands.widget.KNIMEWidgetService;
import org.knime.scijava.core.ResourceAwareClassLoader;
import org.knime.scijava.core.SubContext;
import org.knime.scijava.core.pluginindex.ReusablePluginIndex;
import org.knime.scijava.scripting.parameters.ParameterCodeGeneratorService;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayPostprocessor;
import org.scijava.object.ObjectService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.plugin.PluginService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;

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

    /**
     * The global context for all KNIP-2.0 Nodes.
     */
    private Context m_globalContext;

    /** the services which must be local to the node. */
    protected static List<Class<? extends Service>> localServices = Arrays
            .asList(InputDataRowService.class, OutputDataRowService.class,
                    KNIMEExecutionService.class,
                    NodeDialogSettingsService.class,
                    NodeModelSettingsService.class, ObjectService.class,
                    KNIMEWidgetService.class, UIService.class,
                    OutputAdapterService.class, CommandService.class,
                    ParameterCodeGeneratorService.class,
                    SimpleColumnMappingService.class);
    // former services (for reference TODO: remove))
    // ScriptService.class,
    // PrefService.class,
    // SettingsModelTypeService.class,
    // LanguageSupportService.class,
    // ScriptHeaderService.class,
    // InputAdapterService.class,
    // WidgetService.class,

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
    public Context createSubContext() {
        final Context context = new SubContext(getGlobalContext(),
                localServices, m_pluginIndex);

        // cleanup unwanted services
        final PluginService plugins = context.getService(PluginService.class);
        plugins.removePlugin(plugins.getPlugin(DisplayPostprocessor.class));

        return context;
    }

    private Context getGlobalContext() {
        if (m_globalContext == null) {
            m_globalContext = new Context(m_pluginIndex);

            // NB: required services are local for the subcontext.
            // FIXME: make the required services list as small as possible, then
            // remove.
            // m_globalContext.getServiceIndex().removeAll(requiredServices);
            // m_globalContext.getPluginIndex().removeAll(requiredServices);
        }
        return m_globalContext;
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
