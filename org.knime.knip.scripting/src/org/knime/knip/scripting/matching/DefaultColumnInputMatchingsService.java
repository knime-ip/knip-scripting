package org.knime.knip.scripting.matching;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * TODO
 * @author Jonathan Hale (University of Konstanz)
 *
 */
@Plugin(type = ColumnInputMatchingsService.class)
public class DefaultColumnInputMatchingsService extends AbstractService implements ColumnInputMatchingsService {
	
	private Map<String, ColumnInputMatching> m_ciMatchings = new HashMap<String, ColumnInputMatching>();


	public void setColumnInputMatchings(Collection<ColumnInputMatching> matchings) {
		for (ColumnInputMatching m : matchings) {
			m_ciMatchings.put(m.getInput().getName(), m);
		}
	}
	
	public Collection<ColumnInputMatching> getColumnInputMatchings() {
		return m_ciMatchings.values();
	}

	@Override
	public ColumnInputMatching getColumnInputMatching(String inputName) {
		return m_ciMatchings.get(inputName);
	}
	
}
