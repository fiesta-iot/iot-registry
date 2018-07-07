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

public enum EntityType {
	RESOURCE,
	OBSERVATION,
	ENDPOINT_URL,
	TESTBED;
	
	public static EntityType fromString(String param) {
		String toUpper = param.toUpperCase();
		return valueOf(toUpper);
	}
};