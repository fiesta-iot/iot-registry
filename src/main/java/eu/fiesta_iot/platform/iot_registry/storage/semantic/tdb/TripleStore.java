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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.util.MappingRegistry;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotRegisteredResourceException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumModelException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.StorageManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.StorageManager.ValidationLevel;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.namedgraph.GraphsManager;
import eu.fiesta_iot.utils.semantics.serializer.ModelDeserializer;
import eu.fiesta_iot.utils.semantics.serializer.exceptions.CannotDeserializeException;
import eu.fiesta_iot.utils.semantics.vocabulary.FiestaIoT;

public abstract class TripleStore {

	private static Logger log = LoggerFactory.getLogger(TripleStore.class);

	static {
		MappingRegistry.addPrefixMapping("fiesta-iot", IoTRegistryConfiguration
		        .getInstance().getBaseUri());
		FiestaIoT.PREFIX_MAP
		        .forEach((k, v) -> MappingRegistry.addPrefixMapping(k, v));

		TDB.setOptimizerWarningFlag(false);
		// Don't set TDB batch commits.
		// This can be slower, but it less memory hungry and more predictable.
		TransactionManager.QueueBatchSize = 0;
	}

	protected final Dataset dataset;
	protected final StorageManager storageManager;
	protected final GraphsManager graphsManager;
	
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	public TripleStore(GraphsManager graphsManager,
	        StorageManager storageManager) {

		this.graphsManager = graphsManager;
		this.dataset = graphsManager.getDataset();
		this.storageManager = storageManager;
	}

	public Dataset getDataset() {
		return dataset;
	}
	
	public GraphsManager getGraphsManager() {
		return graphsManager;
	}
	
	public void setStartDate(LocalDateTime date) {
		startDate = date;
	}

	public void setEndDate(LocalDateTime date) {
		endDate = date;
	}

	public List<String> getReadModel() {
		return graphsManager.getReadModel(startDate, endDate);
	}
	
	public String getWriteModel() {
		return graphsManager.getWriteModel();
	}
	
	
	public void
	        addResource(String filename) throws NotRegisteredResourceException,
	                                     NotValidMinimumModelException,
	                                     CannotDeserializeException {
		InputStream input;
		try {
			input = new FileInputStream(filename);
			addResource(input, "JSONLD");
			input.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Model addResource(InputStream input,
	                         Lang lang) throws NotRegisteredResourceException,
	                                    NotValidMinimumModelException,
	                                    CannotDeserializeException {
		return addResource(input, lang.getName());
	}

	public Model addResource(InputStream input,
	                         String lang) throws NotRegisteredResourceException,
	                                      NotValidMinimumModelException,
	                                      CannotDeserializeException {
		ModelDeserializer reader = new ModelDeserializer(input, lang);
		Model resourceInput = reader.read();

		return addEntity(resourceInput, ValidationLevel.NONE);
	}

	public Model addEntity(Model m) throws NotRegisteredResourceException,
	                                NotValidMinimumModelException {
		return addEntity(m, StorageManager.ValidationLevel.NONE);
	}

	public abstract Model
	        addEntity(Model m,
	                  EnumSet<StorageManager.ValidationLevel> validate) throws NotRegisteredResourceException,
	                                                                    NotValidMinimumModelException;

	public List<String> getAllEntities() {
		return getAllEntities(false);
	}

	public abstract List<String> getAllEntities(boolean originalId);

	public List<String> getEntities(String clazz) {
		return getEntities(clazz, false);
	}

	public abstract List<String> getEntities(String clazz, boolean originalId);

	public abstract Model getEntity(String iri);

	// Most probable to be removed when configuration functionality is included
	// Domain will be in the configuration and then the type will be
	// known from there
	// TODO: Validate id is managed by FIESTA
	public String
	        getOriginalEntityId(String id,
	                            EntityType type) throws InvalidEntityIdException {
		return storageManager.getOriginalEntityId(id, type);
	}

	public String
	        getOriginalEntityId(String id) throws InvalidEntityIdException {
		return storageManager.getOriginalEntityId(id);
	}

	public abstract Model deleteEntity(String iri);

	public void reset(boolean backup) {
		graphsManager.reset(backup);
	}
}
