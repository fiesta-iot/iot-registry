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

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.rest.serializers.GraphsXmlSerializer;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadWriteTripleStore;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;
import eu.fiesta_iot.platform.iot_registry.storage.sql.QueryLog;
import eu.fiesta_iot.platform.iot_registry.storage.sql.QueryLogs;

@Path("utils/graphs")
@RequestScoped
public class GraphsRestService {

	private static final Logger log =
	        LoggerFactory.getLogger(GraphsRestService.class);

	@Inject
	QueryLogs queryLogs;

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
	@Path("observations")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<String> observationsGraphs() {
		ReadWriteTripleStore observationsTripleStore =
		        TripleStoreFactory.createObservationsTripleStore();
		return observationsTripleStore.getGraphsManager().listAvailableGraphs();
	}

	@GET
	@Path("observations")
	@Produces({ MediaType.APPLICATION_XML })
	public Response observationsGraphsXml() {
		ReadWriteTripleStore observationsTripleStore =
		        TripleStoreFactory.createObservationsTripleStore();
		Response response =
		        Response.ok(new GraphsXmlSerializer(observationsTripleStore
		                .getGraphsManager().listAvailableGraphs())).build();

		return response;
	}

	@HeaderParam("X-Real-IP")
	String headerRealIp;

	@HeaderParam("X-FIESTA-IoT-Component")
	String headerComponent;

	@HeaderParam("iPlanetDirectoryPro")
	String headerUserToken;
	
	@Context 
	HttpServletRequest request;

	@POST
	@Path("querylog")
	public Response test(String query) throws InterruptedException {

		headerRealIp = request.getRemoteAddr();
		QueryLog ql = queryLogs.startQueryLog(query, headerRealIp,
		                                      headerUserToken, headerComponent);
		Thread.sleep(2000);
		queryLogs.endQueryLog(ql, true);

		return Response.ok().build();
	}

}
