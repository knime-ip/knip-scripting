package org.knime.knip.scripting.adapters;

import org.knime.core.data.BooleanValue;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.scijava.commands.AbstractInputAdapter;
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.InputAdapter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imglib2.img.Img;

/**
 * Adapter for {@link BooleanValue} to Boolean.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */
@Plugin(type = InputAdapter.class)
public class ImgPlusInputAdapter
		extends AbstractInputAdapter<ImgPlusValue, Img> {

	@Parameter
	KNIMEExecutionService execService;

	@Override
	protected Img getValue(ImgPlusValue value) {
		return value.getImgPlus();
	}

	@Override
	public Class<ImgPlusValue> getInputType() {
		return ImgPlusValue.class;
	}

	@Override
	public Class<Img> getOutputType() {
		return Img.class;
	}
}
