package org.knime.knip.scripting.parameters;

import org.scijava.plugin.Plugin;

/**
 * Code snippet generators for input parameters in Java.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
@Plugin(type = ParameterCodeGenerator.class, name = "java")
public class JavaParameterCodeGenerator implements ParameterCodeGenerator {

	@Override
	public String generateInputParameter(String code, String memberName, Class<?> type) {
		final String typeName = type.getName();
		
		return "\n\t@Parameter(type = ItemIO.INPUT)\n\tprivate "
				+ typeName + " " + memberName + ";\n";
	}
	
	@Override
	public int getPosition(String code) {
		return code.indexOf('{') + 1;
	}

}
