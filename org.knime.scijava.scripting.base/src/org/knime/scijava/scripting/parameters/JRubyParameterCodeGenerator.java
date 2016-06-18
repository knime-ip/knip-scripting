package org.knime.scijava.scripting.parameters;

import org.scijava.plugin.Plugin;

/**
 * Code snippet generator for input parameters in Jython.
 * 
 * @author Gabriel Einsdorf (University of Konstanz)
 */
@Plugin(type = ParameterCodeGenerator.class, name = "ruby")
public class JRubyParameterCodeGenerator
        extends CommentBasedParameterCodeGenerator {

    @Override
    protected String getCommentString() {
        return "#";
    }

}
