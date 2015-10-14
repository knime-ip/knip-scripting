package org.knime.knip.scripting.parameters;

import org.scijava.plugin.Plugin;

/**
 * Code snippet generator for input parameters in Jython.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
@Plugin(type = ParameterCodeGenerator.class, name = "python")
public class JythonParameterCodeGenerator
		extends CommentBasedParameterCodeGenerator {

	@Override
	protected String getCommentString() {
		return "#";
	}

}
