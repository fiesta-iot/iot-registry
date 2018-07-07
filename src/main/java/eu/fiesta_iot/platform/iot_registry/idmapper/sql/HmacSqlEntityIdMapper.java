/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.idmapper.sql;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;

public class HmacSqlEntityIdMapper extends SqlEntityIdMapper {

	private static final long serialVersionUID = 1L;

	public HmacSqlEntityIdMapper() {
		super();
	}

	public HmacSqlEntityIdMapper(EntityType type, String url, String secret) {
		super(type, iriHash(url, secret), url);
	}

	protected static final String iriHash(String iri, String secret) {
		try {
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key =
			        new SecretKeySpec(secret.getBytes(), "HmacSHA256");
			sha256_HMAC.init(secret_key);
			// Use URL encoder to avoid problems when referencing it
			return Base64.getUrlEncoder()
			        .encodeToString(sha256_HMAC.doFinal(iri.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}

		// Should never get here
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!(o instanceof HmacSqlEntityIdMapper)) {
			return false;
		}

		HmacSqlEntityIdMapper other = (HmacSqlEntityIdMapper) o;
		if (this.getHash() != other.getHash()) {
			return false;
		}

		return true;
	}
}
