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

import java.util.Set;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;

public abstract class AesEntityIdMappers<T extends AesEntityIdMapper>
        extends EntityIdMappers<T> {

	public AesEntityIdMappers(Class<T> entityClass) {
		super(entityClass);
	}

	@Override
	public T add(T ep) {
		return ep;
	}

	@Override
	public T update(T ep) {
		return ep;
	}

	@Override
	public void sync(EntityIdMapper ep) {
		return;
	}

	@Override
	public void sync(Set<EntityIdMapper> eps) {
		return;
	}

	@Override
	public T delete(String hash) throws InvalidEntityIdException {
		return get(hash);
	}
}
