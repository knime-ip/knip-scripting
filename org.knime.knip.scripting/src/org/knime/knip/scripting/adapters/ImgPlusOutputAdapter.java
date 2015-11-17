package org.knime.knip.scripting.adapters;

import java.io.IOException;

import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.AbstractOutputAdapter;
import org.knime.scijava.commands.adapter.OutputAdapter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;

/**
 * Adapter for ImgPlus to {@link ImgPlusCell}.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */
@SuppressWarnings("rawtypes")
@Plugin(type = OutputAdapter.class)
public class ImgPlusOutputAdapter
		extends AbstractOutputAdapter<ImgPlus, ImgPlusCell> {

	@Parameter
	private KNIMEExecutionService execService;

	@SuppressWarnings({ "unchecked" })
	@Override
	protected ImgPlusCell createCell(ImgPlus imp) {
		try {
			return new ImgPlusCellFactory(execService.getExecutionContext())
					.createCell(imp);
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
	public Class<ImgPlus> getInputType() {
		return ImgPlus.class;
	}

}
