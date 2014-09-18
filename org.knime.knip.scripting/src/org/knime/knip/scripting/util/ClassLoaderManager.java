package org.knime.knip.scripting.util;

import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderManager {
		ClassLoader m_classLoader;

		URLClassLoader m_urlClassLoader;

		public ClassLoaderManager(URL[] urls) {
			setClassLoader(urls);
		}

		public void setClassLoader(URL[] urls) {
			m_classLoader = Thread.currentThread().getContextClassLoader();
			m_urlClassLoader = new URLClassLoader(urls, getClass()
					.getClassLoader());

			setURLClassLoader();
		}

		public void setURLClassLoader() {
			Thread.currentThread().setContextClassLoader(m_urlClassLoader);
		}

		public void resetClassLoader() {
			Thread.currentThread().setContextClassLoader(m_classLoader);
		}
	}