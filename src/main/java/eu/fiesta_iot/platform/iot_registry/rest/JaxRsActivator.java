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

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.rest.exceptions.BadRequestExceptionHandler;
import eu.fiesta_iot.platform.iot_registry.rest.exceptions.NotFoundExceptionHandler;
import eu.fiesta_iot.platform.iot_registry.rest.exceptions.RestWebApplicationExceptionMapper;
import eu.fiesta_iot.platform.iot_registry.rest.exceptions.UncaughtThrowableExceptionMapper;
import eu.fiesta_iot.platform.iot_registry.rest.serializers.GeneratedEnumParamConverterProvider;

public class JaxRsActivator extends Application {
	private static final Logger log =
	        LoggerFactory.getLogger(JaxRsActivator.class);
	
	Set<Object> singletons = new HashSet<Object>();
	Set<Class<?>> classes = new HashSet<Class<?>>();

	public JaxRsActivator() {
		log.info("***** Activating IoT Registry *****");
		// singletons.add(new ResourceRestService());
		classes.add(ResourceRestService.class);
		classes.add(EndpointRestService.class);
		classes.add(ObservationRestService.class);
		classes.add(QueryRestService.class);
		classes.add(TestbedRestService.class);
		
		classes.add(StatisticsRestService.class);

		classes.add(IdentifierRestService.class);
		classes.add(GraphsRestService.class);
		
		classes.add(NotFoundExceptionHandler.class);
		classes.add(BadRequestExceptionHandler.class);
		
		singletons.add(new RestWebApplicationExceptionMapper());
		singletons.add(new UncaughtThrowableExceptionMapper());
		
		singletons.add(new GeneratedEnumParamConverterProvider());
		singletons.add(new ObjectMapperContextResolver());
	}

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
