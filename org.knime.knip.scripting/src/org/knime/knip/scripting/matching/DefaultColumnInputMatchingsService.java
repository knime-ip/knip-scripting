package org.knime.knip.scripting.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.util.Pair;
import org.scijava.module.ModuleInfo;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * Service which organizes ColumnInputMatchings. TODO: javadoc
 * 
 * @author Jonathan Hale (University of Konstanz)
 */
@Plugin(type = ColumnInputMatchingsService.class)
public class DefaultColumnInputMatchingsService extends AbstractService
		implements ColumnInputMatchingsService {

	/* map storing ColumnInputMatchings by name for fast finding by input name */
	private Map<String, String> m_columnByInput = new HashMap<String, String>();
	
	private boolean m_outOfDate;
	private ArrayList<Pair<String, String>> m_matchings = new ArrayList<Pair<String, String>>();

	public List<Pair<String, String>> getColumnInputMatchings() {
		if (m_outOfDate) {
			/* rebuild matchings list */
			m_matchings.clear();
			
			for(Entry<String, String> e : m_columnByInput.entrySet()) {
				m_matchings.add(new Pair<>(e.getKey(), e.getValue()));
			}
			
			m_outOfDate = false;
		}
		
		return m_matchings;
	}

	@Override
	public int getMatchingsCount() {
		return m_columnByInput.size();
	}

	@Override
	public void addColumnInputMatching(String input, String col) {
		if (input == null) {
			throw new NullPointerException(
					"input cannot be null.");
		}
		if (col == null) {
			throw new NullPointerException(
					"col cannot be null.");
		}

		m_matchings.add(new Pair<>(input, col));
		m_columnByInput.put(input, col);
	}

	@Override
	public String getColumnNameFor(String inputName) {
		return m_columnByInput.get(inputName);
	}

	@Override
	public void removeColumnInputMatchingFor(String input) {
		m_columnByInput.remove(input);
		m_outOfDate = true;
	}

	public static final String CFG_NUM_ENTRIES = "NumEntries";
	public static final String CFG_ENTRY = "Entry";
	public static final String CFG_INPUT_NAME = "InputName";
	public static final String CFG_COLUMN_NAME = "ColumnName";
	
	@Override
	public void saveSettingsTo(Config config) {
		config.addInt(CFG_NUM_ENTRIES, getMatchingsCount());

		int i = 0;
		for (Pair<String, String> cim : getColumnInputMatchings()) {
			Config conf = config.addConfig(CFG_ENTRY + i++);
			conf.addString(CFG_INPUT_NAME + i, cim.getFirst());
			conf.addString(CFG_COLUMN_NAME + i, cim.getSecond());
		}
	}

	@Override
	public void loadSettingsFrom(Config config, DataTableSpec spec,
			ModuleInfo info) throws InvalidSettingsException {
		int numEntries = config.getInt(CFG_NUM_ENTRIES);

		for (int i = 0; i < numEntries; ++i) {
			Config conf = config.getConfig(CFG_ENTRY + i);
			addColumnInputMatching(conf.getString(CFG_INPUT_NAME), conf.getString(CFG_INPUT_NAME));
		}
	}

	@Override
	public void removeColumnInputMatching(int row) {
		Pair<String, String> o = m_matchings.remove(row);
		m_columnByInput.remove(o.getFirst());
	}

}
