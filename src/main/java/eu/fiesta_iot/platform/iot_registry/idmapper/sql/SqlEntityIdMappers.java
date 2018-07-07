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

import java.util.Set;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.storage.sql.PersistenceManager;

public class SqlEntityIdMappers<T extends SqlEntityIdMapper>
        extends EntityIdMappers<T> {

	private Logger log = LoggerFactory.getLogger(getClass());

	public SqlEntityIdMappers(Class<T> entityClass) {
		super(entityClass);
	}

	@Override
	public T get(String hash) throws InvalidEntityIdException {
		EntityManager entityManager =
		        PersistenceManager.FIESTAIoT.getEntityManager();
		T ep = entityManager.find(getEntityClass(), hash);
		entityManager.close();

		if (ep == null) {
			throw new InvalidEntityIdException("Not valid hash format");
		}
		return ep;
	}

	// query object is updated with what it is in the database
	@Override
	public T add(T ep) {
		EntityManager entityManager =
		        PersistenceManager.FIESTAIoT.getEntityManager();
		entityManager.getTransaction().begin();
		entityManager.persist(ep);
		entityManager.getTransaction().commit();
		entityManager.refresh(ep);
		entityManager.close();

		return ep;
	}

	// query object is updated with what it is in the database
	@Override
	public T update(T ep) {
		EntityManager entityManager =
		        PersistenceManager.FIESTAIoT.getEntityManager();

		T epDb = entityManager.find(getEntityClass(), ep.getHash());
		if (epDb == null) {
			entityManager.close();
			return null;
		}

		entityManager.getTransaction().begin();
		// Just update in case the value is not NULL
		if (ep.getUrl() != null) {
			epDb.setUrl(ep.getUrl());
		}
		entityManager.getTransaction().commit();
		entityManager.refresh(epDb);
		entityManager.close();

		return epDb;
	}

	// Don't touch if already exists in database
	protected void sync(EntityIdMapper ep, EntityManager entityManager) {

		log.debug("Class" + getEntityClass().toString());

		T endpointMapper = entityManager.find(getEntityClass(), ep.getHash());
		if (endpointMapper != null) {
			return;
		}
		entityManager.getTransaction().begin();
		entityManager.persist(ep);
		entityManager.getTransaction().commit();
	}

	@Override
	public void sync(EntityIdMapper ep) {
		EntityManager entityManager =
		        PersistenceManager.FIESTAIoT.getEntityManager();
		sync(ep, entityManager);
		entityManager.close();
	}

	@Override
	public void sync(Set<EntityIdMapper> eps) {
		EntityManager entityManager =
		        PersistenceManager.FIESTAIoT.getEntityManager();

		for (EntityIdMapper entry : eps) {
			sync(entry, entityManager);
		}

		entityManager.close();
	}

	@Override
	public T delete(String hash) throws InvalidEntityIdException {
		EntityManager entityManager =
		        PersistenceManager.FIESTAIoT.getEntityManager();

		T ep = entityManager.find(getEntityClass(), hash);
		if (ep == null) {
			entityManager.close();
			throw new InvalidEntityIdException();
		}
		entityManager.getTransaction().begin();
		entityManager.remove(ep);
		entityManager.getTransaction().commit();
		entityManager.close();

		return ep;
	}
}
