package org.knime.scijava.scripting.base;

import org.knime.scijava.commands.io.OutputShadowModule;
import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.script.ScriptLanguage;

public class CommandCompileProductHelper implements CompileProductHelper {

    private final CommandInfo m_info;
    private final Context m_context;

    public CommandCompileProductHelper(CommandInfo info, Context context) {
        m_info = info;
        m_context = context;
    }

    @Override
    public Module createModule(ScriptLanguage language) throws ModuleException {
        final Module module = m_info.createModule();
        m_context.inject(module);

        return new OutputShadowModule(module);
    }

    @Override
    public void resetModule(final Module m) {
        for (final String input : m.getInputs().keySet()) {
            m.setResolved(input, false);
        }
        for (final String output : m.getOutputs().keySet()) {
            m.setResolved(output, false);
        }
    }

}
