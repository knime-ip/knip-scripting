package org.knime.knip.scripting.adapters;

import java.io.IOException;

import org.knime.core.data.def.BooleanCell;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.scijava.commands.AbstractOutputAdapter;
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.OutputAdapter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;

/**
 * Adapter for Boolean to {@link BooleanCell}.
 *
 * @author Jonathan Hale (University of Konstanz)
 *
 */
@SuppressWarnings("rawtypes")
@Plugin(type = OutputAdapter.class)
public class ImgPlusOutputAdapter
		extends AbstractOutputAdapter<Img, ImgPlusCell> {

	@Parameter
	KNIMEExecutionService execService;

	@SuppressWarnings({ "unchecked" })
	@Override
	protected ImgPlusCell createCell(Img imp) {
		try {
			return new ImgPlusCellFactory(execService.getExecutionContext())
					.createCell(ImgPlus.wrap(imp));
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Can't convert value: " + e.getMessage());
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<ImgPlusCell> getOutputType() {
		return ImgPlusCell.class;
	}

	@Override
	public Class<Img> getInputType() {
		return Img.class;
	}

}