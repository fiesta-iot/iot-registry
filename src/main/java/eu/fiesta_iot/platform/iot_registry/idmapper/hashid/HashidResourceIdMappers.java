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

public class HashidResourceIdMappers
        extends HashidEntityIdMappers<HashidResourceIdMapper> {

	public HashidResourceIdMappers() {
		super(HashidResourceIdMapper.class);
	}

	@Override
	public HashidResourceIdMapper get(String hash) {
		HashidResourceIdMapper mapper = new HashidResourceIdMapper();
		mapper.setHash(hash);

		return mapper;
	}
}
