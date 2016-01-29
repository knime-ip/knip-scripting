package org.knime.knip.scripting.adapters;

import java.io.IOException;

import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.AbstractOutputAdapter;
import org.knime.scijava.commands.adapter.OutputAdapter;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;

/**
 * Adapter for Img to {@link ImgPlusValue}.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */

@SuppressWarnings("rawtypes")
@Plugin(type = OutputAdapter.class, priority = Priority.LOW_PRIORITY)
public class ImgOutputAdapter extends AbstractOutputAdapter<Img, ImgPlusValue> {

	@Parameter
	private KNIMEExecutionService execService;

	@SuppressWarnings({ "unchecked" })
	@Override
	protected ImgPlusValue createCell(final Img img) {
		try {
			return new ImgPlusCellFactory(execService.getExecutionContext())
					.createCell(ImgPlus.wrap(img));
		} catch (final IOException e) {
			throw new IllegalArgumentException(
					"Can't convert value: " + e.getMessage());
		}
	}

	@Override
	public Class<ImgPlusValue> getOutputType() {
		return ImgPlusValue.class;
	}

	@Override
	public Class<Img> getInputType() {
		return Img.class;
	}

}
