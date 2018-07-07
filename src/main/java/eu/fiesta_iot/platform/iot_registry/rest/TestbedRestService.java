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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.ResourceListSerializerHelper;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.TestbedsListSerializerHelper;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.FiestaIoTIri;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotRegisteredResourceException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumModelException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.StorageManager.ValidationLevel;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadSparqlExecutor;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadSparqlResult;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.SparqlExecutionException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStore;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SemanticStorageLog;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SemanticStorageLogs;
import eu.fiesta_iot.utils.semantics.serializer.ModelSerializer;
import eu.fiesta_iot.utils.semantics.serializer.Serializer;
import eu.fiesta_iot.utils.semantics.serializer.SerializerFactory;
import eu.fiesta_iot.utils.semantics.serializer.exceptions.CannotSerializeException;
import eu.fiesta_iot.utils.semantics.vocabulary.Ssn;

@Path("/testbeds")
@RequestScoped
public class TestbedRestService {

	Logger log = LoggerFactory.getLogger(TestbedRestService.class);

	private static final String TESTBED_CLASS =
	        IoTRegistryConfiguration.getInstance().getTestbedOntologyClass();

	@Context
	private UriInfo uriInfo;

	@Context
	HttpServletRequest request;

	// @PersistenceContext(unitName = "FIESTAIoT", type =
	// PersistenceContextType.EXTENDED)
	// private EntityManager entityManager;

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
	
	private final TripleStore ts;

	public TestbedRestService() {
		// ts = new ResourceTripleStore();
		ts = TripleStoreFactory.createResourcesTripleStore();
	}

	/**
	 * List available testbeds.
	 *
	 * @param originalId
	 *            Flag to set whether to include original or testbed agnostic
	 *            resource's identifier
	 * 
	 * @return Response including a list of the testbeds' identifiers.
	 */
	@GET
	@Path("")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response
	        getTestbeds(@Context HttpHeaders headers,
	                    @DefaultValue("false") @QueryParam("original_id") boolean originalId) {
		List<String> testbeds = ts.getEntities(TESTBED_CLASS, originalId);

		return Response.ok().entity(new TestbedsListSerializerHelper(testbeds))
		        .build();
	}

	/**
	 * Register a new testbed
	 * 
	 * @param hash
	 *            Testbed identifier
	 * 
	 * @return Response including an RDF document.
	 */

	@POST
	@Path("")
	@Consumes({ MediaType.TEXT_PLAIN })
	@Produces({ MediaType.TEXT_PLAIN })
	public Response createTestbed(String iri, @Context HttpHeaders headers,
	                              @Context HttpServletRequest request) {

		String fiestaIri = createTestbed(iri);

		return Response.status(Status.CREATED).location(URI.create(fiestaIri))
		        .entity(fiestaIri).build();
	}

	/**
	 * Register a new testbed
	 * 
	 * @param hash
	 *            Testbed identifier
	 * 
	 * @return Response including an RDF document.
	 */

	@POST
	@Path("")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response createTestbeds(List<String> iris,
	                               @Context HttpHeaders headers,
	                               @Context HttpServletRequest request) {

		List<String> temp = iris.stream().map(i -> createTestbed(i))
		        .collect(Collectors.toList());

		return Response.status(Status.CREATED).entity(temp).build();
	}

	private String createTestbed(String iri) {
		try {
			// Create RDF statement
			Model model = ModelFactory.createDefaultModel();
			Resource deployment = model.createResource(iri)
			        .addProperty(RDF.type, Ssn.Deployment);
			
			SemanticStorageLog ol = semanticStorageLogs
			        .startLog(EntityType.TESTBED, headerRealIp, headerUsername, headerUserAgent);
			
			model = ts.addEntity(model, ValidationLevel.NONE);
			
			semanticStorageLogs.endLog(ol, false);
					
			
			// This can be done by using FiestaIoTIri class and would be quicker
			// and easier
			return model.listSubjects().next().getURI();
		} catch (RiotException ex) {
			throw new BadRequestException(ex.getMessage());
		} catch (NotRegisteredResourceException
		         | NotValidMinimumModelException ex) {
			throw new BadRequestException(ex.getMessage());
		}
	}

	/**
	 * Retrieve a testbed description.
	 * 
	 * @param hash
	 *            Testbed identifier
	 * 
	 * @return Response including an RDF document.
	 */
	@GET
	@Path("{id}")
	public Response getTestbed(@PathParam("id") String hash,
	                           @Context Request req) {
		if (hash == null || hash.trim().length() == 0) {
			return Response.serverError().entity("UUID cannot be blank")
			        .build();
		}

		// TODO: Try to find a way to get resources based only on substring
		// Using UriInfo it returns even the query params
		// String testbedIri = request.getRequestURL().toString();
		String testbedIri =
		        FiestaIoTIri.create(hash, EntityType.TESTBED).asIri();
		Model resource = ts.getEntity(testbedIri);
		if (resource == null) {
			// http://stackoverflow.com/questions/23858488/how-i-return-http-404-json-xml-response-in-jax-rs-jersey-on-tomcat
			// Need to register exception handler in boot class
			throw new NotFoundException("Testbed not found in triple store");
		}

		try {
			Serializer<?> renderer = SerializerFactory.getSerializer(resource);
			Variant variant =
			        req.selectVariant(renderer.listAvailableVariants());
			if (variant == null) {
				throw new CannotSerializeException();
			}

			String response = renderer.writeAs(variant.getMediaType());
			return Response.ok(response).build();
		} catch (CannotSerializeException ex) {
			// TODO: Modify to reuse the getAvailableVariants
			return Response
			        .notAcceptable(ModelSerializer.getAvailableVariants())
			        .build();
		}
	}

	/**
	 * List the resources linked to a testbed.
	 * 
	 * @param hash
	 *            Testbed identifier
	 * @param originalId
	 *            Flag to set whether to include original or testbed agnostic
	 *            resource's identifier
	 * 
	 * @return Response including a list of the resources (sensors) bound to a
	 *         testbed.
	 */
	@GET
	@Path("{id}/resources")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response
	        getTestbedResources(@PathParam("id") String hash,
	                            @DefaultValue("true") @QueryParam("original_id") boolean originalId,
	                            @Context HttpHeaders headers) {

		// String testbedIri = request.getRequestURL().toString();
		// testbedIri = testbedIri.substring(0, testbedIri.lastIndexOf('/'));
		String testbedIri =
		        FiestaIoTIri.create(hash, EntityType.TESTBED).asIri();

		String queryString =
		        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
		                     + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
		                     + "PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#> \n"
		                     + "PREFIX iot-lite: <http://purl.oclc.org/NET/UNIS/fiware/iot-lite#> \n"
		                     + " \n" + "SELECT DISTINCT ?entity ?type \n"
		                     + "WHERE { \n" 
		                     + "  # ssn:Devices \n" 
		                     + "  { \n"
		                     + "    ?entity ssn:hasDeployment <" + testbedIri + "> . \n" 
		                     + "  } \n" 
		                     + "  UNION \n"
		                     + "  # ssn:SensingDevices \n" 
		                     + "  { \n"
		                     + "    ?device ssn:hasDeployment <" + testbedIri + "> . \n"
		                     + "    ?device ssn:hasSubSystem ?entity . \n"
		                     + "  } \n"
//		                     + "  ?entity rdf:type ?type . \n"
//		                     + "  ?type rdfs:subClassOf* ssn:Device . \n"
		                     + "  ?entity iot-lite:hasQuantityKind ?qk . \n"
		                     + "  ?entity iot-lite:hasUnit ?qu . \n" + " \n"
		                     + "}";

		try (ReadSparqlExecutor exec =
		        ReadSparqlExecutor.create(queryString, ts)) {
			ReadSparqlResult res = exec.execute(false);
			ResultSet results = (ResultSet) res.getResult();

			ArrayList<String> resourcesList = new ArrayList<String>();
			while (results.hasNext()) {
				QuerySolution soln = results.next();
				RDFNode node = soln.get("entity");
				String resourceId = node.toString();
				if (originalId) {
					resourceId = ts.getOriginalEntityId(resourceId);
				}

				resourcesList.add(resourceId);
			}

			return Response.ok()
			        .entity(new ResourceListSerializerHelper(resourcesList))
			        .build();
		} catch (SparqlExecutionException ex) {
			// Shouldn't get here but in that case is that the SPARQL is not
			// supported
			throw new BadRequestException(ex.getMessage());
		} catch (InvalidEntityIdException e) {
			// It might happen if some resources have being registered and then
			// the configuration
			// (password, hash method, etc.) has been changed.
			throw new BadRequestException("Any of the entity ids is not valid. Please check server configuration");
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
	public Response deleteTestbed(@PathParam("id") String testbedId,
	                              @Context Request req,
	                              @Context HttpServletRequest request) {
		// Validate URI
		if (testbedId == null || testbedId.trim().length() == 0) {
			return Response.serverError().entity("UUID cannot be blank")
			        .build();
		}

		// Check headers before retrieving data
		MediaType acceptMediaType =
		        SemanticRegistryRestService.validateAcceptHeader(req);

		// TODO: Try to find a way to get resources based only on substring
		// Using UriInfo it returns even the query params
		String testbedIri =
		        FiestaIoTIri.create(testbedId, EntityType.TESTBED).asIri();
		Model resource = ts.deleteEntity(testbedIri);
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
}
