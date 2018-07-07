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

import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;

public class SemanticStorageLogs {

	private static final Logger log =
	        LoggerFactory.getLogger(SemanticStorageLogs.class);

	@PersistenceContext(unitName = "FIESTAIoT",
	                    type = PersistenceContextType.EXTENDED)
	EntityManager entityManager;

	@Resource
	private UserTransaction userTransaction;

	private SemanticStorageLog add(SemanticStorageLog ol) {
		try {
			userTransaction.begin();
			entityManager.joinTransaction();
			entityManager.persist(ol);
			entityManager.refresh(ol);
			userTransaction.commit();
		} catch (IllegalStateException | SecurityException
		         | HeuristicMixedException | HeuristicRollbackException
		         | RollbackException | SystemException
		         | NotSupportedException e) {
			log.error("Unable to write (add) observation storage log in the database");
		}

		return ol;
	}

	private SemanticStorageLog update(SemanticStorageLog ol) {
		try {
			userTransaction.begin();
			entityManager.joinTransaction();
			Query query =
			        entityManager.createNamedQuery("SemanticStorageLog.update");
			query.setParameter("id", ol.getId());
			query.setParameter("exec_time", ol.getExecTime());
			query.setParameter("aborted", ol.isAborted());
			query.executeUpdate();
			userTransaction.commit();
		} catch (IllegalStateException | SecurityException
		         | HeuristicMixedException | HeuristicRollbackException
		         | RollbackException | SystemException
		         | NotSupportedException e) {
			log.error("Unable to write (update) observation storage log in the database");
		}

		return ol;
	}

	public SemanticStorageLog startLog(EntityType entityType, String ipAddress,
	                                   String userToken, String userAgent) {
		SemanticStorageLog ol = new SemanticStorageLog();
		ol.setEntityType(entityType);
		ol.setIpAddress(ipAddress);
		ol.setUserName(userToken);
		ol.setUserAgent(userAgent);
		ol.setAborted(true); // By default it is aborted unless we write
		ol.setStartTime(OffsetDateTime.now());

		add(ol);

		ol.setStartExecTime(System.nanoTime());

		return ol;
	}

	public SemanticStorageLog endLog(SemanticStorageLog ol, boolean aborted) {
		ol.setExecTime(ol.calculateExecTime(System.nanoTime()));
		ol.setAborted(aborted);

		update(ol);

		log.debug("Observation storage time: " + ol.getExecTime() + "ms");

		return ol;
	}
}
