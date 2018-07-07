/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;

// @WebListener
public class ServletContextRetrieverListener implements ServletContextListener {

	private static Logger log =
	        LoggerFactory.getLogger(ServletContextRetrieverListener.class);
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		IoTRegistryConfiguration.getInstance()
		        .setServletContext(event.getServletContext());
		
		TripleStoreFactory.initializeDataset(IoTRegistryConfiguration.getInstance().getTripleStorePath());
		
//		StoredFiestaIoTOntModel.getStoredInstance();
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		TripleStoreFactory.releaseDataset();
	}
}