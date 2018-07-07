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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.NoSuchPaddingException;

import eu.fiesta_iot.platform.iot_registry.idmapper.cipher.AesEndpointIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.cipher.AesObservationIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.cipher.AesResourceIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.cipher.AesTestbedIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.hashid.HashidEndpointIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.hashid.HashidObservationIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.hashid.HashidResourceIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.hashid.HashidTestbedIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.sql.HmacSqlEndpointIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.sql.HmacSqlObservationIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.sql.HmacSqlResourceIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.sql.HmacSqlTestbedIdMapper;

public abstract class EntityIdMapperFactory {

	public static final EntityIdMapper
	        createEndpointIdMapper(EntityIdMapperType type, String url) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlEndpointIdMapper(url);
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidEndpointIdMapper(url);
		} else if (type == EntityIdMapperType.AesCipher) {
			try {
				return new AesEndpointIdMapper(url);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
			         | InvalidKeySpecException e) {
				throw new IllegalArgumentException("EntityIdMapperType not supported",
				                                   e);
			}
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMapper
	        createEndpointIdMapper(EntityIdMapperType type) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlEndpointIdMapper();
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidEndpointIdMapper();
		} else if (type == EntityIdMapperType.AesCipher) {
			try {
				return new AesEndpointIdMapper();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
			         | InvalidKeySpecException e) {
				throw new IllegalArgumentException("EntityIdMapperType not supported",
				                                   e);
			}
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMapper
	        createResourceIdMapper(EntityIdMapperType type, String url) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlResourceIdMapper(url);
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidResourceIdMapper(url);
		} else if (type == EntityIdMapperType.AesCipher) {
			try {
				return new AesResourceIdMapper(url);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
			         | InvalidKeySpecException e) {
				throw new IllegalArgumentException("EntityIdMapperType not supported",
				                                   e);
			}
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMapper
	        createResourceIdMapper(EntityIdMapperType type) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlResourceIdMapper();
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidResourceIdMapper();
		} else if (type == EntityIdMapperType.AesCipher) {
			try {
				return new AesResourceIdMapper();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
			         | InvalidKeySpecException e) {
				throw new IllegalArgumentException("EntityIdMapperType not supported",
				                                   e);
			}
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMapper
	        createObservationIdMapper(EntityIdMapperType type, String url) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlObservationIdMapper(url);
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidObservationIdMapper(url);
		} else if (type == EntityIdMapperType.AesCipher) {
			try {
				return new AesObservationIdMapper(url);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
			         | InvalidKeySpecException e) {
				throw new IllegalArgumentException("EntityIdMapperType not supported",
				                                   e);
			}
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMapper
	        createObservationIdMapper(EntityIdMapperType type) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlObservationIdMapper();
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidObservationIdMapper();
		} else if (type == EntityIdMapperType.AesCipher) {
			try {
				return new AesObservationIdMapper();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
			         | InvalidKeySpecException e) {
				throw new IllegalArgumentException("EntityIdMapperType not supported",
				                                   e);
			}
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMapper
	        createTestbedIdMapper(EntityIdMapperType type, String url) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlTestbedIdMapper(url);
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidTestbedIdMapper(url);
		} else if (type == EntityIdMapperType.AesCipher) {
			try {
				return new AesTestbedIdMapper(url);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
			         | InvalidKeySpecException e) {
				throw new IllegalArgumentException("EntityIdMapperType not supported",
				                                   e);
			}
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMapper
	        createTestbedIdMapper(EntityIdMapperType type) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlTestbedIdMapper();
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidTestbedIdMapper();
		} else if (type == EntityIdMapperType.AesCipher) {
			try {
				return new AesTestbedIdMapper();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException
			         | InvalidKeySpecException e) {
				throw new IllegalArgumentException("EntityIdMapperType not supported",
				                                   e);
			}
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

}
