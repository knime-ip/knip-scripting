package org.knime.knip.scripting.adapters;

import org.knime.core.data.BooleanValue;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.scijava.commands.adapter.AbstractInputAdapter;
import org.knime.scijava.commands.adapter.InputAdapter;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import net.imglib2.RandomAccessibleInterval;

/**
 * Adapter for {@link BooleanValue} to Boolean.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */
@Plugin(type = InputAdapter.class, priority = Priority.LOW_PRIORITY - 1)
public class RAIInputAdapter
		extends AbstractInputAdapter<ImgPlusValue, RandomAccessibleInterval> {

	@Override
	protected RandomAccessibleInterval getValue(final ImgPlusValue value) {
		return value.getImgPlus();
	}

	@Override
	public Class<ImgPlusValue> getInputType() {
		return ImgPlusValue.class;
	}

	@Override
	public Class<RandomAccessibleInterval> getOutputType() {
		return RandomAccessibleInterval.class;
	}
}
