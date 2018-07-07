/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapperType;
import eu.fiesta_iot.platform.utils.commons.configuration.PropertyManagement;

public class IoTRegistryConfiguration extends PropertyManagement {
	
	private static final String PROPERTIES_FILE = "fiesta-iot.properties";

	private static final String API_BASE_URI = "iot-registry.api_base_uri";
	private static final String TRIPLE_STORE_PATH = "iot-registry.triple_store.path";
	private static final String SPATIAL_INDEX_PATH =
	        "iot-registry.spatial_index.path";
	// private static String RESOURCES_NAMED_MODEL =
	// "iot-registry.triple_store.resources_named_model";
	// private static String OBSERVATIONS_NAMED_MODEL =
	// "iot-registry.triple_store.observations_named_model";
	private static final String ENTITY_ID_MAPPER_CLASS =
	        "iot-registry.manager.entity_id_mapper_class";
	private static final String RESOURCE_ID_MAPPER_PASSWORD =
	        "iot-registry.manager.resource_id_mapper_password";
	private static final String OBSERVATION_ID_MAPPER_PASSWORD =
	        "iot-registry.manager.observation_id_mapper_password";
	private static final String ENDPOINT_ID_MAPPER_PASSWORD =
	        "iot-registry.manager.endpoint_id_mapper_password";
	private static final String TESTBED_ONTOLOGY_CLASS =
	        "iot-registry.rest.testbed_ontology_class";

	private static final String VALIDATE_RESOURCE_REGISTRATION_PERCENTAGE =
	        "iot-registry.manager.validate_resource_registration_percentage";
	private static final String VALIDATE_OBSERVATION_REGISTRATION_PERCENTAGE =
	        "iot-registry.manager.validate_observation_registration_percentage";

	private static final String SPARQL_TEMPLATE_VAR_DELIMITER =
	        "iot-registry.sparql_template.var_delimiter";

	private static final String SPARQL_OBSERVATION_MINIMUM_VALID_DOCUMENT =
	        "etc/sparqls/observation_minimum_valid_model.sparql";
	private String sparqlObservationMinimumValidModel;
	private static final String SPARQL_OBSERVATION_TOTAL_SSN_OBSERVATION =
	        "etc/sparqls/observation_total_ssn_observation.sparql";
	private String sparqlObservationTotalSsnObservation;
	private static final String SPARQL_RESOURCE_MINIMUM_VALID_DOCUMENT =
	        "etc/sparqls/resource_minimum_valid_model.sparql";
	private String sparqlResourceMinimumValidModel;
	private static final String SPARQL_RESOURCE_TOTAL_SSN_SENSINGDEVICE =
	        "etc/sparqls/resource_total_ssn_sensingdevice.sparql";
	private String sparqlResourceTotalSsnSensingDevice;

	private static final String SPARQL_STATISTICS_SUMMARY =
	        "etc/sparqls/statistics_summary.sparql";
	private String sparqlStatisticsSummary;
	private static final String SPARQL_STATISTICS_RESOURCES_SSN_SENSINGDEVICE_PER_TESTBED =
	        "etc/sparqls/statistics_resources_ssn_sensingdevice_per_testbed.sparql";
	private String sparqlStatisticsResourcesSsnSensingDevicePerTestbed;
	private static final String SPARQL_STATISTICS_OBSERVATIONS_PER_TESTBED =
	        "etc/sparqls/statistics_observations_per_testbed.sparql";
	private String sparqlStatisticsObservationsPerTestbed;
	private static final String SPARQL_STATISTICS_OBSERVATIONS_PER_QK =
	        "etc/sparqls/statistics_observations_per_quantity_kind.sparql";
	private String sparqlStatisticsObservationsPerQk;
	private static final String SPARQL_STATISTICS_OBSERVATIONS_PER_TESTBED_AND_QK =
	        "etc/sparqls/statistics_observations_per_testbed_and_qk.sparql";
	private String sparqlStatisticsObservationsPerTestbedAndQk;
	
	private static final String SECURITY_OPENAM_URI =
			"iot-registry.security.openam.uri";
	private static final String SECURITY_OPENAM_ADMIN_USERNAME =
	        "iot-registry.security.openam.admin_user";
	private static final String SECURITY_OPENAM_ADMIN_PASSWORD =
	        "iot-registry.security.openam.admin_pass";
		
	// In minutes
	private static final String GRAPHS_BACKUP_PERIOD =
	        "iot-registry.graphs.backup_period";
	
	// In minutes
	private static final String MAX_REQUEST_INTERVAL =
	        "iot-registry.graphs.max_request_interval"; 
		
	private static IoTRegistryConfiguration instance;

	private ServletContext servletContext;

	private IoTRegistryConfiguration() {
		super(PROPERTIES_FILE);
		
		// Read files to String
		// TODO: Change for ServletContext and analyze implications
		this.sparqlObservationMinimumValidModel =
		        readResourcesFileContent(SPARQL_OBSERVATION_MINIMUM_VALID_DOCUMENT);
		this.sparqlObservationTotalSsnObservation =
		        readResourcesFileContent(SPARQL_OBSERVATION_TOTAL_SSN_OBSERVATION);
		this.sparqlResourceMinimumValidModel =
		        readResourcesFileContent(SPARQL_RESOURCE_MINIMUM_VALID_DOCUMENT);
		this.sparqlResourceTotalSsnSensingDevice =
		        readResourcesFileContent(SPARQL_RESOURCE_TOTAL_SSN_SENSINGDEVICE);
		this.sparqlStatisticsSummary =
		        readResourcesFileContent(SPARQL_STATISTICS_SUMMARY);
		this.sparqlStatisticsResourcesSsnSensingDevicePerTestbed =
		        readResourcesFileContent(SPARQL_STATISTICS_RESOURCES_SSN_SENSINGDEVICE_PER_TESTBED);
		this.sparqlStatisticsObservationsPerTestbed =
		        readResourcesFileContent(SPARQL_STATISTICS_OBSERVATIONS_PER_TESTBED);
		this.sparqlStatisticsObservationsPerQk =
		        readResourcesFileContent(SPARQL_STATISTICS_OBSERVATIONS_PER_QK);
		this.sparqlStatisticsObservationsPerTestbedAndQk =
		        readResourcesFileContent(SPARQL_STATISTICS_OBSERVATIONS_PER_TESTBED_AND_QK);
	}

	public static IoTRegistryConfiguration getInstance() {
		if (instance == null) {
			instance = new IoTRegistryConfiguration();
		}

		return instance;
	}

	private String readResourcesFileContent(String filename) {
		// Read files to String
		// TODO: Change for ServletContext and analyze implications
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		InputStream is = ccl.getResourceAsStream(filename);
		return new BufferedReader(new InputStreamReader(is)).lines()
		        .collect(Collectors.joining("\n"));
	}

	/**
	 * Method to access servlet context from any location
	 */
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * Called by ServletContextRetrieverListener
	 */
	protected void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public String getBaseUri() {
		return getProperty(API_BASE_URI);
	}

	public String getTripleStorePath() {
		return getProperty(TRIPLE_STORE_PATH);
	}

	public String getSpatialIndexPath() {
		return getProperty(SPATIAL_INDEX_PATH);
	}

	public String getResourcesNamedModel() {
		return getBaseUri() + "/" + "resources";
		// return props.getProperty(RESOURCES_NAMED_MODEL);
	}

	public String getObservationsNamedModel() {
		return getBaseUri() + "/" + "observations";
		// return props.getProperty(OBSERVATIONS_NAMED_MODEL);
	}

	public EntityIdMapperType getEntityIdMapperClass() {
		String value = getProperty(ENTITY_ID_MAPPER_CLASS);
		return EntityIdMapperType.valueOf(value);
	}

	public String getResourceIdMapperPassword() {
		return getProperty(RESOURCE_ID_MAPPER_PASSWORD);
	}

	public String getObservationIdMapperPassword() {
		return getProperty(OBSERVATION_ID_MAPPER_PASSWORD);
	}

	public String getEndpointMapperPassword() {
		return getProperty(ENDPOINT_ID_MAPPER_PASSWORD);
	}

	public int getValidateResourceRegistrationPercentage() {
		return Integer.valueOf(getProperty(VALIDATE_RESOURCE_REGISTRATION_PERCENTAGE));
	}

	public int getValidateObservationRegistrationPercentage() {
		return Integer.valueOf(getProperty(VALIDATE_OBSERVATION_REGISTRATION_PERCENTAGE));
	}

	public String getTestbedOntologyClass() {
		return getProperty(TESTBED_ONTOLOGY_CLASS);
	}

	public String getSparqlTemplateVarDelimiter() {
		return getProperty(SPARQL_TEMPLATE_VAR_DELIMITER);
	}

	public String getSparqlObservationMinimumValidModel() {
		return this.sparqlObservationMinimumValidModel;
	}

	public String getSparqlObservationTotalSsnObservation() {
		return this.sparqlObservationTotalSsnObservation;
	}

	public String getSparqlResourceMinimumValidModel() {
		return this.sparqlResourceMinimumValidModel;
	}

	public String getSparqlResourceTotalSsnSensingDevice() {
		return this.sparqlResourceTotalSsnSensingDevice;
	}

	public String getSparqlStatisticsSummary() {
		return this.sparqlStatisticsSummary;
	}

	public String getSparqlStatisticsResourcesSsnSensingDevicePerTestbed() {
		return this.sparqlStatisticsResourcesSsnSensingDevicePerTestbed;
	}

	public String getSparqlStatisticsObservationsPerTestbed() {
		return this.sparqlStatisticsObservationsPerTestbed;
	}

	public String getSparqlStatisticsObservationsPerQk() {
		return this.sparqlStatisticsObservationsPerQk;
	}
	
	public String getSparqlStatisticsObservationsPerTestbedAndQk() {
		return this.sparqlStatisticsObservationsPerTestbedAndQk;
	}

	public String getOpenamUri() {
		return getProperty(SECURITY_OPENAM_URI);
	}
	
	public String getOpenamAdminUser() {
		return getProperty(SECURITY_OPENAM_ADMIN_USERNAME);
	}
	
	public String getOpenamAdminPass() {
		return getProperty(SECURITY_OPENAM_ADMIN_PASSWORD);
	}
	
	public long getBackupPeriod() {
		return Long.parseLong(getProperty(GRAPHS_BACKUP_PERIOD));
	}

	public long getMaxRequestInterval() {
		return Long.parseLong(getProperty(MAX_REQUEST_INTERVAL));
	}
}
