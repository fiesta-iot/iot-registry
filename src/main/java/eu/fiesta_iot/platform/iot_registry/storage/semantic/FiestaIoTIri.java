/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.semantic;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapperFactory;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapperType;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMappersFactory;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.rest.EndpointRestService;
import eu.fiesta_iot.platform.iot_registry.rest.ObservationRestService;
import eu.fiesta_iot.platform.iot_registry.rest.ResourceRestService;
import eu.fiesta_iot.platform.iot_registry.rest.TestbedRestService;

public class FiestaIoTIri {

	private static Logger log = LoggerFactory.getLogger(FiestaIoTIri.class);

	private static final String FIESTA_IOT_SERVICE_NAMESPACE =
	        IoTRegistryConfiguration.getInstance().getBaseUri();
	private static final String FIESTA_IOT_OBSERVATION_NAMESPACE =
	        UriBuilder.fromUri(FIESTA_IOT_SERVICE_NAMESPACE)
	                .path(ObservationRestService.class).build().toString();
	private static final String FIESTA_IOT_RESOURCE_NAMESPACE =
	        UriBuilder.fromUri(FIESTA_IOT_SERVICE_NAMESPACE)
	                .path(ResourceRestService.class).build().toString();
	private static final String FIESTA_IOT_ENDPOINT_NAMESPACE =
	        UriBuilder.fromUri(FIESTA_IOT_SERVICE_NAMESPACE)
	                .path(EndpointRestService.class).build().toString();
	private static final String FIESTA_IOT_TESTBED_NAMESPACE =
	        UriBuilder.fromUri(FIESTA_IOT_SERVICE_NAMESPACE)
	                .path(TestbedRestService.class).build().toString();

	String hash;
	EntityType type;

	private FiestaIoTIri(final EntityIdMapper rm) {
		hash = rm.getHash();
		type = rm.getType();
	}

	public static FiestaIoTIri create(final EntityIdMapper rm) {
		return new FiestaIoTIri(rm);
	}

	private FiestaIoTIri(final String hash, final EntityType type) {
		this.hash = hash;
		this.type = type;
	}

	public static FiestaIoTIri create(final String hash,
	                                  final EntityType type) {
		return new FiestaIoTIri(hash, type);
	}

	private FiestaIoTIri(final String iri) {
		// Remove Query parameters
		int paramsOffset = iri.lastIndexOf('?');
		String tmp =
		        (paramsOffset == -1) ? iri : iri.substring(0, paramsOffset);

		if (isResource(tmp)) {
			this.type = EntityType.RESOURCE;
			hash = getHash(tmp, FIESTA_IOT_RESOURCE_NAMESPACE);
		} else if (isObservation(tmp)) {
			this.type = EntityType.OBSERVATION;
			hash = getHash(tmp, FIESTA_IOT_OBSERVATION_NAMESPACE);
		} else if (isEndpoint(tmp)) {
			this.type = EntityType.ENDPOINT_URL;
			hash = getHash(tmp, FIESTA_IOT_ENDPOINT_NAMESPACE);
		} else if (isTestbed(tmp)) {
			this.type = EntityType.TESTBED;
			hash = getHash(tmp, FIESTA_IOT_TESTBED_NAMESPACE);
		} else {
			throw new IllegalArgumentException("IRI not matching FIESTA-IoT format");
		}
	}

	public static FiestaIoTIri create(final String iri) {
		return new FiestaIoTIri(iri);
	}

	public static FiestaIoTIri
	        createFromOriginalTestbedIri(final String iri,
	                                     final EntityType type) {
		EntityIdMapperType mapperType =
		        IoTRegistryConfiguration.getInstance().getEntityIdMapperClass();
		return createFromOriginalTestbedIri(iri, type, mapperType);
	}

	// In case using database storage for ids (unidirectional ids), the iri
	// might not being stored and therefore it will throw
	// InvalidEntityIdException as it cannot translate from FIESTA-IoT to
	// testbed
	public static FiestaIoTIri
	        createFromOriginalTestbedIri(final String iri,
	                                     final EntityType type,
	                                     final EntityIdMapperType mapperType) {
		EntityIdMapper entity;
		if (type == EntityType.RESOURCE) {
			entity = EntityIdMapperFactory.createResourceIdMapper(mapperType,
			                                                      iri);
		} else if (type == EntityType.OBSERVATION) {
			entity = EntityIdMapperFactory.createObservationIdMapper(mapperType,
			                                                         iri);
		} else if (type == EntityType.ENDPOINT_URL) {
			entity = EntityIdMapperFactory.createEndpointIdMapper(mapperType,
			                                                      iri);
		} else if (type == EntityType.TESTBED) {
			entity = EntityIdMapperFactory.createTestbedIdMapper(mapperType,
			                                                     iri);
		} else {
			// Parameter not supported
			throw new IllegalArgumentException("Param <type> value not supported");
		}

		return new FiestaIoTIri(entity);
	}

	public String
	        asOriginalTestbedIri(final EntityIdMapperType mapperType) throws InvalidEntityIdException {
		EntityIdMappers<?> entityMappers;
		if (type == EntityType.RESOURCE) {
			entityMappers =
			        EntityIdMappersFactory.createResourceIdMappers(mapperType);
		} else if (type == EntityType.OBSERVATION) {
			entityMappers = EntityIdMappersFactory
			        .createObservationIdMappers(mapperType);
		} else if (type == EntityType.ENDPOINT_URL) {
			entityMappers =
			        EntityIdMappersFactory.createEndpointIdMappers(mapperType);
		} else if (type == EntityType.TESTBED) {
			entityMappers =
			        EntityIdMappersFactory.createTestbedIdMappers(mapperType);
		} else {
			// Parameter not supported
			throw new IllegalArgumentException("Param <type> value not supported");
		}

		return entityMappers.get(hash).getUrl();

	}

	public String asOriginalTestbedIri() {
		EntityIdMapperType mapperType =
		        IoTRegistryConfiguration.getInstance().getEntityIdMapperClass();
		try {
			return asOriginalTestbedIri(mapperType);
		} catch (InvalidEntityIdException e) {
			// This exception is never thrown as the IRI is properly constructed
			// within the current class
			log.error("IRI not matching FIESTA-IoT format", e);
			return null;
		}
	}

	public String getHash() {
		return this.hash;
	}

	public EntityType getEntityType() {
		return this.type;
	}

	private static String getHash(String iri, String namespace) {
		iri = iri.substring(namespace.length() + 1);
		int slashOffset = iri.lastIndexOf('/');
		return (slashOffset == -1) ? iri : iri.substring(0, slashOffset);
	}

	public URI asUri() {
		String iri = asIri();
		return iri != null ? URI.create(iri) : null;
	}

	public String asIri() {
		switch (this.type) {
			case RESOURCE:
				return asResourceIri();
			case OBSERVATION:
				return asObservationIri();
			case ENDPOINT_URL:
				return asEndpointIri();
			case TESTBED:
				return asTestbedIri();
		}

		return null;
	}

	private String asObservationIri() {
		return asAnyIri(FIESTA_IOT_OBSERVATION_NAMESPACE);
	}

	private String asResourceIri() {
		return asAnyIri(FIESTA_IOT_RESOURCE_NAMESPACE);
	}

	private String asEndpointIri() {
		return asAnyIri(FIESTA_IOT_ENDPOINT_NAMESPACE);
	}

	private String asTestbedIri() {
		return asAnyIri(FIESTA_IOT_TESTBED_NAMESPACE);
	}

	private String asAnyIri(String any) {
		return UriBuilder.fromUri(any).path(hash).build().toString();
	}

	public static boolean isObservation(String iri) {
		return iri.startsWith(FIESTA_IOT_OBSERVATION_NAMESPACE);
	}

	public static boolean isResource(String iri) {
		return iri.startsWith(FIESTA_IOT_RESOURCE_NAMESPACE);
	}

	public static boolean isEndpoint(String iri) {
		return iri.startsWith(FIESTA_IOT_ENDPOINT_NAMESPACE);
	}

	public static boolean isTestbed(String iri) {
		return iri.startsWith(FIESTA_IOT_TESTBED_NAMESPACE);
	}

	public boolean isObservation() {
		return (type == EntityType.OBSERVATION);
	}

	public boolean isResource() {
		return (type == EntityType.RESOURCE);
	}

	public boolean isEndpoint() {
		return (type == EntityType.ENDPOINT_URL);
	}

	public boolean isTestbed() {
		return (type == EntityType.TESTBED);
	}
}
