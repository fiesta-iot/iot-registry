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

import java.util.Set;

public abstract class EntityIdMappers<T extends EntityIdMapper> {

	private Class<T> entityClass;

	// TODO: Have to check for this to work
	// public EndpointMappers() {
	// ParameterizedType genericSuperclass = (ParameterizedType)
	// getClass().getGenericSuperclass();
	// persistentClass = (Class<T>)
	// genericSuperclass.getActualTypeArguments()[0];
	// }

	public EntityIdMappers(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	protected Class<T> getEntityClass() {
		return entityClass;
	}

	public abstract T get(String hash) throws InvalidEntityIdException;

	// query object is updated with what it is in the database
	public abstract T add(T ep);

	// query object is updated with what it is in the database
	public abstract T update(T ep);

	public abstract void sync(EntityIdMapper ep);

	public abstract void sync(Set<EntityIdMapper> eps);

	public abstract T delete(String hash) throws InvalidEntityIdException;
}
