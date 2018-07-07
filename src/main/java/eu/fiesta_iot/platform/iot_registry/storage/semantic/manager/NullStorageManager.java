/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.semantic.manager;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapperType;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotRegisteredResourceException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumModelException;

public class NullStorageManager extends StorageManager {

	private static Logger log = LoggerFactory.getLogger(NullStorageManager.class);

	public NullStorageManager() {
		super(EntityIdMapperType.AesCipher);
	}

	public NullStorageManager(EntityIdMapperType mapperType) {
		super(mapperType);
	}

	public Model adaptEntity(Model resourceInput) {
		return resourceInput;
	}

	public String getOriginalEntityId(String iri) {
		return iri;
	}

	public String getOriginalEntityId(String hash, EntityType type) {
		return hash;
	}

	@Override
	public boolean isManagedEntity(String iri) {
		// Validate namespace
		return true;
	}

	@Override
	public void
	        validateRegisteredParentEntity(Model observationInput) throws NotRegisteredResourceException {
		
	}

	@Override
	public void
	        validateMinimumDocument(Model observationInput) throws NotValidMinimumModelException {
	
	}

}
