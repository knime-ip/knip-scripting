package org.knime.scijava.scripting.parameters;

import org.scijava.plugin.Plugin;

/**
 * Code snippet generator for input parameters in Groovy.
 * 
 * @author Gabriel Einsdorf (University of Konstanz)
 */
@Plugin(type = ParameterCodeGenerator.class, name = "groovy")
public class GroovyParameterCodeGenerator
		extends CommentBasedParameterCodeGenerator {

	@Override
	protected String getCommentString() {
		return "//";
	}

}
