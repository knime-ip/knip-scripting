package org.knime.scijava.scripting.parameters;

import org.scijava.plugin.SingletonPlugin;

/**
 * Plugin to extend the capabilities of the ScriptEditor to generate input
 * parameter code snippets.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public interface ParameterCodeGenerator extends SingletonPlugin {

	/**
	 * Generate an input parameter code snippet for the given <code>code</code> with
	 * type <code>type<code> and name it <code>var</code>.
	 * 
	 * @param code
	 *            Code to add the input parameter to
	 * @param var
	 *            Name for the input
	 * @param type
	 *            Type for the input
	 * @return Modified code
	 */
	public String generateInputParameter(String code, String var, Class<?> type);
	
	/**
	 * Find a position to insert a input parameter code snippet.
	 * @param code The code to search for a position to insert the code in
	 * @return the position at which to insert >= 0
	 */
	public int getPosition(String code);

}
