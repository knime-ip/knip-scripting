package org.knime.scijava.scripting.base;

import java.util.Arrays;
import java.util.List;

import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.InputAdapterService;
import org.knime.scijava.commands.adapter.OutputAdapterService;
import org.knime.scijava.commands.io.InputDataRowService;
import org.knime.scijava.commands.io.OutputDataRowService;
import org.knime.scijava.commands.settings.NodeModelSettingsService;
import org.knime.scijava.commands.settings.NodeSettingsService;
import org.knime.scijava.commands.settings.SettingsModelTypeService;
import org.knime.scijava.commands.widget.KNIMEWidgetService;
import org.knime.scijava.core.ResourceAwareClassLoader;
import org.knime.scijava.core.SubContext;
import org.knime.scijava.core.pluginindex.ReusablePluginIndex;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayPostprocessor;
import org.scijava.object.ObjectService;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.PluginIndex;
import org.scijava.plugin.PluginService;
import org.scijava.prefs.PrefService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import org.scijava.widget.WidgetService;

/**
 * Gateway to the SciJava world
 */
public class KNIMESciJavaGateway {

    /** singleton instance */
    protected static KNIMESciJavaGateway m_instance = null;

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

    /** a list of services which need to be present in newly created contexts */
    protected static List<Class<? extends Service>> requiredServices = Arrays
            .<Class<? extends Service>> asList(InputDataRowService.class,
                    OutputDataRowService.class, PrefService.class,
                    KNIMEExecutionService.class, NodeSettingsService.class,
                    ObjectService.class, WidgetService.class,
                    KNIMEWidgetService.class, InputAdapterService.class,
                    UIService.class, OutputAdapterService.class,
                    CommandService.class, NodeModelSettingsService.class,
                    InputAdapterService.class, SettingsModelTypeService.class);

    /**
     * Constructor. Only to be called from {@link #get()}.
     */
    protected KNIMESciJavaGateway() {
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
    public static synchronized KNIMESciJavaGateway get() {
        if (m_instance == null) {
            m_instance = new KNIMESciJavaGateway();
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
                requiredServices,
                new PluginIndex(new DefaultPluginFinder(m_classLoader)));

        // cleanup unwanted services
        final PluginService plugins = context.getService(PluginService.class);
        plugins.removePlugin(plugins.getPlugin(DisplayPostprocessor.class));

        return context;
    }

    private Context getGlobalContext() {
        if (m_globalContext == null) {
            m_globalContext = new Context(m_pluginIndex);

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

}
