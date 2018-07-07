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

import java.util.List;

public class NotValidMinimumObservationModelException
        extends NotValidMinimumModelException {

	public NotValidMinimumObservationModelException() {
		super();
	}
	
	public NotValidMinimumObservationModelException(List<String> observations) {
		super("The description of observations " + observations.toString()
		      + " does "
		      + "not include the required minimum information. If they are "
		      + "blank node, please note that the identifier may differ from "
		      + "the original.");
	}

	private static final long serialVersionUID = 1L;

}
