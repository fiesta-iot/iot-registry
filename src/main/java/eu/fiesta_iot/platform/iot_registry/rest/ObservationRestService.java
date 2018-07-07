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

import java.util.Random;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Path;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.ObservationListSerializerHelper;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;
import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQuery.Scope;

@Path("/observations")
@RequestScoped
public class ObservationRestService
        extends SemanticRegistryRestService<ObservationListSerializerHelper> {

	private static int VALIDATE_REGISTRATION_PERCENTAGE =
	        IoTRegistryConfiguration.getInstance()
	                .getValidateObservationRegistrationPercentage();

	public ObservationRestService() {
		super(TripleStoreFactory.createObservationsTripleStore(),
		      new ObservationListSerializerHelper(), EntityType.OBSERVATION,
		      Scope.OBSERVATIONS);
	}

	@Override
	protected boolean validateRegistration() {
		Random rand = new Random();
		return rand.nextInt(100) < VALIDATE_REGISTRATION_PERCENTAGE ? true
		        : false;
	}

}
