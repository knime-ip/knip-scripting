package org.knime.scijava.scripting.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import javax.script.ScriptException;

import org.knime.core.util.FileUtil;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.plugins.scripting.java.JavaEngine;
import org.scijava.plugins.scripting.java.JavaScriptLanguage;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptLanguage;

/**
 *
 * @author Jonathan Hale
 */
public class CompileHelper {

    private File m_tempDir;
    private String m_script;

    private Context m_context;
    private Writer m_errorWriter;
    private Writer m_outputWriter;

    /**
     * Constructor
     *
     * @param context
     * @param outputWriter
     * @throws IOException
     */
    public CompileHelper(Context context, Writer errorWriter,
            Writer outputWriter) throws IOException {

        m_errorWriter = errorWriter;
        m_outputWriter = outputWriter;
        m_tempDir = FileUtil.createTempDir("ScriptingNode");

        m_context = context;
    }

    /*
     * Write script code from the current settings to a file in the temp
     * directory.
     */
    private void writeScriptToFile(final ScriptLanguage lang) {
        try (Writer w = new FileWriter(
                new File(m_tempDir, "script." + lang.getExtensions().get(0)))) {
            w.write(m_script);
            w.close();
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Compile the given script with the given language Note that compilation
     * may be done lazily.
     * 
     * @param script
     *            the script
     * @param language
     *            for compiling the script
     * @return The resulting scriptInfo.
     * @throws ScriptException
     */
    public CompileProductHelper compile(final String script,
            final ScriptLanguage language) throws ScriptException {
        setScript(script, language);

        if (language instanceof JavaScriptLanguage) {
            JavaEngine scriptEngine = (JavaEngine) language.getScriptEngine();
            scriptEngine.getContext().setErrorWriter(m_errorWriter);
            scriptEngine.getContext().setWriter(m_outputWriter);

            Class<? extends Command> commandClass = null;
            try {
                commandClass = (Class<? extends Command>) scriptEngine
                        .compile(new StringReader(m_script));
                // If the code does not follow basic java syntax (e.g. not
                // contained in a class, etc. the scriptEngine throws a
                // NullPointerException
            } catch (NullPointerException e) {
                throw new ScriptException("Code could not be parsed as Java,"
                        + " did you select the right language?");
            }
            if (commandClass == null) {
                throw new ScriptException("Could not initialize Command!");
            }
            return new CommandCompileProductHelper(
                    new CommandInfo(commandClass), m_context);
        }

        // create script module for execution
        final File scriptFile = new File(m_tempDir,
                "script." + language.getExtensions().get(0));

        final ScriptInfo info = new ScriptInfo(m_context,
                scriptFile.getAbsolutePath(), new StringReader(m_script));
        return new ScriptCompileProductHelper(info, m_context);
    }

    /**
     * Set the script to compile.
     *
     * @param script
     * @param language
     */
    protected void setScript(final String script,
            final ScriptLanguage language) {
        m_script = script;
        writeScriptToFile(language);
    }
}
