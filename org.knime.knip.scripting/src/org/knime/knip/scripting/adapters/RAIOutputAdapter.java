package org.knime.knip.scripting.adapters;

import java.io.IOException;

import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.AbstractOutputAdapter;
import org.knime.scijava.commands.adapter.OutputAdapter;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;

/**
 * Adapter for Img to {@link ImgPlusCell}.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */

@SuppressWarnings("rawtypes")
@Plugin(type = OutputAdapter.class, priority = Priority.LOW_PRIORITY - 1)
public class RAIOutputAdapter extends AbstractOutputAdapter<Img, ImgPlusCell> {

	@Parameter
	KNIMEExecutionService execService;

	@SuppressWarnings({ "unchecked" })
	@Override
	protected ImgPlusCell createCell(Img img) {
		try {
			return new ImgPlusCellFactory(execService.getExecutionContext())
					.createCell(ImgPlus.wrap(img));
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Can't convert value: " + e.getMessage());
		}
	}

	@Override
	public Class<ImgPlusCell> getOutputType() {
		return ImgPlusCell.class;
	}

	@Override
	public Class<Img> getInputType() {
		return Img.class;
	}

}
