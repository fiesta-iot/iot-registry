/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
 package eu.fiesta_iot.platform.iot_registry.storage.semantic.namedgraph;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NamedGraphsManager extends GraphsManager {
	private static Logger log =
	        LoggerFactory.getLogger(NamedGraphsManager.class);

	protected String baseName;

	public NamedGraphsManager(String baseName, Dataset dataset) {
		super(dataset);
		this.baseName = baseName;
	}

	@Override
	public String getWriteModel() {
		return getCurrentModel();
		
	}

	@Override
	public List<String> getReadModel(LocalDateTime startDate, LocalDateTime endDate) {
		return getIntervalModel(startDate, endDate);
	}

	protected abstract String getReadCurrentName();

	protected abstract String getWriteCurrentName();

	protected abstract List<String> getIntervalModel(LocalDateTime startDate,
	                                          LocalDateTime endDate);

	public abstract List<String> listAvailableGraphs();

	// In order to update the in-memory graph list
	protected abstract void addGraphName(String name);

	// In order not to call Dataset.containsNamedModel
	protected abstract boolean containsGraphName(String name);

	protected abstract void resetCurrentName();

	private synchronized String getCurrentModel() {
		return getWriteCurrentName();
	}

	/* 
	  By Andy Seaborne <andy@apache.org>
	 
	Personal preference:
		Txn.executeWrite(dataset,()->
		             UpdateAction.parseExecute("MOVE <g1> TO <g2>")
		                );
	
		You need to sort out the <g1> and <g2>
		(untested)
	
		MOVE works remotely.
		You could use the RDFConnection as well.
	
		It's not likely to be quicker - it's got to do the same amount of work and there is no TDB magic for this. 
	*/
	@Override
	public void reset(boolean backup) {
		if (backup) {
			resetCurrentName();
		} else {
			dataset.removeNamedModel(getReadCurrentName());
		}
	}
}
