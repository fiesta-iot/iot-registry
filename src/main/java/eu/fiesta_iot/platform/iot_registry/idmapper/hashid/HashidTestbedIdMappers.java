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

public class HashidTestbedIdMappers
        extends HashidEntityIdMappers<HashidTestbedIdMapper> {

	public HashidTestbedIdMappers() {
		super(HashidTestbedIdMapper.class);
	}

	@Override
	public HashidTestbedIdMapper get(String hash) {
		HashidTestbedIdMapper mapper = new HashidTestbedIdMapper();
		mapper.setHash(hash);

		return mapper;
	}
}
