package org.knime.knip.scripting.adapters;

import org.knime.core.data.BooleanValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.scijava.commands.adapter.AbstractInputAdapter;
import org.knime.scijava.commands.adapter.InputAdapter;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

/**
 * Adapter for {@link BooleanValue} to Boolean.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */
@Plugin(type = InputAdapter.class, priority = Priority.HIGH_PRIORITY)
public class LabelingInputAdapter
		extends AbstractInputAdapter<LabelingValue, LabelingValue> {

	@Override
	protected LabelingValue getValue(final LabelingValue value) {
		return value;
	}

	@Override
	public Class<LabelingValue> getInputType() {
		return LabelingValue.class;
	}

	@Override
	public Class<LabelingValue> getOutputType() {
		return LabelingValue.class;
	}
}
