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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.EndLocalDateTimeParameter;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.ListSerializerHelper;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.ResourceListSerializerHelper;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.FiestaIoTIri;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotRegisteredResourceException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumModelException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.StorageManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.StorageManager.ValidationLevel;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadSparqlExecutor;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadSparqlResult;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.SparqlExecutionException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStore;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SemanticStorageLog;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SemanticStorageLogs;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQueries;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQuery;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQuery.Scope;
import eu.fiesta_iot.utils.semantics.serializer.ModelDeserializer;
import eu.fiesta_iot.utils.semantics.serializer.ModelSerializer;
import eu.fiesta_iot.utils.semantics.serializer.ResultSetSerializer;
import eu.fiesta_iot.utils.semantics.serializer.Serializer;
import eu.fiesta_iot.utils.semantics.serializer.SerializerFactory;
import eu.fiesta_iot.utils.semantics.serializer.exceptions.CannotDeserializeException;
import eu.fiesta_iot.utils.semantics.serializer.exceptions.CannotSerializeException;

@RequestScoped
public abstract class SemanticRegistryRestService<T extends ListSerializerHelper<String>> {

	Logger log = LoggerFactory.getLogger(getClass());

	private final TripleStore ts;
	private final T lsh;
	private final Scope scope;
	private final EntityType entityType;

	@Inject
	SparqlQueries sparqlQueries;

	@Context
	UriInfo uri;

	@HeaderParam("X-Real-IP")
	String headerRealIp;

	@HeaderParam("X-FIESTA-IoT-Component")
	String headerComponent;

	@HeaderParam("User-Agent")
	String headerUserAgent;

	@HeaderParam("X-OpenAM-Username")
	String headerUsername;
	
	@Inject
	SemanticStorageLogs semanticStorageLogs;

	public SemanticRegistryRestService(TripleStore ts, T lsh,
	        EntityType entityType, Scope scope) {
		this.ts = ts;
		this.lsh = lsh;
		this.scope = scope;
		this.entityType = entityType;
	}

	/**
	 * List all available entities in the resources triple store. Filtering can
	 * be applied by setting the desired class IRI or by executing a previously
	 * stored SPARQL query (see /queries)
	 *
	 * @param originalId
	 *            Flag to set whether to include original or testbed agnostic
	 *            resource's identifier
	 * @param clazz
	 *            Class IRI
	 * @param queryId
	 *            Stored query
	 * 
	 * @return Response including a list of the resources' identifiers.
	 */
	/*
	 * IMPORTANT: THIS IS THE WAY IT SHOULD BE DONE. THE ONE CURRENTLY USED IS A
	 * WORKAROUND. DO CHECK COMMENTS HERE TO TRY TO FIX IT
	 * 
	 * @GET
	 * 
	 * @Path("") // TODO: Unable to make MediaType.APPLICATION_XML to work //
	 * Could not find MessageBodyWriter for response object of type: //
	 * java.util.ArrayList of media type: application/xml // It works if the
	 * list is encapsulated in a class and an instance of the // class is
	 * returned
	 * 
	 * @Produces({ MediaType.APPLICATION_JSON })
	 * // @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	 * // @Wrapped(element = "resources") public Response getResources() {
	 * log.trace("getResources");
	 * 
	 * List<String> resources = ts.getAllResources(); //
	 * GenericEntity<List<String>> entity = new //
	 * GenericEntity<List<String>>(resources) {};
	 * 
	 * return Response.ok().entity(new GenericEntity<List<String>>(resources)
	 * {}).build(); // return Response.ok(resources).build(); // return
	 * resources; }
	 * 
	 */
	@GET
	@Path("")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response
	        getResources(@DefaultValue("-1") @QueryParam("query") long queryId,
	                     @DefaultValue("false") @QueryParam("original_id") boolean originalId,
	                     @QueryParam("class") String clazz,
	                     @QueryParam("from") EndLocalDateTimeParameter startDate,
	                     @QueryParam("to") EndLocalDateTimeParameter endDate,
	                     @Context Request req) {

		// Search on this graph
		if (startDate != null) {
			ts.setStartDate(startDate.getDateTime());
		}
		if (endDate != null) {
			ts.setEndDate(endDate.getDateTime());
		}

		if (queryId != -1) {
			return getResourcesSparql(queryId, req);
		}

		List<String> resources;
		if (clazz == null) {
			resources = ts.getAllEntities(originalId);
		} else {
			log.debug("Looking for entities of class " + clazz);
			resources = ts.getEntities(clazz, originalId);
		}

		return Response.ok().entity(lsh.getInstance(resources)).build();
		// return Response.ok().entity(new
		// ResourceListSerializerHelper(resources)).build();
		// return
		// Response.ok().entity(lshClass.getDeclaredConstructor(List.class).newInstance(resources)).build();
	}

	private Response getResourcesSparql(long queryId, Request req) {
		SparqlQuery query = sparqlQueries.get(queryId);
		if (query == null) {
			throw new NotFoundException("Query not found");
		}
		if (query.getScope() != this.scope
		    && query.getScope() != Scope.GLOBAL) {
			return Response.status(422)
			        .entity("Linked query is not associated with resource")
			        .build();
		}

		try (ReadSparqlExecutor exec =
		        ReadSparqlExecutor.create(query.getValue(), ts)) {
			ReadSparqlResult res = exec.execute();
			Serializer<?> renderer =
			        SerializerFactory.getSerializer(res.getResult());
			Variant variant =
			        req.selectVariant(renderer.listAvailableVariants());
			if (variant == null) {
				throw new CannotSerializeException();
			}
			String response = renderer.writeAs(variant.getMediaType());
			return Response.ok(response).build();
		} catch (SparqlExecutionException ex) {
			// Should get here but in that case is that the SPARQL is not
			// supported
			throw new BadRequestException(ex.getMessage());
		} catch (CannotSerializeException ex) {
			return Response
			        .notAcceptable(ResultSetSerializer.getAvailableVariants())
			        .build();
		}
	}

	/**
	 * Retrieve a resource description.
	 *
	 * @param resourceId
	 *            Resource identifier
	 * 
	 * @return Response including an RDF document.
	 */
	@GET
	@Path("{id}")
	public Response getResource(@PathParam("id") String resourceId,
	                            @Context UriInfo uriInfo, @Context Request req,
	                            @Context HttpServletRequest request) {
		// Validate URI
		if (resourceId == null || resourceId.trim().length() == 0) {
			return Response.serverError().entity("UUID cannot be blank")
			        .build();
		}

		// Check headers before retrieving data
		MediaType acceptMediaType = validateAcceptHeader(req);

		// TODO: Try to find a way to get resources based only on substring
		// Using UriInfo it returns even the query params

		// Modification to enable the potential use of SSH tunnels
		// The IRI prefix is based on configuration file and not on HTTP request
		// parameters
		String resourceIri =
		        FiestaIoTIri.create(resourceId, entityType).asIri();
		// String resourceIri = request.getRequestURL().toString();
		// Not required any longer as overriden by configuration file
		// resourceIri = resourceIri.replace("localhost:8080",
		// "platform.fiesta-iot.eu");
		Model resource = ts.getEntity(resourceIri);
		if (resource == null) {
			// http://stackoverflow.com/questions/23858488/how-i-return-http-404-json-xml-response-in-jax-rs-jersey-on-tomcat
			// Need to register exception handler in boot class
			throw new NotFoundException("Entity not found in triple store");
		}

		try {
			Serializer<?> renderer = SerializerFactory.getSerializer(resource);
			String response = renderer.writeAs(acceptMediaType);
			return Response.ok(response, acceptMediaType).build();
		} catch (CannotSerializeException | RiotException ex) {
			return Response
			        .notAcceptable(ModelSerializer.getAvailableVariants())
			        .entity(ex.getMessage()).build();
		}
	}

	/**
	 * Retrieve the resource identifier originally registered by the testbed.
	 *
	 * @param resourceId
	 *            Resource identifier
	 * 
	 * @return Response including the identifier.
	 */

	@GET
	@Path("{id}/original_id")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getOriginalResourceId(@PathParam("id") String resourceId,
	                                      @Context HttpServletRequest request) {
		// Validate URI
		if (resourceId == null || resourceId.trim().length() == 0) {
			return Response.serverError().entity("UUID cannot be blank")
			        .build();
		}

		try {
			// String resourceIri = request.getRequestURL().toString();
			// resourceIri =
			// resourceIri.substring(0, resourceIri.lastIndexOf('/'));
			String origIri = ts.getOriginalEntityId(FiestaIoTIri
			        .create(resourceId, entityType).asIri());
			return Response.ok(origIri).build();
		} catch (InvalidEntityIdException e) {
			throw new NotFoundException("Entity not found");
		} catch (IllegalArgumentException e) {
			throw new NotFoundException("IRI not matching FIESTA-IoT format");
		}
	}

	private InputStream copyStream(InputStream input) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// Fake code simulating the copy
		// You can generally do better with nio if you need...
		// And please, unlike me, do something about the Exceptions :D
		byte[] buffer = new byte[1024];
		int len;
		try {
			while ((len = input.read(buffer)) > -1) {
				baos.write(buffer, 0, len);
			}
			baos.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// InputStream is2 = new ByteArrayInputStream(baos.toByteArray());
		// int n = in.available();
		// byte[] bytes = new byte[n];
		// in.read(bytes, 0, n);
		String s = new String(baos.toByteArray(), StandardCharsets.UTF_8); // Or
		                                                                   // any
		                                                                   // encoding.
		log.debug("Received document\n" + s);

		// Open new InputStreams using the recorded bytes
		// Can be repeated as many times as you wish
		return new ByteArrayInputStream(baos.toByteArray());
	}

	protected enum ValidationPolicy {
		TRUE,
		RANDOM,
		FALSE;

		public static ValidationPolicy fromString(String param) {
			String toUpper = param.toUpperCase();
			return valueOf(toUpper);
		}
	}

	protected abstract boolean validateRegistration();

	/**
	 * Create a new resource(s) based on its semantic description.
	 *
	 * @param input
	 *            The resource(s) semantic description.
	 * 
	 * @return Response including the stored resource(s) description.
	 */
	@POST
	@Path("")
	public Response
	        createResource(InputStream input,
	                       @DefaultValue("false") @QueryParam("validate") ValidationPolicy validationPolicy,
	                       @Context Request req,
	                       @Context HttpServletRequest request) {
		EnumSet<StorageManager.ValidationLevel> validate =
		        ValidationLevel.DEFAULT;
		if (validationPolicy == ValidationPolicy.RANDOM) {
			if (validateRegistration()) {
				validate = ValidationLevel.ALL;
			}
		} else if (validationPolicy == ValidationPolicy.TRUE) {
			validate = ValidationLevel.ALL;
		}

		// log.debug("Received data format: " + request.getContentType());
		// input = copyStream(input);

		// Check headers before retrieving data
		MediaType acceptMediaType = validateAcceptHeader(req);
		try {
			ModelDeserializer reader =
			        new ModelDeserializer(input, request.getContentType());
			Model resource = reader.read();

			SemanticStorageLog ol = semanticStorageLogs
			        .startLog(entityType, headerRealIp, headerUsername, headerUserAgent);

			resource = ts.addEntity(resource, validate);

			semanticStorageLogs.endLog(ol, false);

			Serializer<?> renderer = SerializerFactory.getSerializer(resource);
			String response = renderer.writeAs(acceptMediaType);
			resource.close();
			return Response.status(Status.CREATED).entity(response).build();
		} catch (RiotException ex) {
			throw new BadRequestException(ex.getMessage());
		} catch (CannotSerializeException ex) {
			return Response
			        .notAcceptable(ModelSerializer.getAvailableVariants())
			        .build();
		} catch (CannotDeserializeException e) {
			throw new NotSupportedException("Not supported language for semantic description");
		} catch (NotRegisteredResourceException
		         | NotValidMinimumModelException e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	/**
	 * Delete an entity from the triple store.
	 * 
	 * @param resourceId
	 *            the entity
	 * 
	 * @return Response with the deleted resource description.
	 */
	@DELETE
	@Path("{id}")
	public Response deleteResource(@PathParam("id") String resourceId,
	                               @Context Request req,
	                               @Context HttpServletRequest request) {
		// Validate URI
		if (resourceId == null || resourceId.trim().length() == 0) {
			return Response.serverError().entity("UUID cannot be blank")
			        .build();
		}

		// Check headers before retrieving data
		MediaType acceptMediaType = validateAcceptHeader(req);

		// TODO: Try to find a way to get resources based only on substring
		// Using UriInfo it returns even the query params
		String resourceIri = request.getRequestURL().toString();
		// resourceIri = resourceIri.replace("localhost:8080",
		// "platform.fiesta-iot.eu");
		log.debug(resourceIri);
		Model resource = ts.deleteEntity(resourceIri);
		if (resource == null) {
			// http://stackoverflow.com/questions/23858488/how-i-return-http-404-json-xml-response-in-jax-rs-jersey-on-tomcat
			// Need to register exception handler in boot class
			throw new NotFoundException("Entity not found in triple store");
		}

		// Returning 204 No content
		// return Response.noContent().build();

		// Return 200 with representation of the deleted resource
		try {
			Serializer<?> renderer = SerializerFactory.getSerializer(resource);
			String response = renderer.writeAs(acceptMediaType);
			resource.close();
			return Response.ok(response).build();
		} catch (CannotSerializeException ex) {
			return Response
			        .notAcceptable(ModelSerializer.getAvailableVariants())
			        .build();
		}
	}

	/**
	 * Delete an entity from the triple store.
	 * 
	 * @param resourceId
	 *            the entity
	 * 
	 * @return Response with the deleted resource description.
	 */
	@DELETE
	@Path("")
	public Response
	        deleteResources(@DefaultValue("true") @QueryParam("backup") boolean backup,
	                        @Context Request req,
	                        @Context HttpServletRequest request) {
		ts.reset(backup);

		return Response.ok().build();
	}

	// TODO: Move to ModelSerializer
	// We have to analyse if possible to send an object in the exception
	protected static MediaType validateAcceptHeader(Request req) {
		Variant variant =
		        req.selectVariant(ModelSerializer.getAvailableVariants());
		if (variant == null) {
			throw new WebApplicationException(Response
			        .notAcceptable(ModelSerializer.getAvailableVariants())
			        .build());
		}
		return variant.getMediaType();
	}
}
