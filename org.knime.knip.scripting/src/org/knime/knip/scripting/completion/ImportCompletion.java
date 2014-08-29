package org.knime.knip.scripting.completion;

import org.fife.ui.autocomplete.AbstractCompletion;
import org.fife.ui.autocomplete.CompletionProvider;

public class ImportCompletion extends AbstractCompletion {

	protected Package p;
	
	protected ImportCompletion(CompletionProvider provider, Package p) {
		super(provider);
	}

	@Override
	public String getReplacementText() {
		return p.toString();
	}

	@Override
	public String getSummary() {
		return "This is Summary?";
	}

}
