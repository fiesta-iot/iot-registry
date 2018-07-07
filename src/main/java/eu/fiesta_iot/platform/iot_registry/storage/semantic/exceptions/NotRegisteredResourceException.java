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

import java.util.Collection;

public class NotRegisteredResourceException extends Exception {
	public NotRegisteredResourceException() {
		super("The description does not include the required minimum information");
	}

	public NotRegisteredResourceException(Collection<String> resources) {
		super("The resources " + resources.toString() + " have not being "
		      + "previously registered and therefore cannot be referenced.");
	}

	private static final long serialVersionUID = 1L;

}
