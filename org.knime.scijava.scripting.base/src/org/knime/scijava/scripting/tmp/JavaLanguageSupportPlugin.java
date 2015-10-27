/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.knime.scijava.scripting.tmp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.fife.rsta.ac.java.JavaLanguageSupport;
import org.scijava.plugin.Plugin;

import net.imagej.ui.swing.script.LanguageSupportPlugin;
import net.imagej.ui.swing.script.LanguageSupportService;

/**
 * {@link LanguageSupportPlugin} for the java language.
 *
 * @author Jonathan Hale
 * @see JavaLanguageSupport
 * @see LanguageSupportService
 * @deprecated because we want to use the service of imagej-ui-swing as soon as
 *             dependency on 2.5.8 of fifesoft:RSTALanguageSupport in KNIME is
 *             available and this plugin has been reactived in imagej-ui-swing
 */
// This plugin is temporarily disabled pending a resolution for:
// https://github.com/bobbylight/RSTALanguageSupport/issues/26
@Deprecated
@Plugin(type = LanguageSupportPlugin.class)
public class JavaLanguageSupportPlugin extends JavaLanguageSupport
		implements LanguageSupportPlugin {

	public JavaLanguageSupportPlugin() throws IOException {
		super();
		getJarManager().addCurrentJreClassFileSource();

		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader instanceof URLClassLoader) {
			for (final URL url : ((URLClassLoader) classLoader).getURLs()) {
				if (url.getFile().endsWith(".jar")) {
					getJarManager().addClassFileSource(new File(url.getFile()));
				}
			}
		}
	}

	@Override
	public String getLanguageName() {
		return "java";
	}

}
