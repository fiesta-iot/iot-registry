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

import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

//@WebListener
public class PersistenceHelper implements ServletContextListener {

	private static EntityManagerFactory fiestaEmf;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// fiestaEmf = Persistence.createEntityManagerFactory("FIESTAIoT");
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		PersistenceManager.FIESTAIoT.close();
	}
}