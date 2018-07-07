/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.idmapper.cipher;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.NoSuchPaddingException;

import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;

public class AesEndpointIdMappers
        extends AesEntityIdMappers<AesEndpointIdMapper> {

	public AesEndpointIdMappers() {
		super(AesEndpointIdMapper.class);
	}

	@Override
	public AesEndpointIdMapper
	        get(String hash) throws InvalidEntityIdException {
		AesEndpointIdMapper mapper;
		try {
			mapper = new AesEndpointIdMapper();
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
		         | InvalidKeySpecException e) {
			// TODO: Think on a way to launch an exception
			e.printStackTrace();
			return null;
		}
		mapper.setHash(hash);

		return mapper;
	}
}
