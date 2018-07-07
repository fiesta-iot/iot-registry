/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.idmapper;

/**
 * Not valid hash or internal id is being used.
 * The id cannot be found in the database or the format is not correct.
 * 
 */
public class InvalidEntityIdException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidEntityIdException() {
		super("Not valid hash format");
	}

	public InvalidEntityIdException(String string) {
		super(string);
	}

	public InvalidEntityIdException(String string, Throwable e) {
		super(string, e);
	}
}
