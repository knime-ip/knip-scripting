package org.knime.scijava.scripting.base;

import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.script.ScriptLanguage;

/**
 * Interface for classes which wrap a compilation product. This may be a
 * CommandInfo (for Java scripts) or a ScriptInfo (basically everything else.
 *
 * @author Jonathan Hale
 */
public interface CompileProductHelper {

	public Iterable<ModuleItem<?>> inputs();

	public ModuleInfo getModuleInfo();

	public Module createModule(ScriptLanguage language) throws ModuleException;

	public void resetModule(Module m);

}
