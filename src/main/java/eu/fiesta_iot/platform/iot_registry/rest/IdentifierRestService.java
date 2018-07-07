/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.rest;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

@Path("utils/identifier")
@RequestScoped
public class IdentifierRestService {

	Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Create a new query.
	 * 
	 * @param query
	 *            The query document, including SPARQL, name, description, etc.
	 *            Query id, if included, is not considered.
	 * 
	 * @return Response including the stored query document.
	 */
	@GET
	@Path("to_fiesta_iot")
	@Produces({ MediaType.TEXT_PLAIN })
	public Response getToFiestaIot(
	                               @DefaultValue("resource") @QueryParam("type") EntityType type,
	                               @QueryParam("value") String value) {
		try {
			List<String> values = new ArrayList<>();
			values.add(value);
			return Response.ok(convertToFiestaIoT(type, values).get(0)).build();
		} catch (IllegalArgumentException e) {
			return Response.status(422).entity(e.getMessage()).build();
		}
	}

	@POST
	@Path("to_fiesta_iot")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response postToFiestaIot(
	                                @DefaultValue("resource") @QueryParam("type") EntityType type,
	                                List<String> values) {

		try {
			return Response.ok(convertToFiestaIoT(type, values)).build();
		} catch (IllegalArgumentException e) {
			return Response.status(422).entity(e.getMessage()).build();
		}
	}

	private List<String> convertToFiestaIoT(EntityType type,
	                                        List<String> values) {
		EntityIdMapperType mapperType =
		        IoTRegistryConfiguration.getInstance().getEntityIdMapperClass();

		EntityIdMapper entity;
		if (type == EntityType.RESOURCE) {
			entity = EntityIdMapperFactory.createResourceIdMapper(mapperType,
			                                                      "dummy");
		} else if (type == EntityType.OBSERVATION) {
			entity = EntityIdMapperFactory.createObservationIdMapper(mapperType,
			                                                         "dummy");
		} else if (type == EntityType.ENDPOINT_URL) {
			entity = EntityIdMapperFactory.createEndpointIdMapper(mapperType,
			                                                      "dummy");
		} else if (type == EntityType.TESTBED) {
			entity = EntityIdMapperFactory.createTestbedIdMapper(mapperType,
			                                                     "dummy");
		} else {
			// Parameter not supported
			throw new IllegalArgumentException("Param <type> value not supported");
		}

		List<String> resp = new ArrayList<>(values.size());
		for (String value : values) {
			entity.setUrl(value);
			resp.add(entity.getHash());
		}

		return resp;
	}

	@GET
	@Path("to_testbed")
	@Produces({ MediaType.TEXT_PLAIN })
	public Response getToTestbed(
	                             @DefaultValue("resource") @QueryParam("type") EntityType type,
	                             @QueryParam("value") String value) {
		try {
			List<String> values = new ArrayList<>();
			values.add(value);
			return Response.ok(convertToTestbed(type, values).get(0)).build();
		} catch (InvalidEntityIdException e) {
			return Response.status(422)
			        .entity("Not valid FIESTA-IoT identifier found").build();
		} catch (IllegalArgumentException e) {
			return Response.status(422).entity(e.getMessage()).build();
		}
	}

	@POST
	@Path("to_testbed")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response postToTestbed(
	                              @DefaultValue("resource") @QueryParam("type") EntityType type,
	                              List<String> values) {

		try {
			return Response.ok(convertToTestbed(type, values)).build();
		} catch (InvalidEntityIdException e) {
			return Response.status(422)
			        .entity("Not valid FIESTA-IoT identifier found").build();
		} catch (IllegalArgumentException e) {
			return Response.status(422).entity(e.getMessage()).build();
		}

	}

	private List<String>
	        convertToTestbed(EntityType type,
	                         List<String> values) throws InvalidEntityIdException {
		EntityIdMapperType mapperType =
		        IoTRegistryConfiguration.getInstance().getEntityIdMapperClass();

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

		List<String> resp = new ArrayList<>(values.size());
		for (String hash : values) {
			resp.add(entityMappers.get(hash).getUrl());
		}

		return resp;
	}

}
