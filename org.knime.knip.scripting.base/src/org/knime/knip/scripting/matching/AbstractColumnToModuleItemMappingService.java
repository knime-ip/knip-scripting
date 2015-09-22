package org.knime.knip.scripting.matching;

import java.util.ArrayList;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.knip.scripting.matching.ColumnToModuleItemMappingService.ColumnToModuleItemMappingChangeListener;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

/**
 * Abstract Service implementing some simple methods via other methods and the
 * {@link ColumnToModuleItemMapping}
 * interface as {@link DefaultColumnToModuleItemMapping}.
 *
 * @author Jonathan Hale (University of Konstanz)
 */
public abstract class AbstractColumnToModuleItemMappingService extends
		AbstractService implements ColumnToModuleItemMappingService,
		ColumnToModuleItemMappingChangeListener {

	public class DefaultColumnToModuleItemMapping implements
			ColumnToModuleItemMapping {

		protected String m_columnName;
		protected String m_itemName;
		protected boolean m_active;
		protected ArrayList<ColumnToModuleItemMappingChangeListener> m_listeners;

		public DefaultColumnToModuleItemMapping(final String columnName,
				final String itemName) {
			m_columnName = columnName;
			m_itemName = itemName;
			m_active = true;
			m_listeners = new ArrayList<ColumnToModuleItemMappingChangeListener>();
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
		public DataColumnSpec getColumnSpec(final DataTableSpec spec) {
			return spec.getColumnSpec(m_columnName);
		}

		@Override
		public Integer getColumnIndex(final DataTableSpec spec) {
			return spec.findColumnIndex(m_columnName);
		}

		@Override
		public ModuleItem<?> getModuleItem(final Module module) {
			return module.getInfo().getInput(m_itemName);
		}

		@Override
		public boolean isActive() {
			return m_active;
		}

		@Override
		public void setActive(final boolean flag) {
			m_active = flag;
		}

		@Override
		public void addMappingChangeListener(
				final ColumnToModuleItemMappingChangeListener listener) {
			m_listeners.add(listener);
		}

		@Override
		public void removeMappingChangeListener(
				final ColumnToModuleItemMappingChangeListener listener) {
			m_listeners.remove(listener);
		}

		@Override
		public void fireMappingColumnChanged(final String oldValue) {
			final ColumnToModuleItemMappingChangeEvent e = new ColumnToModuleItemMappingChangeEvent(
					this, oldValue);
			for (final ColumnToModuleItemMappingChangeListener l : m_listeners) {
				l.onMappingColumnChanged(e);
			}
		}

		@Override
		public void fireMappingItemChanged(final String oldValue) {
			final ColumnToModuleItemMappingChangeEvent e = new ColumnToModuleItemMappingChangeEvent(
					this, oldValue);
			for (final ColumnToModuleItemMappingChangeListener l : m_listeners) {
				l.onMappingItemChanged(e);
			}
		}

		@Override
		public void setColumnName(final String columnName) {
			if (columnName == null) {
				return;
			}
			if (!columnName.equals(m_columnName)) {
				final String oldName = m_columnName;
				m_columnName = columnName;
				fireMappingColumnChanged(oldName);
			}
		}

		@Override
		public void setItemName(final String itemName) {
			if (itemName == null) {
				return;
			}
			if (!itemName.equals(m_itemName)) {
				final String oldName = m_itemName;
				m_itemName = itemName;
				fireMappingItemChanged(oldName);
			}
		}

	}

	@Override
	public ColumnToModuleItemMapping getMappingForColumn(
			final DataColumnSpec column) {
		return getMappingForColumnName(column.getName());
	}

	@Override
	public ColumnToModuleItemMapping getMappingForModuleItem(
			final ModuleItem<?> item) {
		return getMappingForModuleItemName(item.getName());
	}

	@Override
	public ColumnToModuleItemMapping addMapping(final String columnName,
			final String itemName) {
		final ColumnToModuleItemMapping m = new DefaultColumnToModuleItemMapping(
				columnName, itemName);
		m.addMappingChangeListener(this);
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
