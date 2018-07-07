/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.sql;

import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.transaction.UserTransaction;

import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQuery.Scope;

public class SparqlQueries {

	@PersistenceContext(unitName = "FIESTAIoT",
	                    type = PersistenceContextType.EXTENDED)
	EntityManager entityManager;

	@Resource
	private UserTransaction userTransaction;

	public SparqlQuery get(long id) {
		SparqlQuery query = entityManager.find(SparqlQuery.class, id);

		return query;
	}

	public List<Long> getAllIds() {
		return entityManager.createNamedQuery("Query.findAllIds", Long.class)
		        .getResultList();
	}

	public List<Long> getAllIds(Scope scope) {
		return entityManager
		        .createNamedQuery("Query.findIdsByScope", Long.class)
		        .setParameter("scope", scope).getResultList();
	}

	// query object is updated with what it is in the database
	public SparqlQuery add(SparqlQuery query) throws Exception {
		userTransaction.begin();
		entityManager.persist(query);

		entityManager.refresh(query);
		userTransaction.commit();

		return query;
	}

	// query object is updated with what it is in the database
	public SparqlQuery update(long id, SparqlQuery query) throws Exception {
		SparqlQuery queryDb = entityManager.find(SparqlQuery.class, id);
		if (queryDb == null) {
			return null;
		}

		userTransaction.begin();
		// Just update in case the value is not NULL
		if (query.getName() != null) {
			queryDb.setName(query.getName());
		}
		if (query.getValue() != null) {
			queryDb.setValue(query.getValue());
		}
		if (query.getDescription() != null) {
			queryDb.setDescription(query.getDescription());
		}
		if (query.getScope() != null) {
			queryDb.setScope(query.getScope());
		}
		// Need to modify the value in order to get the database engine
		// to automatically update the value to CURRENT_TIMESTAMP
		queryDb.setModified(null);
		// entityManager.getTransaction().commit();
		entityManager.refresh(queryDb);
		userTransaction.commit();

		return queryDb;
	}

	public SparqlQuery delete(long id) throws Exception {

		SparqlQuery query = entityManager.find(SparqlQuery.class, id);
		if (query == null) {
			return null;
		}
		userTransaction.begin();
		entityManager.remove(query);
		userTransaction.commit();

		return query;
	}
}
