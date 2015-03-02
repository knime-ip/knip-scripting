package org.knime.knip.scripting.matching;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.knip.scijava.bridge.KnimePreprocessor;
import org.knime.knip.scijava.bridge.adapter.InputAdapter;
import org.knime.knip.scijava.bridge.adapter.InputAdapterService;
import org.knime.knip.scijava.bridge.impl.KnimeInputDataTableService;
import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * 
 * @author Jonathan Hale (University of Konstanz)
 *
 */
@Plugin(type = PreprocessorPlugin.class, priority = Priority.FIRST_PRIORITY)
public class ColumnInputMatchingKnimePreprocessor extends AbstractPreprocessorPlugin implements KnimePreprocessor {

	@Parameter
	ColumnInputMatchingsService m_cimService;
	
	@Parameter
	KnimeInputDataTableService m_inputTable;
	
	@Parameter
	InputAdapterService m_inputAdapters;
	
	@Override
	public void process(Module module) {
		DataTableSpec spec = m_inputTable.getInputDataTableSpec();
		for (ModuleItem<?> i : module.getInfo().inputs()) {
			if (!module.isResolved(i.getName())) {
				DataRow r = m_inputTable.getInputDataRow();

				ColumnInputMatching m = m_cimService.getColumnInputMatching(i.getName());
				
				if (!m.getActive()) {
					// skip this one,  it wont be resolved by this Preprocessor
					continue;
				}
				
				DataCell c = m.getDataCell(r, spec);
				
				if (c == null) {
					cancel("Column Input Matching referred to non-existent cell.");
				}
				
				InputAdapter ia = m_inputAdapters.getMatchingInputAdapter(c.getClass(), i.getType());
				
				if (ia == null) {
					cancel("No InputAdapter for: " + c.getClass() + " > " + i.getType().getCanonicalName());
					return;
				}
				
				module.setInput(i.getName(), ia.getValue(c));
				module.setResolved(i.getName(), true);
			}
		}
	}
}
