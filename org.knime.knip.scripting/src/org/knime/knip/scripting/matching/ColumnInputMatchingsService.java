package org.knime.knip.scripting.matching;

import java.util.Collection;

import org.scijava.service.Service;

/**
 * TODO
 * @author Jonathan Hale (University of Konstanz)
 *
 */
public interface ColumnInputMatchingsService extends Service {
	
	Collection<ColumnInputMatching> getColumnInputMatchings();
	
	ColumnInputMatching getColumnInputMatching(String inputName);
	
}
