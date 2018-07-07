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

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapperType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.FiestaIoTIri;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadSparqlExecutor;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.ReadSparqlResult;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.SparqlExecutionException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStore;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;

@Path("/statistics")
@RequestScoped
public class StatisticsRestService {

	Logger log = LoggerFactory.getLogger(getClass());

	public StatisticsRestService() {
	}

	@GET
	@Path("")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getSummary() {
		return Response.ok(getSummaryStatistics()).build();
	}

	private Map<String, Integer> getSummaryStatistics() {
		String queryString = IoTRegistryConfiguration.getInstance()
		        .getSparqlStatisticsSummary();

		Map<String, Integer> statistics = new HashMap<>();

		try (ReadSparqlExecutor exec = ReadSparqlExecutor
		        .create(queryString,
		                TripleStoreFactory.createGlobalTripleStore())) {
			ReadSparqlResult result = exec.execute();
			ResultSet resultSet = (ResultSet) result.getResult();

			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();
				int deployments =
				        sol.get("count_deployments").asLiteral().getInt();
				int resources = sol.get("count_devices").asLiteral().getInt();
				int observations =
				        sol.get("count_observations").asLiteral().getInt();
				statistics.put("deployments", deployments);
				statistics.put("devices", resources);
				statistics.put("observations", observations);
			}
		} catch (SparqlExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return statistics;
	}

	/**
	 * Get the resource statistics statistics
	 * 
	 * @return Response including the information.
	 */
	// TODO: Should think on how to implement it in an async way so in case
	// latency is too
	// high, we get a better user experience
	@GET
	@Path("resources")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response
	        getResources(@DefaultValue("false") @QueryParam("original_id") boolean originalId) {
		return Response.ok(getResourcesStatistics(originalId)).build();
	}

	public Map<String, Map<String, Integer>>
	        getResourcesStatistics(boolean originalId) {
		Map<String, Map<String, Integer>> statistics =
		        new HashMap<String, Map<String, Integer>>();

		String queryString = IoTRegistryConfiguration.getInstance()
		        .getSparqlStatisticsResourcesSsnSensingDevicePerTestbed();

		TripleStore rts = TripleStoreFactory.createResourcesTripleStore();
		try (ReadSparqlExecutor exec =
		        ReadSparqlExecutor.create(queryString, rts)) {
			ReadSparqlResult result = exec.execute();
			ResultSet resultSet = (ResultSet) result.getResult();
			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				String deployment = sol.get("deployment").toString();
				if (originalId) {
					deployment = rts.getOriginalEntityId(deployment);
				}
				String type = sol.get("type").toString();
				int count = sol.get("count").asLiteral().getInt();

				Map<String, Integer> perDeployment = statistics.get(deployment);
				if (perDeployment == null) {
					perDeployment = new HashMap<String, Integer>();
					statistics.put(deployment, perDeployment);
				}

				perDeployment.put(type, Integer.valueOf(count));
			}
		} catch (SparqlExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidEntityIdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// for (Map.Entry<String, Map<String, Integer>> temp : statistics
		// .entrySet()) {
		// System.out.println(temp.getValue()); // Or something as per temp
		// // defination. can be used
		// }

		return statistics;
	}

	protected enum GroupByPolicy {
		TESTBED_AND_QK,
		QK,
		TESTBED;

		public static GroupByPolicy fromString(String param) {
			String toUpper = param.toUpperCase();
			return valueOf(toUpper);
		}
	}

	@GET
	@Path("observations")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response
	        getObservations(@DefaultValue("testbed_and_qk") @QueryParam("group_by") GroupByPolicy groupBy,
	                        @DefaultValue("false") @QueryParam("original_id") boolean originalId) {

		switch (groupBy) {
			case TESTBED_AND_QK:
				return Response
				        .ok(getObservationsByTestbedAndQkStatistics(originalId))
				        .build();
			case QK:
				return Response.ok(getObservationsByQuantityKindStatistics())
				        .build();
			case TESTBED:
				return Response
				        .ok(getObservationsByTestbedStatistics(originalId))
				        .build();
			default:
				throw new BadRequestException("Given value for parameter <group_by> is not currently supported");
		}
	}

	private Map<String, Map<String, Integer>>
	        getObservationsByTestbedAndQkStatistics(boolean originalId) {
		Map<String, Map<String, Integer>> statistics =
		        new HashMap<String, Map<String, Integer>>();

		String queryString = IoTRegistryConfiguration.getInstance()
		        .getSparqlStatisticsObservationsPerTestbedAndQk();

		try (ReadSparqlExecutor exec = ReadSparqlExecutor
		        .create(queryString,
		                TripleStoreFactory.createGlobalTripleStore())) {
			ReadSparqlResult result = exec.execute();
			ResultSet resultSet = (ResultSet) result.getResult();
			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				String deployment = sol.get("deployment").toString();
				if (originalId) {
					deployment = FiestaIoTIri.create(deployment)
					        .asOriginalTestbedIri(EntityIdMapperType.AesCipher);
					// gts.getOriginalEntityId(deployment);
				}
				String type = sol.get("type").toString();
				int count = sol.get("count").asLiteral().getInt();

				Map<String, Integer> perDeployment = statistics.get(deployment);
				if (perDeployment == null) {
					perDeployment = new HashMap<String, Integer>();
					statistics.put(deployment, perDeployment);
				}

				perDeployment.put(type, Integer.valueOf(count));
			}
		} catch (SparqlExecutionException | InvalidEntityIdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return statistics;
	}

	private Map<String, Integer>
	        getObservationsByTestbedStatistics(boolean originalId) {
		String queryString = IoTRegistryConfiguration.getInstance()
		        .getSparqlStatisticsObservationsPerTestbed();

		Map<String, Integer> statistics = new HashMap<>();

		TripleStore rts = originalId
		        ? TripleStoreFactory.createResourcesTripleStore() : null;
		try (ReadSparqlExecutor exec = ReadSparqlExecutor
		        .create(queryString,
		                TripleStoreFactory.createGlobalTripleStore())) {
			ReadSparqlResult result = exec.execute();
			ResultSet resultSet = (ResultSet) result.getResult();
			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				String deployment = sol.get("deployment").toString();
				if (originalId) {
					deployment = rts.getOriginalEntityId(deployment);
				}
				int count = sol.get("count").asLiteral().getInt();
				statistics.put(deployment, count);
			}
		} catch (SparqlExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidEntityIdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return statistics;

	}

	private Map<String, Integer> getObservationsByQuantityKindStatistics() {
		String queryString = IoTRegistryConfiguration.getInstance()
		        .getSparqlStatisticsObservationsPerQk();

		Map<String, Integer> statistics = new HashMap<>();

		try (ReadSparqlExecutor exec = ReadSparqlExecutor
		        .create(queryString,
		                TripleStoreFactory.createGlobalTripleStore())) {
			ReadSparqlResult result = exec.execute();
			ResultSet resultSet = (ResultSet) result.getResult();
			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();

				String qk = sol.get("type").toString();
				int count = sol.get("count").asLiteral().getInt();
				statistics.put(qk, count);
			}
		} catch (SparqlExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return statistics;
	}

}
