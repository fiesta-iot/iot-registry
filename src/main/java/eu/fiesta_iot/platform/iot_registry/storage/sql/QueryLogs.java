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

import java.time.OffsetDateTime;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryLogs {

	private static final Logger log = LoggerFactory.getLogger(QueryLogs.class);

	private static final String QUERY_STRING_LOG =
	        "INSERT IGNORE INTO sparql_query_log (hash, query) VALUES (:hash, :query)";

	@PersistenceContext(unitName = "FIESTAIoT",
	                    type = PersistenceContextType.EXTENDED)
	EntityManager entityManager;

	@Resource
	private UserTransaction userTransaction;

	private QueryLog add(QueryLog ql) {
		try {
			userTransaction.begin();
			entityManager.joinTransaction();
			Query query = entityManager.createNativeQuery(QUERY_STRING_LOG)
			        .setParameter("hash", ql.getQueryHash())
			        .setParameter("query", ql.getQuery());
			query.executeUpdate();
			entityManager.persist(ql);
			entityManager.refresh(ql);
			userTransaction.commit();
		} catch (IllegalStateException | SecurityException
		         | HeuristicMixedException | HeuristicRollbackException
		         | RollbackException | SystemException
		         | NotSupportedException e) {
			log.error("Unable to write (add) query log in the database", e);
		}

		return ql;
	}

	private QueryLog update(QueryLog ql) {
		try {
			userTransaction.begin();
			entityManager.joinTransaction();
			Query query = entityManager.createNamedQuery("QueryLog.update");
			query.setParameter("id", ql.getId());
			query.setParameter("exec_time", ql.getExecTime());
			query.setParameter("aborted", ql.isAborted());
			query.executeUpdate();
			userTransaction.commit();
		} catch (IllegalStateException | SecurityException
		         | HeuristicMixedException | HeuristicRollbackException
		         | RollbackException | SystemException
		         | NotSupportedException e) {
			log.error("Unable to write (update) query log in the database", e);
		}

		return ql;
	}

	public QueryLog startQueryLog(String query, String ipAddress,
	                              String username, String userAgent) {
		QueryLog ql = new QueryLog();
		ql.setIpAddress(ipAddress);
		ql.setUserName(username);
		ql.setQuery(query);
		ql.setUserAgent(userAgent);
		ql.setAborted(true);
		ql.setStartTime(OffsetDateTime.now());

		add(ql);

		ql.setStartExecTime(System.nanoTime());

		return ql;
	}

	public QueryLog endQueryLog(QueryLog ql, boolean aborted) {
		ql.setExecTime(ql.calculateExecTime(System.nanoTime()));
		ql.setAborted(aborted);

		update(ql);

		log.debug("Query execution time: " + ql.getExecTime() + "ms");

		return ql;
	}
}
