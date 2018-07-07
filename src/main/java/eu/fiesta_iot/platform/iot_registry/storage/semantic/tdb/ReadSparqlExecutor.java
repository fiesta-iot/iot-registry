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

import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.model.FiestaIoTOntModel;

public class ReadSparqlExecutor implements AutoCloseable {

	private static final Logger log =
	        LoggerFactory.getLogger(ReadSparqlExecutor.class);

	private Query query;
	private Dataset dataset;
	private TripleStore ts;

	protected ReadSparqlExecutor(Query query, TripleStore ts) {
		this.query = query;
		this.ts = ts;
		this.dataset = ts.getDataset();
	}

	protected ReadSparqlExecutor(String query, TripleStore ts) {
		this(QueryFactory.create(query), ts);
	}

	public static ReadSparqlExecutor create(Query query, TripleStore ts) {
		return new ReadSparqlExecutor(query, ts);
	}

	public static ReadSparqlExecutor create(String query, TripleStore ts) {
		return new ReadSparqlExecutor(query, ts);
	}

	public ReadSparqlResult execute() throws SparqlExecutionException {
		return execute(true);
	}

	public ReadSparqlResult
	        execute(boolean inference) throws SparqlExecutionException {

		ReadSparqlResult result = Txn.calculateRead(dataset, () -> {
 
			List<String> models = ts.getReadModel();
			if (inference) {
				// TODO: Inferencia cargar el modelo de FIESTA-IoT
				//models.add("NuevoModelo");
			}

			Query q = query.cloneQuery();
			models.stream().forEach(model -> q.addGraphURI(model));
			
			log.debug("SPARQL Query:\n" + q.toString());

			try (QueryExecution qExec =
			        QueryExecutionFactory.create(q, dataset)) {
				if (query.isSelectType()) {
					return new ReadSparqlResult(ResultSetFactory
					        .copyResults(qExec.execSelect()), this);
				} else if (query.isDescribeType()) {
					return new ReadSparqlResult(qExec.execDescribe(), this);
				} else if (query.isAskType()) {
					return new ReadSparqlResult(qExec.execAsk(), this);
				} else if (query.isConstructType()) {
					return new ReadSparqlResult(qExec.execConstruct(), this);
				} else {
					return null;
					// throw new SparqlExecutionException("Unsupported query
					// type: "
					// + query.getQueryType());
				}
			}
		});

		// TODO: Change SparqlExecutionException to RuntimeException
		if (result == null) {
			throw new SparqlExecutionException("Unsupported query type: "
			                                   + query.getQueryType());
		}
		
		return result;
	}

	public Query getQuery() {
		return query;
	}

	@Override
	public void close() {
	}
}
