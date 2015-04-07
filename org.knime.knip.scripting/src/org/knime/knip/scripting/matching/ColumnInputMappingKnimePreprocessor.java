package org.knime.knip.scripting.matching;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.knip.scijava.commands.KnimePreprocessor;
import org.knime.knip.scijava.commands.adapter.InputAdapter;
import org.knime.knip.scijava.commands.adapter.InputAdapterService;
import org.knime.knip.scijava.commands.impl.KnimeInputDataTableService;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService.ColumnToModuleItemMapping;
import org.scijava.Priority;
import org.scijava.log.LogService;
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
@Plugin(type = PreprocessorPlugin.class, priority = Priority.NORMAL_PRIORITY)
public class ColumnInputMappingKnimePreprocessor extends
		AbstractPreprocessorPlugin implements KnimePreprocessor {

	@Parameter
	ColumnToModuleItemMappingService m_cimService;

	@Parameter
	KnimeInputDataTableService m_inputTable;

	@Parameter
	InputAdapterService m_inputAdapters;

	@Parameter
	LogService m_log;

	@Override
	public void process(Module module) {

		// get the DataTableSpec to later find column indices
		DataTableSpec spec = m_inputTable.getInputDataTableSpec();

		// some local variables set and used in the following loop
		ColumnToModuleItemMapping mapping = null;
		String inputName = "";

		// DataRow will remain the same while processing, this is a shortcut to
		// it.
		DataRow row = m_inputTable.getInputDataRow();

		// try to set module input values from the current DataRow
		for (ModuleItem<?> i : module.getInfo().inputs()) {
			inputName = i.getName();

			// the input may have already been filled by a previous
			// preprocessor.
			if (!module.isResolved(inputName)) {
				// get a column to input mapping
				mapping = m_cimService.getMappingForModuleItem(i);

				// there might be no mapping for this input
				if (mapping == null) {
					// skip this one, it wont be resolved by this Preprocessor
					m_log.warn("Couldn't find column input mapping for input \""
							+ inputName + "\".");
					continue;
				}
				
				// the mapping may be inactive in which case we wont use it
				if (!mapping.isActive()) {
					// skip this one, it wont be resolved by this Preprocessor
					m_log.warn("Mapping for input \"" + inputName
							+ "\" was found, but is not active.");
					continue;
				}

				// try to get the data cell matching the mapped column
				DataCell cell = null;
				try {
					cell = row.getCell(mapping.getColumnIndex(spec));
				} catch (IndexOutOfBoundsException e) {
					// getColumnIndex() might return -1 or a index greater the
					// column count
					m_log.warn("Couldn't find column \""
							+ mapping.getColumnName()
							+ "\" which is mapped to input " + inputName + ".");
					continue;
				}

				// find a input adapter which can convert the cells value to the
				// a type required by the input
				InputAdapter ia = m_inputAdapters.getMatchingInputAdapter(
						cell.getClass(), i.getType());

				if (ia == null) {
					cancel("No InputAdapter for: " + cell.getClass() + " > "
							+ i.getType().getCanonicalName());
					return;
				}

				// set the input and mark resolved
				module.setInput(inputName, ia.getValue(cell));
				module.setResolved(inputName, true);
			} else {
				// Often this is not intended, so we should inform about this problem
				m_log.warn("Input \"" + inputName + "\" was already resolved!");
			}
		}
	}
}
