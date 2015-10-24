package org.knime.knip.scripting.adapters;

import org.knime.core.data.BooleanValue;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.scijava.commands.AbstractInputAdapter;
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.InputAdapter;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.ImgLabeling;

/**
 * Adapter for {@link BooleanValue} to Boolean.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */
@Plugin(type = InputAdapter.class, priority=Priority.HIGH_PRIORITY)
public class LabelingInputAdapter
		extends AbstractInputAdapter<LabelingValue, LabelingValue> {

	@Override
	protected LabelingValue getValue(LabelingValue value) {
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
