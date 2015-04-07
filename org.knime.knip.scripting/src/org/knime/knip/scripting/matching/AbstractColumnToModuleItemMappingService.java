package org.knime.knip.scripting.matching;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * Abstract Service implementing some simple methods via other methods and the
 * {@link ColumnToModuleItemMapping} interface as
 * {@link DefaultColumnToModuleItemMapping}.
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
@Plugin(type = ColumnToModuleItemMappingService.class)
public abstract class AbstractColumnToModuleItemMappingService extends
		AbstractService implements ColumnToModuleItemMappingService {

	public class DefaultColumnToModuleItemMapping implements
			ColumnToModuleItemMapping {

		protected String m_columnName;
		protected String m_itemName;
		protected boolean m_active;

		public DefaultColumnToModuleItemMapping(String columnName,
				String itemName) {
			m_columnName = columnName;
			m_itemName = itemName;
		}

		@Override
		public String getColumnName() {
			return m_columnName;
		}

		@Override
		public String getItemName() {
			return m_itemName;
		}

		@Override
		public DataColumnSpec getColumnSpec(DataTableSpec spec) {
			return spec.getColumnSpec(m_columnName);
		}

		@Override
		public Integer getColumnIndex(DataTableSpec spec) {
			return spec.findColumnIndex(m_columnName);
		}

		@Override
		public ModuleItem<?> getModuleItem(Module module) {
			return module.getInfo().getInput(m_itemName);
		}

		@Override
		public boolean isActive() {
			return m_active;
		}

		@Override
		public void setActive(boolean flag) {
			m_active = flag;
		}

	}

	@Override
	public ColumnToModuleItemMapping getMappingForColumn(DataColumnSpec column) {
		return getMappingForColumnName(column.getName());
	}

	@Override
	public ColumnToModuleItemMapping getMappingForModuleItem(ModuleItem<?> item) {
		return getMappingForModuleItemName(item.getName());
	}

	@Override
	public ColumnToModuleItemMapping addMapping(String columnName,
			String itemName) {
		ColumnToModuleItemMapping m = new DefaultColumnToModuleItemMapping(
				columnName, itemName);

		addMapping(m);

		return m;
	}

	/**
	 * Add a pre created {@link ColumnToModuleItemMapping} to the Service. This
	 * method is called by {@link #addMapping(String, String)}.
	 * 
	 * @param mapping
	 *            {@link ColumnToModuleItemMapping} to add
	 */
	protected abstract void addMapping(ColumnToModuleItemMapping mapping);

}
