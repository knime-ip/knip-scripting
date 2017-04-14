package org.knime.scijava.scripting.base;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.knime.scijava.commands.io.OutputShadowModule;
import org.scijava.Context;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;

public class ScriptCompileProductHelper implements CompileProductHelper {

    private final ScriptInfo m_info;
    private Context m_context;

    public ScriptCompileProductHelper(final ScriptInfo info, Context context) {
        m_info = info;
        m_context = context;
    }

    @Override
    public Module createModule(ScriptLanguage language) throws ModuleException {
        ScriptModule scriptModule = m_info.createModule();

        // use the currently selected language to execute the script
        scriptModule.setLanguage(language);
        m_context.inject(scriptModule);

        return new OutputShadowModule(scriptModule);
    }

    @Override
    public void resetModule(final Module m) {
        final ScriptEngine scriptEngine = ((ScriptModule) m).getEngine();

        for (final String input : m.getInputs().keySet()) {
            m.setResolved(input, false);
        }
        for (final String output : m.getOutputs().keySet()) {
            m.setResolved(output, false);
            scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).remove(output);
        }
    }

}
