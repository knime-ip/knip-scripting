package org.knime.scijava.scripting.util;

import java.io.IOException;
import java.io.Writer;

public class LineWriter extends Writer {

	private StringBuilder builder;

	public LineWriter() {
		builder = new StringBuilder();
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		builder.append(cbuf);
		builder.append("\n");
	}

	@Override
	public void flush() throws IOException {
		builder.setLength(0); // deletes content from buffer.
	}

	public void clear() {
		builder.setLength(0);
	}

	@Override
	public void close() throws IOException {
		// not needed
	}

	@Override
	public String toString() {
		return builder.toString();
	}
}
