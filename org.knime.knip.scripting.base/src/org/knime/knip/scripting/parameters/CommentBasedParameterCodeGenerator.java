package org.knime.knip.scripting.parameters;

/**
 * Code snippet generator for input parameters using the notation using in
 * comments for Javascript or Python for example.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
public abstract class CommentBasedParameterCodeGenerator
		implements ParameterCodeGenerator {

	@Override
	public final String generateInputParameter(String code, String memberName,
			Class<?> type) {
		final String typeName = type.getName();

		return getCommentString() + " @INPUT " + typeName + " " + memberName
				+ "\n";
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
