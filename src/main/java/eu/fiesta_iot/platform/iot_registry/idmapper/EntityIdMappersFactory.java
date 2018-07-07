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

import eu.fiesta_iot.platform.iot_registry.idmapper.cipher.AesEndpointIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.cipher.AesObservationIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.cipher.AesResourceIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.cipher.AesTestbedIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.hashid.HashidEndpointIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.hashid.HashidObservationIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.hashid.HashidResourceIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.hashid.HashidTestbedIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.sql.HmacSqlEndpointIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.sql.HmacSqlObservationIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.sql.HmacSqlResourceIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.sql.HmacSqlTestbedIdMappers;

public abstract class EntityIdMappersFactory {

	public static final EntityIdMappers<?>
	        createEndpointIdMappers(EntityIdMapperType type) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlEndpointIdMappers();
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidEndpointIdMappers();
		} else if (type == EntityIdMapperType.AesCipher) {
			return new AesEndpointIdMappers();
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMappers<?>
	        createResourceIdMappers(EntityIdMapperType type) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlResourceIdMappers();
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidResourceIdMappers();
		} else if (type == EntityIdMapperType.AesCipher) {
			return new AesResourceIdMappers();
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMappers<?>
	        createObservationIdMappers(EntityIdMapperType type) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlObservationIdMappers();
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidObservationIdMappers();
		} else if (type == EntityIdMapperType.AesCipher) {
			return new AesObservationIdMappers();
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

	public static final EntityIdMappers<?>
	        createTestbedIdMappers(EntityIdMapperType type) {
		if (type == EntityIdMapperType.HmacSql) {
			return new HmacSqlTestbedIdMappers();
		} else if (type == EntityIdMapperType.Hashid) {
			return new HashidTestbedIdMappers();
		} else if (type == EntityIdMapperType.AesCipher) {
			return new AesTestbedIdMappers();
		}

		throw new IllegalArgumentException("EntityIdMapperType not supported");
	}

}
