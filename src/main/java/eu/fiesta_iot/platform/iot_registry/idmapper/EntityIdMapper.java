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

public abstract class EntityIdMapper {

	EntityType type;
	
	public abstract String getHash();

	public abstract void setHash(String hash) throws InvalidEntityIdException;

	public abstract String getUrl();

	public abstract void setUrl(String url);
	
	public EntityType getType() {
		return type;
	}
	
	protected void setType(EntityType type) {
		this.type = type;
	}
}
