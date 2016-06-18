package org.knime.scijava.scripting.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.core.runtime.FileLocator;

public class ScriptUtils {

    private ScriptUtils() {
        // NB Utility class
    }

    /**
     * Get the entire contents of an URL as String.
     *
     * @param path
     *            url to the file to get the contents of
     * @return contents of path as url
     */
    public static final String fileAsString(final String path) {
        try {
            final URL resolvedUrl = FileLocator.resolve(new URL(path));
            final byte[] bytes = Files.readAllBytes(Paths
                    .get(new URI(resolvedUrl.toString().replace(" ", "%20"))));
            return new String(bytes, Charset.defaultCharset());
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}
