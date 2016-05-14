package org.knime.scijava.scripting.parameters;

import org.knime.scijava.commands.KNIMESciJavaConstants;

/**
 * Code snippet generator for input parameters using the notation using in
 * comments for many scripting languages.
 * 
 * @author Jonathan Hale (University of Konstanz)
 * @author Gabriel Einsdorf (University of Konstanz)
 */
public abstract class CommentBasedParameterCodeGenerator
		implements ParameterCodeGenerator {

	@Override
	public final String generateInputParameter(String code, String memberName,
			Class<?> type, String defaultColumn) {

		// Type conversion is done by the scijava framework
		final String typeName = type.getSimpleName();
		// final String typeName = (ClassUtils.isPrimitiveOrWrapper(type)
		// || type == String.class) ? type.getSimpleName()
		// : type.getName();

		// Create the comment string, e.g. : // @Double(columnSelect = "true")
		// number
		StringBuilder builder = new StringBuilder();
		builder.append(getCommentString());
		builder.append(" @");
		builder.append(typeName);
		builder.append("(");
		builder.append(KNIMESciJavaConstants.COLUMN_SELECT_KEY);
		builder.append("= \"true\", ");
		builder.append(KNIMESciJavaConstants.DEFAULT_COLUMN_KEY);
		builder.append("= \"");
		builder.append(defaultColumn);
		builder.append("\")");
		builder.append(" ");
		builder.append(memberName);
		builder.append("\n");

		return builder.toString();
	}

	@Override
	public final int getPosition(String code) {
		return Math.max(0, code.indexOf(getCommentString()));
	}

	/**
	 * @return String used to indicate a one-line comment. May be
	 *         <code>//</code> or <code>#</code> for example.
	 */
	protected abstract String getCommentString();

}
