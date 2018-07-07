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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.RequestScoped;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;

import org.apache.jena.rdf.model.Model;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.spi.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.FiestaIoTIri;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.ObservationStorageManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStore;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;
import eu.fiesta_iot.utils.openam.Authentication;
import eu.fiesta_iot.utils.openam.Authorization;
import eu.fiesta_iot.utils.openam.Authorization.Action;
import eu.fiesta_iot.utils.semantics.serializer.ModelDeserializer;
import eu.fiesta_iot.utils.semantics.serializer.ModelSerializer;
import eu.fiesta_iot.utils.semantics.serializer.exceptions.CannotDeserializeException;
import eu.fiesta_iot.utils.semantics.serializer.exceptions.CannotSerializeException;

@Path("/endpoints")
@RequestScoped
public class EndpointRestService {

	private static Logger log =
	        LoggerFactory.getLogger(EndpointRestService.class);

	private static final String OPENAM_ROOT_ENDPOINT =
	        IoTRegistryConfiguration.getInstance().getOpenamUri();
	private static final String OPENAM_ADMIN_USERNAME =
	        IoTRegistryConfiguration.getInstance().getOpenamAdminUser();
	private static final String OPENAM_ADMIN_PASSWORD =
	        IoTRegistryConfiguration.getInstance().getOpenamAdminPass();

	private static List<String> LOCAL_QUERY_PARAMS =
	        Stream.of("policies", "agnostic").collect(Collectors.toList());

	private static final String POLICY_SET = "SmartSantander";
	private static final String REALM = "test";

	@Context
	private UriInfo uriInfo;

	@Context
	HttpServletRequest request;

	private final TripleStore ts;

	public EndpointRestService() {
		ts = TripleStoreFactory.createResourcesTripleStore();
	}

	private class EndpointObservation {
		@XmlElement(name = "endpoint")
		@JsonProperty(value = "endpoint")
		String endpoint;
		@XmlElement(name = "observation")
		@JsonProperty(value = "observation")
		String observation;

		public EndpointObservation(String endpoint, String observation) {
			this.endpoint = endpoint;
			this.observation = observation;
		}
	}

	/**
	 * Get the information from the endpoint.
	 * 
	 * @param hash
	 *            Endpoint internal identifier.
	 * 
	 * @return Response including the information.
	 */
	// TODO: Should think on how to implement it in an async way so in case
	// latency is too high, we get a better user experience
	// It returns HTTP errors depending on the behaviour. The mulitiple one
	// doesn't do
	@GET
	@Path("{id}")
	public Response
	        getEndpointValueGet(@PathParam("id") String hash,
	                            @HeaderParam("iPlanetDirectoryPro") String userToken,
	                            @DefaultValue("false") @QueryParam("policies") boolean checkPolicies,
	                            @DefaultValue("true") @QueryParam("agnostic") boolean agnostic,
	                            @Context Request req) {

		try {
			// Check headers before retrieving data
			MediaType acceptMediaType =
			        SemanticRegistryRestService.validateAcceptHeader(req);

			String testbedEndpoint =
			        ts.getOriginalEntityId(hash, EntityType.ENDPOINT_URL);
			log.debug("Accessing " + testbedEndpoint);

			if (checkPolicies) {
				if (!validateAccessRights(testbedEndpoint, userToken,
				                          POLICY_SET, Action.GET)) {
					throw new WebApplicationException("User access rights don't allow access to endpoint",
					                                  Status.FORBIDDEN);
				}
			}

			String queryParams = removeLocalQueryParams();

			EndpointObservation rsp =
			        requestGet(testbedEndpoint,
			                   uriInfo.getRequestUri().toString(), queryParams,
			                   acceptMediaType, agnostic);

			return Response.ok(rsp.observation, acceptMediaType).build();
		} catch (InvalidEntityIdException e) {
			throw new NotFoundException();
		} catch (IllegalArgumentException | IOException e) {
			throw new InternalServerErrorException(e.getMessage());
		}
	}

	@POST
	@Path("{id}")
	public Response
	        getEndpointValuePost(@PathParam("id") String hash,
	                             @HeaderParam("Content-Type") MediaType contentType,
	                             String body,
	                             @HeaderParam("iPlanetDirectoryPro") String userToken,
	                             @DefaultValue("false") @QueryParam("policies") boolean checkPolicies,
	                             @DefaultValue("true") @QueryParam("agnostic") boolean agnostic,
	                             @Context Request req) {

		try {
			// Check headers before retrieving data
			MediaType acceptMediaType =
			        SemanticRegistryRestService.validateAcceptHeader(req);

			String testbedEndpoint =
			        ts.getOriginalEntityId(hash, EntityType.ENDPOINT_URL);
			log.debug("Accessing " + testbedEndpoint);

			if (checkPolicies) {
				if (!validateAccessRights(testbedEndpoint, userToken,
				                          POLICY_SET, Action.POST)) {
					throw new WebApplicationException("User access rights don't allow access to endpoint",
					                                  Status.FORBIDDEN);
				}
			}

			String queryParams = removeLocalQueryParams();

			String rsp =
			        requestPost(testbedEndpoint,
			                    uriInfo.getRequestUri().toString(), queryParams,
			                    contentType, body, acceptMediaType, agnostic);

			return Response.ok(rsp, acceptMediaType).build();
		} catch (InvalidEntityIdException e) {
			throw new NotFoundException();
		} catch (IllegalArgumentException | IOException e) {
			throw new InternalServerErrorException(e.getMessage());
		}
	}

	@POST
	@Path("")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public List<EndpointObservation>
	        getEndpointsValue(List<String> endpoints,
	                          @HeaderParam("iPlanetDirectoryPro") String userToken,
	                          @DefaultValue("false") @QueryParam("policies") boolean checkPolicies,
	                          @DefaultValue("true") @QueryParam("agnostic") boolean agnostic,
	                          @Context Request req) {
		// Check headers before retrieving data
		MediaType acceptMediaType =
		        SemanticRegistryRestService.validateAcceptHeader(req);

		List<EndpointObservation> rsp =
		        _getEndpointsValue(endpoints, userToken, checkPolicies,
		                           acceptMediaType, agnostic);

		// return Response.ok(rsp).build();
		return rsp;
	}

	private List<EndpointObservation>
	        _getEndpointsValue(List<String> endpoints, String userToken,
	                           boolean checkPolicies, MediaType acceptMediaType,
	                           boolean agnostic) {
		Map<String, String> mapEndpoints = getOriginalTestbedIds(endpoints);
		// Should filter first based on different testbeds

		if (checkPolicies) {
			Map<String, Boolean> accessRights;
			try {
				accessRights =
				        validateAccessRights(mapEndpoints.keySet(), userToken,
				                             POLICY_SET, Action.GET);
			} catch (IOException e) {
				throw new InternalServerErrorException("Unable to validate access rights to requested endpoints",
				                                       e);
			}

			// Remove items not accessible
			List<String> nonAccesibleEndpoints =
			        accessRights.entrySet().parallelStream()
			                .filter(p -> Boolean.FALSE.equals(p.getValue()))
			                .map(p -> p.getKey()).collect(Collectors.toList());
			mapEndpoints.keySet().removeAll(nonAccesibleEndpoints);
		}

		// Query only for accessible items
		ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
		// Create list of task to retrieve observation for each sensor
		Collection<Callable<EndpointObservation>> tasks =
		        new ArrayList<>(endpoints.size());
		for (Map.Entry<String, String> pair : mapEndpoints.entrySet()) {
			Callable<EndpointObservation> task =
			        new Callable<EndpointObservation>() {
				        public EndpointObservation
				                call() throws IOException,
				                       InvalidEntityIdException {

					        String fiestaEndpoint = pair.getValue();
					        int queryStart = fiestaEndpoint.indexOf('?');
					        String fiestaQueryParams = queryStart != -1
					                ? fiestaEndpoint.substring(queryStart + 1)
					                : null;

					        // TODO: Maybe remove local parameters as well,
					        // At least to keep coherence
					        return requestGet(pair.getKey(), pair.getValue(),
					                          fiestaQueryParams,
					                          acceptMediaType, agnostic);
				        }
			        };
			tasks.add(task);
		}

		List<EndpointObservation> rsp = new ArrayList<>(endpoints.size());
		try {
			// Wait till all tasks have been executed
			List<Future<EndpointObservation>> futures =
			        taskExecutor.invokeAll(tasks);
			for (Future<EndpointObservation> future : futures) {
				try {
					rsp.add(future.get());
				} catch (InterruptedException e) {
					// TODO: Maybe it is the same error as in invokeAll, so use
					// the outer catch
					log.error(e.getLocalizedMessage());
				} catch (ExecutionException e) {
					// If provided callable threw exception in the past (the
					// exception is stored in the Future).
					// Exception thrown when attempting to retrieve the result
					// of a task that aborted by throwing an exception. This
					// exception can be inspected using the Throwable.getCause()
					// method.
					log.warn(e.getLocalizedMessage());
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			taskExecutor.shutdown();
		}

		return rsp;
	}

	private static Builder
	        buildRequest(String testbedEndpoint,
	                     String queryParams) throws MalformedURLException {
		Client client = createClient();

		// Check in case testbed endpoint includes query parameters
		testbedEndpoint = getFullTestbedEndpoint(testbedEndpoint, queryParams);
		log.debug("Requesting endpoint : " + testbedEndpoint);

		WebTarget wt = client.target(testbedEndpoint);
		return wt.request();
	}

	private static String
	        handleResponse(Response rsp, MediaType acceptMediaType,
	                       boolean agnostic, String testbedEndpoint,
	                       String fiestaEndpoint) throws IOException {
		// Response rsp = bldr.accept(mediaType).get();
		if (rsp.getStatus() != Response.Status.OK.getStatusCode()) {
			StatusType status = rsp.getStatusInfo();
			rsp.close();
			throw new IOException("HTTP error " + status.getStatusCode()
			                      + " with message " + status.getReasonPhrase()
			                      + " accessing " + testbedEndpoint + " ("
			                      + fiestaEndpoint + ")");
		}

		String rspMediaType =
		        rsp.getMediaType().toString().split(";")[0].trim();

		// Change RDF format to the desired one
		return changeRdfFormat(rsp.readEntity(String.class), rspMediaType,
		                       acceptMediaType, agnostic);

	}

	private static String
	        getFullTestbedEndpoint(final String testbedEndpoint,
	                               final String queryParams) throws MalformedURLException {
		// Check in case testbed endpoint includes query parameters
		URL tmpUrl = new URL(testbedEndpoint);
		if (queryParams != null) {
			return testbedEndpoint + (tmpUrl.getQuery() != null ? "&" : "?")
			       + queryParams;
		}

		return testbedEndpoint;
	}

	private static String changeRdfFormat(String input, String fromLang,
	                                      MediaType toLang,
	                                      boolean agnostic) throws IOException {
		try {
			InputStream is = new ByteArrayInputStream(input
			        .getBytes(StandardCharsets.UTF_8.name()));

			ModelDeserializer reader = new ModelDeserializer(is, fromLang);
			Model resource = reader.read();

			if (agnostic) {
				ObservationStorageManager om =
				        new ObservationStorageManager(IoTRegistryConfiguration
				                .getInstance().getEntityIdMapperClass());
				resource = om.adaptEntity(resource);
			}

			ModelSerializer writer = new ModelSerializer(resource);
			return writer.writeAs(toLang);
		} catch (UnsupportedEncodingException | CannotSerializeException
		         | CannotDeserializeException e) {
			// Probably that will never happens as we can check the media types
			// prior to manage the data request
			throw new IOException("RDF format not supported");
		}
	}

	private EndpointObservation
	        requestGet(String testbedEndpoint, String fiestaEndpoint,
	                   String queryParams, MediaType acceptMediaType,
	                   boolean agnostic) throws IOException,
	                                     InvalidEntityIdException {
		Response rsp = buildRequest(testbedEndpoint, queryParams).get();
		String entity = handleResponse(rsp, acceptMediaType, agnostic,
		                               testbedEndpoint, fiestaEndpoint);

		return new EndpointObservation(fiestaEndpoint, entity);
	}

	private String requestPost(String testbedEndpoint, String fiestaEndpoint,
	                           String queryParams, MediaType contentType,
	                           String body, MediaType acceptMediaType,
	                           boolean agnostic) throws IOException,
	                                             InvalidEntityIdException {
		Response rsp = buildRequest(testbedEndpoint, queryParams)
		        .post(Entity.entity(body, contentType));
		return handleResponse(rsp, acceptMediaType, agnostic, testbedEndpoint,
		                      fiestaEndpoint);
	}

	/**
	 * Get the original endpoint URL.
	 * 
	 * @param hash
	 *            Endpoint internal identifier.
	 * 
	 * @return Response including the original link.
	 */
	@GET
	@Path("{id}/original_id")
	public Response getEndpoint(@PathParam("id") String hash,
	                            @Context HttpHeaders headers) {
		try {
			String endpoint =
			        ts.getOriginalEntityId(hash, EntityType.ENDPOINT_URL);

			// URI uri = rdfLiteralToUri(endpoint);

			return Response.ok(endpoint).build();
		} catch (InvalidEntityIdException e) {
			throw new NotFoundException();
		} catch (IllegalArgumentException e) {
			throw new InternalServerErrorException("Not valid literal for representing endpoint");
		}
	}

	private Map<String, String>
	        getOriginalTestbedIds(List<String> fiestaResources) {
		Map<String, String> testbedResources =
		        new HashMap<>(fiestaResources.size());
		for (String resource : fiestaResources) {
			try {
				// Remove query params
				int queryStart = resource.indexOf('?');
				String fiestaUrl = queryStart != -1
				        ? resource.substring(0, queryStart) : resource;

				if (FiestaIoTIri.isEndpoint(fiestaUrl)) {
					testbedResources.put(ts.getOriginalEntityId(fiestaUrl),
					                     resource);
				}
			} catch (InvalidEntityIdException e) {
				log.debug("Endpoint " + resource + " is not valid");
			}
		}

		return testbedResources;
	}

	// Policy set should be testbed IRI
	private Map<String, Boolean>
	        validateAccessRights(Collection<String> resources, String userToken,
	                             String policySet,
	                             Action action) throws IOException {
		Authentication auth = new Authentication(OPENAM_ROOT_ENDPOINT);
		auth.login(OPENAM_ADMIN_USERNAME, OPENAM_ADMIN_PASSWORD);

		Authorization authz = new Authorization(auth);
		authz.setRealm(REALM);
		Map<String, Boolean> res = authz
		        .checkAccessRightsFor(action, userToken, resources, policySet);
		auth.logout();

		return res;
	}

	// Policy set should be testbed IRI
	private boolean validateAccessRights(String resource, String userToken,
	                                     String policySet,
	                                     Action action) throws IOException {
		Authentication auth = new Authentication(OPENAM_ROOT_ENDPOINT);
		auth.login(OPENAM_ADMIN_USERNAME, OPENAM_ADMIN_PASSWORD);

		Authorization authz = new Authorization(auth);
		authz.setRealm(REALM);
		boolean res = authz.checkAccessRightsFor(action, userToken, resource,
		                                         policySet);
		auth.logout();

		return res;
	}

	private static Client createClient() {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("TLSv1.2");

			// Set up a TrustManager that trusts everything
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs,
				                               String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs,
				                               String authType) {
				}
			} }, new SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			log.warn(e.getMessage());
		} catch (KeyManagementException e) {
			log.warn(e.getMessage());
		}

		// Proxy implementation to get remote data
		Client client = new ResteasyClientBuilder()
		        .establishConnectionTimeout(100, TimeUnit.SECONDS)
		        .socketTimeout(2, TimeUnit.SECONDS).sslContext(sslContext)
		        .hostnameVerifier(new HostnameVerifier() {
			        public boolean verify(String s, SSLSession sslSession) {
				        return true;
			        }
		        })
		        // .hostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
		        // .hostnameVerifier(new NoopHostnameVerifier());
		        .build();

		return client;
	}

	private String removeLocalQueryParams() {
		// Remove local query params
		// https://stackoverflow.com/questions/41063948/how-to-remove-a-query-parameter-from-a-query-string
		String queryParams = uriInfo.getRequestUri().getRawQuery();
		if (queryParams != null) {
			queryParams = Arrays.stream(queryParams.split("&"))
			        .map(param -> new SimpleEntry<>(param.split("=")[0],
			                                        param.split("=")[1]))
			        .filter(e -> !LOCAL_QUERY_PARAMS
			                .contains(e.getKey().toLowerCase()))
			        .map(e -> e.getKey() + "=" + e.getValue())
			        .collect(Collectors.joining("&"));
		}

		return queryParams;
	}
}
