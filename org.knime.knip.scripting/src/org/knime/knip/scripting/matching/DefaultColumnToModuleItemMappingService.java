package org.knime.knip.scripting.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.scijava.plugin.Plugin;

/**
 * Service which handles {@link ColumnToModuleInputMapping}s. They are used by
 * {@link ColumnInputMappingKnimePreprocessor} to determine which data table
 * column should fill the value of a module input.
 * 
 * @author Jonathan Hale (University of Konstanz)
 * 
 * @see ColumnToModuleItemMapping
 * @see ColumnToModuleItemMappingService
 */
@Plugin(type = ColumnToModuleItemMappingService.class)
public class DefaultColumnToModuleItemMappingService extends
		AbstractColumnToModuleItemMappingService {

	/** list containing all mappings of this service */
	private ArrayList<ColumnToModuleItemMapping> m_mappings = new ArrayList<ColumnToModuleItemMapping>();
	
	/** mappings optimized for {@link #getMappingForColumnName(String)} */
	private WeakHashMap<String, ColumnToModuleItemMapping> m_mappingsByColumn = new WeakHashMap<String, ColumnToModuleItemMappingService.ColumnToModuleItemMapping>();
	
	/** mappings optimized for {@link #getMappingForModuleItemName(String)} */
	private WeakHashMap<String, ColumnToModuleItemMapping> m_mappingsByItem = new WeakHashMap<String, ColumnToModuleItemMappingService.ColumnToModuleItemMapping>();

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
		return m_mappingsByItem.get(inputName);
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
			// a mapping has been removed, we need to update the hash maps
			m_mappingsByColumn.remove(mapping.getColumnName());
			m_mappingsByItem.remove(mapping.getItemName());

			mapping.removeMappingChangeListener(this);

			return mapping;
		}

		// given mapping was not found
		return null;
	}

	@Override
	public void onMappingColumnChanged(ColumnToModuleItemMappingChangeEvent e) {
		// a column name has changed, we need to update the has maps to reflect
		// that change
		m_mappingsByColumn.remove(e.getPreviousValue());

		ColumnToModuleItemMapping mapping = e.getSourceMapping();
		m_mappingsByColumn.put(mapping.getColumnName(), mapping);
	}

	@Override
	public void onMappingItemChanged(ColumnToModuleItemMappingChangeEvent e) {
		// a module input name has changed, we need to update the has maps to reflect
		// that change
		m_mappingsByItem.remove(e.getPreviousValue());

		ColumnToModuleItemMapping mapping = e.getSourceMapping();
		m_mappingsByItem.put(mapping.getItemName(), mapping);
	}

	@Override
	public void clear() {
		// remove all mappings
		
		m_mappings.clear();
		m_mappingsByColumn.clear();
		m_mappingsByItem.clear();
	}
}
