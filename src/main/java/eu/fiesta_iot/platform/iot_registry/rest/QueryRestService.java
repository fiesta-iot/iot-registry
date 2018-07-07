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

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.util.ExprUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.EndLocalDateTimeParameter;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.QueriesListSerializerHelper;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.StartLocalDateTimeParameter;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadSparqlExecutor;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadSparqlResult;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.SparqlExecutionException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStore;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;
import eu.fiesta_iot.platform.iot_registry.storage.sql.QueryLog;
import eu.fiesta_iot.platform.iot_registry.storage.sql.QueryLogs;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQueries;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQuery;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQuery.Scope;
import eu.fiesta_iot.utils.semantics.serializer.Serializer;
import eu.fiesta_iot.utils.semantics.serializer.SerializerFactory;
import eu.fiesta_iot.utils.semantics.serializer.exceptions.CannotSerializeException;

@Path("/queries")
@RequestScoped
public class QueryRestService {

	private static String SPARQL_RESULT_TYPE_HEADER =
	        "X-FIESTA-IoT-Sparql-Result";
	private static String SPARQL_RESULTSET_DATA_HEADER =
	        "X-FIESTA-IoT-Sparql-ResultSet-HasData";

	private static final String INFERENCE_QUERY_PARAM_DEFAULT = "false";
	private static final String ASK_QUERY_PARAM_DEFAULT = "true";

	Logger log = LoggerFactory.getLogger(getClass());

	@Inject
	SparqlQueries sparqlQueries;

	@HeaderParam("X-Real-IP")
	String headerRealIp;

	@HeaderParam("X-FIESTA-IoT-Component")
	String headerComponent;

	@HeaderParam("User-Agent")
	String headerUserAgent;

	@HeaderParam("X-OpenAM-Username")
	String headerUsername;

	@Inject
	QueryLogs queryLogs;

	// // Seems we need to modify Wildfly configuration to create a datasource
	// // that enables MySQL
	// @PersistenceContext(unitName="FIESTAIoT")
	// protected EntityManager entityManager;

	/**
	 * List all the queries stored.
	 * 
	 * @return Response including a list of the queries.
	 */
	@GET
	@Path("store")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getQueries() {
		return Response
		        .ok(new QueriesListSerializerHelper(sparqlQueries.getAllIds()))
		        .build();
	}

	/**
	 * Retrieve an stored query.
	 * 
	 * @param queryId
	 *            Query identifier
	 * 
	 * @return Response including the query document.
	 */
	@GET
	@Path("store/{id}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getQuery(@PathParam("id") long queryId,
	                         @Context HttpHeaders headers) {
		SparqlQuery query = sparqlQueries.get(queryId);
		if (query == null) {
			throw new NotFoundException("Not found in repository");
		}

		return Response.ok(query).build();
	}

	/**
	 * List all the queries stored with an specific scope.
	 * 
	 * @param db
	 *            Scope the queries belong to
	 * 
	 * @return Response including the query document.
	 */
	@GET
	@Path("store/{db}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getQuery(@PathParam("db") String db,
	                         @Context HttpHeaders headers) {
		if (isValidQueryId(db)) {
			return getQuery(Long.parseLong(db), headers);
		}

		try {
			Scope scope = Scope.fromString(db);
			if (scope == Scope.GLOBAL) {
				return getQueries();
			} else {
				return Response.ok(new QueriesListSerializerHelper(sparqlQueries
				        .getAllIds(Scope.fromString(db)))).build();
			}
		} catch (IllegalArgumentException ex) {
			// throw new IllegalArgumentException("Given value (" + userName +
			// ") is not valid");
			throw new NotFoundException("Given path value (" + db
			                            + ") is not valid");
		}
	}

	/**
	 * Create a new query.
	 * 
	 * @param query
	 *            The query document, including SPARQL, name, description, etc.
	 *            Query id, if included, is not considered.
	 * 
	 * @return Response including the stored query document.
	 */
	@POST
	@Path("store")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response createQuery(SparqlQuery query, @Context UriInfo uriInfo) {
		// Validate template
		if (!query.isValidSparqlFormat()) {
			throw new BadRequestException("Error parsing SPARQL sentence. Format not valid.");
		}

		SparqlQuery queryDb;
		try {
			queryDb = sparqlQueries.add(query);
		} catch (PersistenceException ex) {
			ex.printStackTrace();
			throw new BadRequestException("Query description format error.");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BadRequestException("Unable to access database.");
		}

		UriBuilder builder = uriInfo.getAbsolutePathBuilder();
		builder.path(Long.toString(queryDb.getId()));

		return Response.created(builder.build()).entity(queryDb).build();
	}

	/**
	 * Create a new query.
	 * 
	 * @param db
	 *            Scope the new query belongs to.
	 * @param query
	 *            The query document, including SPARQL, name, description, etc.
	 *            Query id, if included, is not considered.
	 * 
	 * @return Response including the stored query document.
	 */
	@POST
	@Path("store/{db}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response createQuery(@PathParam("db") Scope db, SparqlQuery query,
	                            @Context UriInfo uriInfo) {
		query.setScope(db);

		return createQuery(query, uriInfo);
	}

	/**
	 * Update a query.
	 * 
	 * @param queryId
	 *            Query identifier
	 * @param query
	 *            The new query document, including SPARQL, name, description,
	 *            etc.
	 * 
	 * @return Response including the stored query document.
	 */
	@PUT
	@Path("store/{id}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response updateQuery(@PathParam("id") long queryId,
	                            SparqlQuery query) {
		SparqlQuery queryDb;
		try {
			queryDb = sparqlQueries.update(queryId, query);
		} catch (PersistenceException ex) {
			throw new BadRequestException();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BadRequestException("Unable to access database.");
		}

		if (queryDb == null) {
			throw new NotFoundException();
		}

		return Response.status(Status.OK).entity(queryDb).build();
	}

	/**
	 * Update a query.
	 * 
	 * @param db
	 *            Scope the new query belongs to.
	 * @param queryId
	 *            Query identifier
	 * @param query
	 *            The new query document, including SPARQL, name, description,
	 *            etc.
	 * 
	 * @return Response including the stored query document.
	 */
	@PUT
	@Path("store/{db}/{id}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response updateQuery(@PathParam("db") Scope db,
	                            @PathParam("id") long queryId,
	                            SparqlQuery query) {
		query.setScope(db);

		return updateQuery(queryId, query);
	}

	/**
	 * Delete a query.
	 * 
	 * @param queryId
	 *            Query identifier
	 * 
	 * @return Response including the stored query document.
	 */
	@DELETE
	@Path("store/{id}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response deleteQuery(@PathParam("id") long queryId) {

		try {
			SparqlQuery query = sparqlQueries.delete(queryId);
			if (query == null) {
				throw new NotFoundException();
			}
			return Response.status(Status.OK).entity(query).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new BadRequestException("Unable to access database.");
		}
	}

	/**
	 * Execute an stored SPARQL query.
	 * 
	 * @param queryId
	 *            the query
	 * 
	 * @return Response including the stored query document.
	 */
	@GET
	@Path("execute/{id}")
	public Response
	        executeQueryIdGet(@PathParam("id") long queryId,
	                          @QueryParam("from") StartLocalDateTimeParameter startDate,
	                          @QueryParam("to") EndLocalDateTimeParameter endDate,
	                          @DefaultValue(INFERENCE_QUERY_PARAM_DEFAULT) @QueryParam("inference") boolean inference,
	                          @DefaultValue(ASK_QUERY_PARAM_DEFAULT) @QueryParam("ask") boolean ask,
	                          @Context Request req, @Context UriInfo ui) {
		return executeQueryId(null, queryId, startDate, endDate, inference, ask,
		                      req, ui.getQueryParameters());
	}

	@POST
	@Path("execute/{id}")
	@Consumes("application/x-www-form-urlencoded")
	public Response
	        executeQueryIdPost(@PathParam("id") long queryId,
	                           @QueryParam("from") StartLocalDateTimeParameter startDate,
	                           @QueryParam("to") EndLocalDateTimeParameter endDate,
	                           @DefaultValue(INFERENCE_QUERY_PARAM_DEFAULT) @QueryParam("inference") boolean inference,
	                           @DefaultValue(ASK_QUERY_PARAM_DEFAULT) @QueryParam("ask") boolean ask,
	                           @Context Request req,
	                           MultivaluedMap<String, String> formParams) {

		return executeQueryId(null, queryId, startDate, endDate, inference, ask,
		                      req, formParams);
	}

	/**
	 * Execute an stored SPARQL query on an specific graph or triple store.
	 * 
	 * @param db
	 *            graph or triple store
	 * @param queryId
	 *            the query
	 * 
	 * @return Response including the stored query document.
	 */
	@GET
	@Path("execute/{db}/{id}")
	public Response
	        executeQueryIdGet(@PathParam("db") Scope db,
	                          @PathParam("id") long queryId,
	                          @QueryParam("from") StartLocalDateTimeParameter startDate,
	                          @QueryParam("to") EndLocalDateTimeParameter endDate,
	                          @DefaultValue(INFERENCE_QUERY_PARAM_DEFAULT) @QueryParam("inference") boolean inference,
	                          @DefaultValue(ASK_QUERY_PARAM_DEFAULT) @QueryParam("ask") boolean ask,
	                          @Context Request req, @Context UriInfo ui) {
		return executeQueryId(db, queryId, startDate, endDate, inference, ask,
		                      req, ui.getQueryParameters());
	}

	// This is for templates where parameters are passed in body
	@POST
	@Path("execute/{db}/{id}")
	@Consumes("application/x-www-form-urlencoded")
	public Response
	        executeQueryIdPost(@PathParam("db") Scope db,
	                           @PathParam("id") long queryId,
	                           @QueryParam("from") StartLocalDateTimeParameter startDate,
	                           @QueryParam("to") EndLocalDateTimeParameter endDate,
	                           @DefaultValue(INFERENCE_QUERY_PARAM_DEFAULT) @QueryParam("inference") boolean inference,
	                           @DefaultValue(ASK_QUERY_PARAM_DEFAULT) @QueryParam("ask") boolean ask,
	                           @Context Request req,
	                           MultivaluedMap<String, String> formParams) {

		return executeQueryId(db, queryId, startDate, endDate, inference, ask,
		                      req, formParams);
	}

	private Response
	        executeQueryId(Scope db, long queryId,
	                       StartLocalDateTimeParameter startDate,
	                       EndLocalDateTimeParameter endDate,
	                       @DefaultValue(INFERENCE_QUERY_PARAM_DEFAULT) @QueryParam("inference") boolean inference,
	                       @DefaultValue(ASK_QUERY_PARAM_DEFAULT) @QueryParam("ask") boolean ask,
	                       Request req,
	                       MultivaluedMap<String, String> templateVars) {
		SparqlQuery query = sparqlQueries.get(queryId);
		if (query == null) {
			throw new NotFoundException("Query not found");
		}

		if (db == null) {
			db = query.getScope();
		}

		if (query.getScope() != db && query.getScope() != Scope.GLOBAL) {
			return Response.status(422)
			        .entity("Linked query is not associated with resource")
			        .build();
		}

		List<String> remainingVars =
		        query.replaceTemplateVariables(templateVars);
		if (!remainingVars.isEmpty()) {
			return Response.status(422)
			        .entity("Missing variables in template: " + remainingVars)
			        .build();
		}

		return executeQuery(db, query.getValue(), startDate, endDate, inference,
		                    ask, req);
	}

	/**
	 * Execute a SPARQL query on an specific graph or triple store.
	 * 
	 * @param db
	 *            graph or triple store
	 * @param query
	 *            SPARQL query
	 * 
	 * @return Response including the stored query document.
	 */
	@POST
	@Path("execute/{db}")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response
	        executeQuery(@PathParam("db") Scope db, String queryString,
	                     @QueryParam("from") StartLocalDateTimeParameter startDate,
	                     @QueryParam("to") EndLocalDateTimeParameter endDate,
	                     @DefaultValue(INFERENCE_QUERY_PARAM_DEFAULT) @QueryParam("inference") boolean inference,
	                     @DefaultValue(ASK_QUERY_PARAM_DEFAULT) @QueryParam("ask") boolean ask,
	                     @Context Request req) {
		return executeQueryStream(db, queryString, startDate, endDate,
		                          inference, ask, req);
		// return executeQueryResponse(db, queryString, startDate, endDate,
		// inference, ask, req);
	}

	private TripleStore getTripleStore(Scope scope) {
		return TripleStoreFactory.createFromScope(scope);

		// // Select the triple store to launch the query
		// if (scope == Scope.RESOURCES) {
		// return new ResourceTripleStore();
		// } else if (scope == Scope.OBSERVATIONS) {
		// return new ObservationTripleStore();
		// } else {
		// log.debug("Using default value database (global) for executeQuery");
		// return new GlobalTripleStore();
		// }
	}

	private static boolean isValidQueryId(String str) {
		// return str.matches("-?\\d+(\\.\\d+)?"); //match a number with
		// optional '-' and decimal.
		return str.matches("\\d+"); // match a number with optional '-' and
		                            // decimal.
	}

	private boolean
	        askBeforeExecute(final Query query, TripleStore ts,
	                         boolean inference) throws SparqlExecutionException {
		// final String regex =
		// "SELECT[\\s|\\S]*?(WHERE[\\s|\\S]*\\{[\\s|\\S]*\\})";
		// final String subst = "ASK $1";

		Query ask = query.cloneQuery();
		ask.setQueryAskType();
		ask = QueryFactory.create(ask.toString());

		try (ReadSparqlExecutor exec = ReadSparqlExecutor.create(ask, ts)) {
			ReadSparqlResult result = exec.execute(inference);
			Boolean b = (Boolean) result.getResult();

			return b.booleanValue();
		}
	}

	private Query emptyResultSetQuery(final Query query) {
		Query emptySelect = QueryFactory.create();
		emptySelect.setQuerySelectType();
		query.getResultVars().stream()
		        .forEach(var -> emptySelect.addResultVar(var));
		ElementFilter filter = new ElementFilter(ExprUtils.parse("false"));
		emptySelect.setQueryPattern(filter);

		return QueryFactory.create(emptySelect.toString());
	}

	/**
	 * Execute a SPARQL query on an specific graph or triple store.
	 * 
	 * @param db
	 *            graph or triple store
	 * @param query
	 *            SPARQL query
	 * 
	 * @return Response including the stored query document.
	 */
	@POST
	@Path("response/{db}")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response
	        executeQueryResponse(@PathParam("db") Scope db, String queryString,
	                             @QueryParam("from") StartLocalDateTimeParameter startDate,
	                             @QueryParam("to") EndLocalDateTimeParameter endDate,
	                             @DefaultValue(INFERENCE_QUERY_PARAM_DEFAULT) @QueryParam("inference") boolean inference,
	                             @DefaultValue(ASK_QUERY_PARAM_DEFAULT) @QueryParam("ask") boolean ask,
	                             @Context Request req) {

		TripleStore ts = getTripleStore(db);

		// Search on restricted by dates graph
		if (startDate != null) {
			ts.setStartDate(startDate.getDateTime());
		}
		if (endDate != null) {
			ts.setEndDate(endDate.getDateTime());
		}

		QueryLog ql = queryLogs.startQueryLog(queryString, headerRealIp,
		                                      headerUsername, headerUserAgent);

		try {
			Query query = QueryFactory.create(queryString);

			if (query.isSelectType()) {
				if (ask) {
					boolean hasResults = askBeforeExecute(query, ts, inference);
					if (!hasResults) {
						// Create query for empty resultset
						query = emptyResultSetQuery(query);
					}
				}
			}

			try (ReadSparqlExecutor exec =
			        ReadSparqlExecutor.create(query, ts)) {
				ReadSparqlResult res = exec.execute(inference);
				Object obj = res.getResult();

				Serializer<?> renderer = SerializerFactory.getSerializer(obj);
				Variant variant =
				        req.selectVariant(renderer.listAvailableVariants());
				if (variant == null) {
					throw new CannotSerializeException();
				}

				Response.ResponseBuilder rsp =
				        Response.ok().type(variant.getMediaType());
				// Don't like it but it is the easiest solution for the
				// moment
				// Set headers to include type of data returned
				// Use Serializer as class names are known
				rsp.header(SPARQL_RESULT_TYPE_HEADER, renderer.getClass()
				        .getSimpleName().replace("Serializer", ""));
				if (obj instanceof ResultSet) {
					// rsp.header("X-FIESTA-IoT-Sparql-ResultSet", true);
					rsp.header(SPARQL_RESULTSET_DATA_HEADER,
					           ((ResultSet) obj).hasNext());
				}

				String result = renderer.writeAs(variant.getMediaType());
				Response response = rsp.entity(result).build();
				queryLogs.endQueryLog(ql, false);

				return response;
			}
		} catch (SparqlExecutionException | QueryParseException ex) {
			throw new BadRequestException(ex.getMessage());
		} catch (CannotSerializeException ex) {
			throw new WebApplicationException(Response.notAcceptable(null)
			        .build());
			// .notAcceptable(renderer.listAvailableVariants()).build());
		}
	}

	private static long MAX_REQUEST_INTERVAL =
	        60 * IoTRegistryConfiguration.getInstance().getMaxRequestInterval();

	public TripleStore
	        checkAndSetRequestInterval(TripleStore ts,
	                                   StartLocalDateTimeParameter startDate,
	                                   EndLocalDateTimeParameter endDate) {

		if (startDate != null) {
			LocalDateTime start = startDate.getDateTime();
			LocalDateTime end = (endDate == null) ? LocalDateTime.now()
			        : endDate.getDateTime();

			if (end.isAfter(start.plusSeconds(MAX_REQUEST_INTERVAL))) {
				throw new BadRequestException("The request cannot span over an interval of more than "
				                              + MAX_REQUEST_INTERVAL
				                              + " seconds");
			}
		} else if (endDate != null) {
			throw new BadRequestException("The request cannot span over an interval of more than "
			                              + MAX_REQUEST_INTERVAL + " seconds");
		}

		// Search on restricted by dates graph
		if (startDate != null) {
			ts.setStartDate(startDate.getDateTime());
		}
		if (endDate != null) {
			ts.setEndDate(endDate.getDateTime());
		}

		return ts;
	}

	@POST
	@Path("stream/{db}")
	@Consumes(MediaType.TEXT_PLAIN)
	public Response
	        executeQueryStream(@PathParam("db") Scope db, String queryString,
	                           @QueryParam("from") StartLocalDateTimeParameter startDate,
	                           @QueryParam("to") EndLocalDateTimeParameter endDate,
	                           @DefaultValue(INFERENCE_QUERY_PARAM_DEFAULT) @QueryParam("inference") boolean inference,
	                           @DefaultValue(ASK_QUERY_PARAM_DEFAULT) @QueryParam("ask") boolean ask,
	                           @Context Request req) {

		// Cannot check headers as possible output formats depends on the type
		// of SPARQL sentence that is used.
		// SELECT returns a ResultSet
		// DESCRIBE and CONSTRUCT return a Model
		// ASK returns a Boolean

		TripleStore ts = getTripleStore(db);

		checkAndSetRequestInterval(ts, startDate, endDate);

		QueryLog ql = queryLogs.startQueryLog(queryString, headerRealIp,
		                                      headerUsername, headerUserAgent);

		ReadSparqlExecutor exec = null;
		try {
			Query query = QueryFactory.create(queryString);

			// TODO: Return the real resultset, but include the header
			// Therefore change the emptyResultSetQuery to include the count or
			// group by, etc. Just remove the where clause
			if (query.isSelectType()) {
				if (ask) {
					boolean hasResults = askBeforeExecute(query, ts, inference);
					if (!hasResults) {
						// Create query for empty resultset
						query = emptyResultSetQuery(query);
					}
				}
			}

			exec = ReadSparqlExecutor.create(query, ts);
			ReadSparqlResult res = exec.execute(inference);
			Object obj = res.getResult();

			Serializer<?> renderer = SerializerFactory.getSerializer(obj);
			Variant variant =
			        req.selectVariant(renderer.listAvailableVariants());
			if (variant == null) {
				throw new CannotSerializeException();
			}

			Response.ResponseBuilder rsp =
			        Response.ok().type(variant.getMediaType());
			// Don't like it but it is the easiest solution for the moment
			// Set headers to include type of data returned
			// Use Serializer as class names are known
			rsp.header(SPARQL_RESULT_TYPE_HEADER, renderer.getClass()
			        .getSimpleName().replace("Serializer", ""));
			if (obj instanceof ResultSet) {
				// rsp.header("X-FIESTA-IoT-Sparql-ResultSet", true);
				rsp.header(SPARQL_RESULTSET_DATA_HEADER,
				           ((ResultSet) obj).hasNext());
			}

			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException,
				                                       WebApplicationException {
					boolean aborted = false;
					try {
						renderer.writeAs(output, variant.getMediaType());
					} catch (CannotSerializeException e) {
						log.error("Shouldn't have reached here", e);
						aborted = true;
					} catch (Exception e) {
						log.error("Unknown error", e);
						aborted = true;
					} finally {
						// Close ReadSparqlExecutor through ReadSparqlResult
						res.close();
						queryLogs.endQueryLog(ql, aborted);
					}
				}
			};

			return rsp.entity(stream).build();
		} catch (SparqlExecutionException | QueryParseException ex) {
			// Should get here but in that case is that the SPARQL is
			// not supported
			if (exec != null) {
				exec.close();
			}
			throw new BadRequestException(ex.getMessage());
		} catch (CannotSerializeException ex) {
			if (exec != null) {
				exec.close();
			}
			throw new WebApplicationException(Response.notAcceptable(null)
			        .build());
			// .notAcceptable(renderer.listAvailableVariants()).build());
		}

	}

	////////////////////////////////////////////////////////////////////

	// @Path("{id: \\d+}")
	@Path("{id}")
	@GET
	public String findUserById(@PathParam("id") long userId) {
		return "Find users by id <" + userId + ">";
	}

	// @Path("{name: resources|observations}")
	// @GET
	// public String findUserByName(@PathParam("name") String userName) {
	// return "RESOURCE Find users by name <" + userName + ">";
	// }

	@Path("{name}")
	@GET
	public String findUserByNameIdMix(@PathParam("name") String userName) {
		if (isValidQueryId(userName)) {
			return findUserById(Long.parseLong(userName));
		}

		try {
			Scope a = Scope.fromString(userName);
		} catch (IllegalArgumentException ex) {
			// throw new IllegalArgumentException("Given value (" + userName +
			// ") is not valid");
			throw new NotFoundException("Given path value (" + userName
			                            + ") is not valid");
		}

		return "OTROS Find users by name (mixed) <" + userName + ">";
	}

	@Path("{a: \\d+}-{b: \\d+}-{c: \\d+}")
	@GET
	public String findUserByGroupId(@PathParam("a") int a,
	                                @PathParam("b") int b,
	                                @PathParam("c") int c) {
		return "Collective ID is " + a + "-" + b + "-" + c;
	}
}
