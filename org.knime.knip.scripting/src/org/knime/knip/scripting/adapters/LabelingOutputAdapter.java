package org.knime.knip.scripting.adapters;

import java.io.IOException;

import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.scijava.commands.AbstractOutputAdapter;
import org.knime.scijava.commands.KNIMEExecutionService;
import org.knime.scijava.commands.adapter.OutputAdapter;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;

/**
 * Adapter for LabelingValue to {@link LabelingCell}.
 *
 * @author Gabriel Einsdorf (University of Konstanz)
 *
 */

@SuppressWarnings("rawtypes")
@Plugin(type = OutputAdapter.class, priority=Priority.HIGH_PRIORITY)
public class LabelingOutputAdapter
		extends AbstractOutputAdapter<LabelingValue, LabelingCell> {

	@Parameter
	KNIMEExecutionService execService;

	@SuppressWarnings({ "unchecked" })
	@Override
	protected LabelingCell createCell(LabelingValue imgL) {
				try {
			 return new LabelingCellFactory(execService.getExecutionContext())
					.createCell(imgL.getLabeling(), imgL.getLabelingMetadata());
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Can't convert value: " + e.getMessage());
		}
	}

	@Override
	public Class<LabelingCell> getOutputType() {
		return LabelingCell.class;
	}

	@Override
	public Class<LabelingValue> getInputType() {
		return LabelingValue.class;
	}

}