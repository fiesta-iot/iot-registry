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

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualGraphsManager extends GraphsManager {
	private static Logger log =
	        LoggerFactory.getLogger(VirtualGraphsManager.class);

	private final NamedGraphsManager rmngr;
	private final NamedGraphsManager omngr;

	// Dataset is taken from resource
	public VirtualGraphsManager(NamedGraphsManager rmngr,
	        NamedGraphsManager omngr) {
		super(rmngr.getDataset());
		this.rmngr = rmngr;
		this.omngr = omngr;
	}

	@Override
	public Model getWriteModel() {
		throw new UnsupportedOperationException("Virtual graphs don't allow write operation");
	}

	@Override
	public List<String> getReadModel(LocalDateTime startDate,
	                                 LocalDateTime endDate) {
		// Get graphs for resource
		List<String> modelNames = rmngr.getReadModel(startDate, endDate);
		// Get graphs for observations
		modelNames.addAll(omngr.getReadModel(startDate, endDate));

		return modelNames;
	}

	@Override
	public void reset(boolean backup) {
		throw new UnsupportedOperationException("Virtual graphs don't allow reset operation");
	}
}
