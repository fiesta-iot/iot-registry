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

import java.util.EnumSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapperType;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotRegisteredResourceException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumModelException;

public abstract class StorageManager {

	private EntityIdMapperType entityIdMapperType;

	public StorageManager(EntityIdMapperType mapperType) {
		this.entityIdMapperType = mapperType;
	}

	protected EntityIdMapperType getEntityIdMapperType() {
		return entityIdMapperType;
	}

	public abstract Model adaptEntity(Model resourceInput);

	public enum ValidationLevel {
		PARENT(1 << 0),
		MINIMUM(1 << 1);

		public static final EnumSet<ValidationLevel> ALL =
		        EnumSet.allOf(ValidationLevel.class);
		public static final EnumSet<ValidationLevel> NONE =
		        EnumSet.noneOf(ValidationLevel.class);

		public static final EnumSet<ValidationLevel> DEFAULT = EnumSet.of(ValidationLevel.PARENT);
		
		private final int flag;

		private ValidationLevel(int flag) {
			this.flag = flag;
		}

		private int getValue() {
			return flag;
		}

		public EnumSet<ValidationLevel> getValue(int flags) {
			EnumSet<ValidationLevel> levels =
			        EnumSet.noneOf(ValidationLevel.class);
			for (ValidationLevel level : ValidationLevel.values()) {
				int levelValue = level.getValue();
				if ((flags & levelValue) == levelValue) {
					levels.add(level);
				}
			}

			return levels;
		}

		public int getValue(Set<ValidationLevel> flags) {
			int levels = 0;
			for (ValidationLevel flag : flags) {
				levels |= flag.getValue();
			}

			return levels;
		}
	}

	public void
	        validateEntity(Model resourceInput) throws NotRegisteredResourceException,
	                                            NotValidMinimumModelException {
		validateEntity(resourceInput, ValidationLevel.ALL);
	}

	public void
	        validateEntity(Model resourceInput,
	                       EnumSet<ValidationLevel> level) throws NotRegisteredResourceException,
	                                                       NotValidMinimumModelException {
		if (level.contains(ValidationLevel.PARENT)) {
			validateRegisteredParentEntity(resourceInput);
		}

		if (level.contains(ValidationLevel.MINIMUM)) {
			validateMinimumDocument(resourceInput);
		}
	}

	public abstract void
	        validateRegisteredParentEntity(Model observationInput) throws NotRegisteredResourceException;

	public abstract void
	        validateMinimumDocument(Model observationInput) throws NotValidMinimumModelException;

	// TODO: we should also validate that the full IRI is valid, including hash.
	// Validate namespace
	public abstract boolean isManagedEntity(String iri);

	// The Managers should be always in between the addition and retrieval of
	// data
	// as it might change the value
	// Exception is thrown when the IRI is not valid one
	public abstract String
	        getOriginalEntityId(String uri) throws InvalidEntityIdException;

	public abstract String
	        getOriginalEntityId(String id,
	                            EntityType type) throws InvalidEntityIdException;

	/**
	 * Retrieves the hash from an IRI in the following format
	 * http://whatever/hash
	 * 
	 * @param iri
	 *            the IRI
	 * 
	 * @return the hash value
	 */
	protected String getHash(String iri) {
		return iri.substring(iri.lastIndexOf('/') + 1, iri.length());
	}
}
