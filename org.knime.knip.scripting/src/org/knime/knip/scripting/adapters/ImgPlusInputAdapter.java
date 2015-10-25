package org.knime.knip.scripting.adapters;

import org.knime.core.data.BooleanValue;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.scijava.commands.AbstractInputAdapter;
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.InputAdapter;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;

/**
 * Adapter for {@link BooleanValue} to Boolean.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */
@Plugin(type = InputAdapter.class, priority = Priority.HIGH_PRIORITY)
public class ImgPlusInputAdapter
		extends AbstractInputAdapter<ImgPlusValue, ImgPlus> {

	@Parameter
	KNIMEExecutionService execService;

	@Override
	protected ImgPlus getValue(ImgPlusValue value) {
		return value.getImgPlus();
	}

	@Override
	public Class<ImgPlusValue> getInputType() {
		return ImgPlusValue.class;
	}

	@Override
	public Class<ImgPlus> getOutputType() {
		return ImgPlus.class;
	}
}