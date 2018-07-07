/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb;

public class SparqlExecutionException extends Exception {

	private static final long serialVersionUID = 1L;

	public SparqlExecutionException(String message) {
		super(message);
	}
}
