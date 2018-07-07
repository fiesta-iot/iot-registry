/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb;

import java.io.Closeable;

import org.apache.jena.query.Query;

public class ReadSparqlResult implements AutoCloseable, Closeable {
	private Object result;
	ReadSparqlExecutor exec;

	public ReadSparqlResult(Object result, ReadSparqlExecutor exec) {
		this.result = result;
		this.exec = exec;
	}
	
	public Object getResult() {
		return result;
	}

	public Query getQuery() {
		return exec.getQuery();
	}

	@Override
	public void close() {
		exec.close();
	}

}
