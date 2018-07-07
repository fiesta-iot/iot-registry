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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RestWebApplicationExceptionMapper
        implements ExceptionMapper<WebApplicationException> {

	private static final Logger log =
	        LoggerFactory.getLogger(RestWebApplicationExceptionMapper.class);

	@Override
	public Response toResponse(WebApplicationException ex) {
		StatusType status = ex.getResponse().getStatusInfo();
		log.info("WebApplicationException thrown [{} {}]",
		          status.getStatusCode(), status.getReasonPhrase());
		return ex.getResponse();
	}
}
