package org.knime.scijava.scripting.parameters;

import org.scijava.plugin.Plugin;

/**
 * Code snippet generator for input parameters in Javascript.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
@Plugin(type = ParameterCodeGenerator.class, name = "javascript")
public class JavascriptParameterCodeGenerator
        extends CommentBasedParameterCodeGenerator {

    @Override
    protected String getCommentString() {
        return "//";
    }

}
