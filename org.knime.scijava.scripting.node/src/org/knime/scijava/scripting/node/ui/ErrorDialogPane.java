package org.knime.scijava.scripting.node.ui;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;

public class ErrorDialogPane extends DefaultNodeSettingsPane {
    public ErrorDialogPane(final Exception e) {
        addDialogComponent(new DialogComponentLabel(
                "Could not create dialog component: " + e));

    }

    public ErrorDialogPane(final String s) {
        addDialogComponent(new DialogComponentLabel(s));
    }
}
