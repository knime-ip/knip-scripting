package org.knime.scijava.scripting.base;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.scijava.Context;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;

public class ScriptCompileProductHelper implements CompileProductHelper {

	private final ScriptInfo m_info;
	private final Context m_context;
	private Iterable<ModuleItem<?>> m_inputs = null;

	public ScriptCompileProductHelper(Context context, ScriptInfo info) {
		m_info = info;
		m_context = context;
	}

	@Override
	public Iterable<ModuleItem<?>> inputs() {
		if (m_inputs == null) {
			// ScriptInfo inputs are reparsed every time. Cache for more
			// efficiency.
			m_inputs = m_info.inputs();
		}

		return m_inputs;
	}

	@Override
	public ModuleInfo getModuleInfo() {
		return m_info;
	}

	@Override
	public Module createModule(final ScriptLanguage language) throws ModuleException {
		final ScriptModule module = m_info.createModule();

		// use the currently selected language to execute the script
		module.setLanguage(language);
		m_context.inject(module);

		return module;
	}

	@Override
	public void resetModule(Module m) {
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
