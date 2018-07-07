/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions;

public abstract class NotValidMinimumModelException extends Exception {
	public NotValidMinimumModelException() {
		super("The description does not include the required minimum information");
	}

	public NotValidMinimumModelException(String message) {
		super(message);
	}
	
	private static final long serialVersionUID = 1L;

}
