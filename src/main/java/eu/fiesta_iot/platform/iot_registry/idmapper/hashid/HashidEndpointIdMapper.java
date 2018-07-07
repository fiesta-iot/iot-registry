/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.idmapper.hashid;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;

public class HashidEndpointIdMapper extends HashidEntityIdMapper {

	private static final String SECRET =
	        IoTRegistryConfiguration.getInstance().getEndpointMapperPassword();

	public HashidEndpointIdMapper() {
		super(EntityType.ENDPOINT_URL, SECRET);
	}

	public HashidEndpointIdMapper(String url) {
		super(EntityType.ENDPOINT_URL, url, SECRET);
	}
}
