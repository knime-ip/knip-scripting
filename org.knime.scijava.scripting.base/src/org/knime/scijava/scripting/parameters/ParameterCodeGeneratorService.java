package org.knime.scijava.scripting.parameters;

import org.scijava.plugin.SingletonService;
import org.scijava.script.ScriptLanguage;

/**
 * Service which manages {@link ParameterCodeGenerator} plugins.
 * 
 * @author Jonathan Hale
 */
public interface ParameterCodeGeneratorService
		extends SingletonService<ParameterCodeGenerator> {

	/**
	 * Get a generator which generates input parameter code snippets for the
	 * given {@link ScriptLanguage}.
	 * 
	 * @param language
	 * @return the generator or <code>null</code> if no generator could be found
	 *         for the given language.
	 */
	ParameterCodeGenerator getGeneratorForLanguage(
			final ScriptLanguage language);

}
