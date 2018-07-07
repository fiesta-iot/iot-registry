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

public class AesObservationIdMappers
        extends AesEntityIdMappers<AesObservationIdMapper> {

	public AesObservationIdMappers() {
		super(AesObservationIdMapper.class);
	}

	@Override
	public AesObservationIdMapper
	        get(String hash) throws InvalidEntityIdException {
		AesObservationIdMapper mapper;
		try {
			mapper = new AesObservationIdMapper();
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
		         | InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		mapper.setHash(hash);

		return mapper;
	}
}
