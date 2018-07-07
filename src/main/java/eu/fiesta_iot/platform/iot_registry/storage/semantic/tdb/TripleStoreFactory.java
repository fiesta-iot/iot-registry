/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
 package eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.NullStorageManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.ObservationStorageManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.ResourceStorageManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.namedgraph.MultipleNamedGraphsManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.namedgraph.SingleNamedGraphsManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.namedgraph.VirtualGraphsManager;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQuery.Scope;

public final class TripleStoreFactory {

	private static Logger log =
	        LoggerFactory.getLogger(TripleStoreFactory.class);

	private static final String TRIPLE_STORE_PATH =
	        IoTRegistryConfiguration.getInstance().getTripleStorePath();

	private static Dataset dataset;
	
	private static SingleNamedGraphsManager resourcesNamedGraphManager =
	        new SingleNamedGraphsManager(IoTRegistryConfiguration.getInstance()
	                .getResourcesNamedModel(), dataset);

	private static MultipleNamedGraphsManager observationsNamedGraphManager =
	        new MultipleNamedGraphsManager(IoTRegistryConfiguration
	                .getInstance().getObservationsNamedModel(), dataset);

	private static VirtualGraphsManager virtualGraphManager =
	        new VirtualGraphsManager(resourcesNamedGraphManager,
	                                 observationsNamedGraphManager);

	private static ResourceStorageManager resourceStorageManager =
	        new ResourceStorageManager(IoTRegistryConfiguration.getInstance()
	                .getEntityIdMapperClass());

	private static ObservationStorageManager observationStorageManager =
	        new ObservationStorageManager(IoTRegistryConfiguration.getInstance()
	                .getEntityIdMapperClass());

	private static NullStorageManager nullStorageManager =
	        new NullStorageManager(IoTRegistryConfiguration.getInstance()
	                .getEntityIdMapperClass());

	public static void initializeDataset(String tripleStorePath) {
		dataset = TDBFactory.createDataset(TRIPLE_STORE_PATH);
		
		resourcesNamedGraphManager =
		        new SingleNamedGraphsManager(IoTRegistryConfiguration.getInstance()
		                .getResourcesNamedModel(), dataset);
		observationsNamedGraphManager =
		        new MultipleNamedGraphsManager(IoTRegistryConfiguration
		                .getInstance().getObservationsNamedModel(), dataset);
		virtualGraphManager =
		        new VirtualGraphsManager(resourcesNamedGraphManager,
		                                 observationsNamedGraphManager);

		resourceStorageManager =
		        new ResourceStorageManager(IoTRegistryConfiguration.getInstance()
		                .getEntityIdMapperClass());
		observationStorageManager =
		        new ObservationStorageManager(IoTRegistryConfiguration.getInstance()
		                .getEntityIdMapperClass());
		nullStorageManager =
		        new NullStorageManager(IoTRegistryConfiguration.getInstance()
		                .getEntityIdMapperClass());

		
	}
	
	public static void releaseDataset() {
		TDBFactory.release(dataset);;
		
		resourcesNamedGraphManager = null;
		observationsNamedGraphManager = null;
		virtualGraphManager = null; 
		
		resourceStorageManager = null;
		observationStorageManager = null;
		nullStorageManager = null;
	}

	public static Dataset getDataset() {
		return dataset;
	}
	
	public static RDFConnection getRdfConnection() {
		return RDFConnectionFactory.connect(dataset);
	}
	
	public static ReadWriteTripleStore createResourcesTripleStore() {
		return new ReadWriteTripleStore(resourcesNamedGraphManager,
		                                resourceStorageManager);
	}

	public static ReadWriteTripleStore createObservationsTripleStore() {
		return new ReadWriteTripleStore(observationsNamedGraphManager,
		                                observationStorageManager);
	}

	public static ReadOnlyTripleStore createGlobalTripleStore() {
		return new ReadOnlyTripleStore(virtualGraphManager, nullStorageManager);
	}

	public static TripleStore createFromScope(Scope scope) {
		// Select the triple store to launch the query
		if (scope == Scope.RESOURCES) {
			return createResourcesTripleStore();
		} else if (scope == Scope.OBSERVATIONS) {
			return createObservationsTripleStore();
		} else {
			log.debug("Using default value database (global) for executeQuery");
			return createGlobalTripleStore();
		}
	}
}
