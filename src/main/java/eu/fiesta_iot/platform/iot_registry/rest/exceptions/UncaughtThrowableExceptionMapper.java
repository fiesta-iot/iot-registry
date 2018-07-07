/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.rest.exceptions;

import java.util.Arrays;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class UncaughtThrowableExceptionMapper
        implements ExceptionMapper<Throwable> {

	private static final Logger log =
	        LoggerFactory.getLogger(UncaughtThrowableExceptionMapper.class);

	String[] clientErrorPackages = { "org.codehaus.jackson" };

	@Override
	public Response toResponse(Throwable throwable) {
		log.error("Uncaught throwable exception", throwable);

		String pkg = Arrays
		        .stream(clientErrorPackages).filter(p -> throwable.getClass()
		                .getPackage().getName().startsWith(p))
		        .findAny().orElse(null);
		if (pkg != null) {
			return Response.status(Status.BAD_REQUEST).build();
		} else {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}