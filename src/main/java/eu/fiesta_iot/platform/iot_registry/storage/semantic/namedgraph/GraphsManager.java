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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GraphsManager {

	private static Logger log =
	        LoggerFactory.getLogger(GraphsManager.class);

	protected final Dataset dataset;

	public GraphsManager(Dataset dataset) {
		this.dataset = dataset;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public abstract String getWriteModel();

	public abstract List<String> getReadModel(LocalDateTime startDate, LocalDateTime endDate);
	
	public abstract void reset(boolean backup);
}