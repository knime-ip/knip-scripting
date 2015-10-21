package org.knime.knip.scripting.base;

import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.script.ScriptLanguage;

public class CommandCompileProductHelper implements CompileProductHelper {

	private final CommandInfo m_info;
	private final Context m_context;

	public CommandCompileProductHelper(Context context, CommandInfo info) {
		m_info = info;
		m_context = context;
	}

	@Override
	public Iterable<ModuleItem<?>> inputs() {
		return m_info.inputs();
	}

	@Override
	public ModuleInfo getModuleInfo() {
		return m_info;
	}

	@Override
	public Module createModule(final ScriptLanguage lang)
			throws ModuleException {
		final Module module = m_info.createModule();
		m_context.inject(module);

		return module;
	}

	@Override
	public void resetModule(Module m) {
		for (final String input : m.getInputs().keySet()) {
			m.setResolved(input, false);
		}
		for (final String output : m.getOutputs().keySet()) {
			m.setResolved(output, false);
		}
	}

}
