package org.knime.knip.scripting.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.Pair;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * Service which handles {@link ColumnToModuleInputMapping}s. They are used by
 * {@link ColumnInputMappingKnimePreprocessor} to determine which data table
 * column should fill the value of a module input.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
@Plugin(type = ColumnToModuleItemMappingService.class)
public class DefaultColumnToModuleItemMappingService extends AbstractColumnToModuleItemMappingService {

	ArrayList<ColumnToModuleItemMapping> m_mappings = new ArrayList<ColumnToModuleItemMapping>();
	WeakHashMap<String, ColumnToModuleItemMapping> m_mappingsByColumn = new WeakHashMap<String, ColumnToModuleItemMappingService.ColumnToModuleItemMapping>();
	WeakHashMap<String, ColumnToModuleItemMapping> m_mappingsByItem = new WeakHashMap<String, ColumnToModuleItemMappingService.ColumnToModuleItemMapping>();
	
	@Override
	public List<ColumnToModuleItemMapping> getMappingsList() {
		return m_mappings;
	}

	@Override
	public ColumnToModuleItemMapping getMappingForColumnName(String columnName) {
		return m_mappingsByColumn.get(columnName);
	}

	@Override
	public ColumnToModuleItemMapping getMappingForModuleItemName(
			String inputName) {
		return m_mappingsByColumn.get(inputName);
	}

	@Override
	protected void addMapping(ColumnToModuleItemMapping mapping) {
		m_mappings.add(mapping);
		m_mappingsByColumn.put(mapping.getColumnName(), mapping);
		m_mappingsByItem.put(mapping.getItemName(), mapping);
	}
	
	@Override
	public ColumnToModuleItemMapping removeMapping(
			ColumnToModuleItemMapping mapping) {
		
		if (m_mappings.remove(mapping)) {
			m_mappingsByColumn.remove(mapping.getColumnName());
			m_mappingsByItem.remove(mapping.getItemName());
			return mapping;
		}
		
		return null;
	}
}
