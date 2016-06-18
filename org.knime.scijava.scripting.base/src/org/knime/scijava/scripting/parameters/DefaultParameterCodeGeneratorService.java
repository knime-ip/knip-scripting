package org.knime.scijava.scripting.parameters;

import java.util.HashMap;
import java.util.Map;

import org.scijava.InstantiableException;
import org.scijava.plugin.AbstractSingletonService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.service.Service;

/**
 * Default implementation of {@link ParameterCodeGeneratorService}.
 * 
 * @author Jonathan Hale
 */
@Plugin(type = Service.class)
public class DefaultParameterCodeGeneratorService
        extends AbstractSingletonService<ParameterCodeGenerator>
        implements ParameterCodeGeneratorService {

    private Map<String, ParameterCodeGenerator> m_generatorMap = null;

    @Override
    public Class<ParameterCodeGenerator> getPluginType() {
        return ParameterCodeGenerator.class;
    }

    @Override
    public ParameterCodeGenerator getGeneratorForLanguage(
            ScriptLanguage language) {
        if (m_generatorMap == null) {
            processInstances();
        }

        for (String name : language.getNames()) {
            ParameterCodeGenerator generator = m_generatorMap
                    .get(name.toLowerCase());
            if (generator != null) {
                return generator;
            }
        }

        return null;
    }

    /*
     * Sort the plugins into a hashmap of ScriptingLanguage name to plugin
     * instance.
     */
    private void processInstances() {
        try {
            m_generatorMap = new HashMap<>();
            for (PluginInfo<ParameterCodeGenerator> pluginInfo : getPlugins()) {
                m_generatorMap.put(pluginInfo.getName().toLowerCase(),
                        getInstance(pluginInfo.loadClass()));
            }
        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
    }

}
