package org.knime.knip.scripting.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for working with {@link Class}.
 * 
 * @author Jonathan Hale
 */
public class ClassUtils {

	private static final Set<Class<?>> primitives = new HashSet<Class<?>>();
	private static final Set<Class<?>> wrappers = new HashSet<Class<?>>();

	static {
		primitives.add(boolean.class);
		primitives.add(byte.class);
		primitives.add(char.class);
		primitives.add(short.class);
		primitives.add(int.class);
		primitives.add(long.class);
		primitives.add(float.class);
		primitives.add(double.class);
		primitives.add(void.class);

		wrappers.add(Boolean.class);
		wrappers.add(Byte.class);
		wrappers.add(Character.class);
		wrappers.add(Short.class);
		wrappers.add(Integer.class);
		wrappers.add(Long.class);
		wrappers.add(Float.class);
		wrappers.add(Double.class);
		wrappers.add(Void.class);
	}

	/**
	 * @param c
	 *            The class to check
	 * @return <code>true</code> if the given class is either a primitve class
	 *         or a primitive wrapper class.
	 */
	public static boolean isPrimitiveOrWrapper(final Class<?> c) {
		return primitives.contains(c) || wrappers.contains(c);
	}
}
