/*
 * #%L
 * JSR-223-compliant Java scripting language plugin.
 * %%
 * Copyright (C) 2008 - 2014 Board of Regents of the University of
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

package org.knime.knip.scripting.java;

import java.util.concurrent.ExecutionException;

import org.scijava.Priority;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.plugins.scripting.java.AbstractJavaRunner;
import org.scijava.plugins.scripting.java.JavaRunner;

/**
 * Runs the given {@link Command} class.
 * 
 * @author Curtis Rueden
 * @author Jonathan Hale
 */
@Plugin(type = JavaRunner.class, priority = Priority.FIRST_PRIORITY)
public class BlockingCommandJavaRunner extends AbstractJavaRunner {

	@Parameter
	private PluginService pluginService;

	@Parameter
	private CommandService commandService;

	// -- JavaRunner methods --

	@Override
	public void run(final Class<?> c) {
		@SuppressWarnings("unchecked")
		final Class<? extends Command> commandClass = (Class<? extends Command>) c;
		final Plugin annotation = c.getAnnotation(Plugin.class);
		final CommandInfo info = new CommandInfo(commandClass, annotation);
		pluginService.addPlugin(info);

		try {
			commandService.run(info, true).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	// -- Typed methods --

	@Override
	public boolean supports(final Class<?> c) {
		return Command.class.isAssignableFrom(c);
	}

}
