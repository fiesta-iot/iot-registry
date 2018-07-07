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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleNamedGraphsManager extends NamedGraphsManager {

	private static Logger log =
	        LoggerFactory.getLogger(SingleNamedGraphsManager.class);

	private final String currentName;

	public SingleNamedGraphsManager(String baseName, Dataset dataset) {
		super(baseName, dataset);
		this.currentName = baseName;
	}

	protected boolean isValidName(String name) {
		return baseName.equals(name);
	}

	@Override
	protected String getReadCurrentName() {
		return currentName;
	}

	@Override
	protected String getWriteCurrentName() {
		return currentName;
	}

	@Override
	protected void addGraphName(String name) {
		// Do nothing
		// Name is always the same
	}

	@Override
	protected boolean containsGraphName(String name) {
		return isValidName(name);
	}

	@Override
	public void resetCurrentName() {
		// Do nothing
		// Name is always the same
	}

	@Override
	protected List<String> getIntervalModel(LocalDateTime startDate,
	                                 LocalDateTime endDate) {
		return listAvailableGraphs();
		
	}

	@Override
	public List<String> listAvailableGraphs() {
		return new ArrayList<String>(Arrays.asList(currentName));
	}
}
