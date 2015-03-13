package org.knime.knip.scripting.matching;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.knip.scijava.commands.KnimePreprocessor;
import org.knime.knip.scijava.commands.adapter.InputAdapter;
import org.knime.knip.scijava.commands.adapter.InputAdapterService;
import org.knime.knip.scijava.commands.impl.KnimeInputDataTableService;
import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.InitPreprocessor;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * 
 * @author Jonathan Hale (University of Konstanz)
 *
 */
@Plugin(type = PreprocessorPlugin.class, priority = Priority.NORMAL_PRIORITY)
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

				String name = m_cimService.getColumnNameFor(i.getName());
				
				if (name == null) {
					System.out.println("Warning: Couldn't find column input matching.");
					continue;
				}
//				if (!m.getActive()) {
//					// skip this one,  it wont be resolved by this Preprocessor
//					System.out.println("Warning: cim is not active.");
//					continue;
//				}
				
				int index = spec.findColumnIndex(name);
				DataCell c = r.getCell(index);
				
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
			} else {
				System.out.println("Warning: input already resolved!");
			}
		}
	}
}
