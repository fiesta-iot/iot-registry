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

import javax.persistence.Entity;
import javax.persistence.Table;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;

@Entity
public class HmacSqlTestbedIdMapper extends HmacSqlResourceIdMapper {

	private static final long serialVersionUID = 1L;

	public HmacSqlTestbedIdMapper() {
		super();
		setType(EntityType.TESTBED);
	}

	public HmacSqlTestbedIdMapper(String url) {
		super(url);
		setType(EntityType.TESTBED);
	}

}
