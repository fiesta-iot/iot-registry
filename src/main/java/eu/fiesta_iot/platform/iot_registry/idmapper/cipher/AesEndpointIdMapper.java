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

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;

public class AesEndpointIdMapper extends AesEntityIdMapper {

	private static final byte[] key;

	static {
		final String password = IoTRegistryConfiguration.getInstance()
		        .getEndpointMapperPassword();

		key = generateKeyFromPassword(password);
	}

	public AesEndpointIdMapper() throws NoSuchAlgorithmException,
	                             NoSuchPaddingException,
	                             InvalidKeySpecException {
		super(EntityType.ENDPOINT_URL, key);
	}

	public AesEndpointIdMapper(String url) throws NoSuchAlgorithmException,
	                                       NoSuchPaddingException,
	                                       InvalidKeySpecException {
		super(EntityType.ENDPOINT_URL, url, key);
	}
}
