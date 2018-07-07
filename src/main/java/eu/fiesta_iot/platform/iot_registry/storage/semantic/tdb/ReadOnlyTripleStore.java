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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.jena.dboe.jenax.Txn;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.model.FiestaIoTOntModelSpec;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotRegisteredResourceException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumModelException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.StorageManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.namedgraph.GraphsManager;

public class ReadOnlyTripleStore extends TripleStore {

	private static Logger log =
	        LoggerFactory.getLogger(ReadOnlyTripleStore.class);

	public ReadOnlyTripleStore(GraphsManager graphsManager,
	        StorageManager storageManager) {
		super(graphsManager, storageManager);
	}

	public Model
	        addEntity(Model m,
	                  EnumSet<StorageManager.ValidationLevel> validate) throws NotRegisteredResourceException,
	                                                                    NotValidMinimumModelException {
		throw new UnsupportedOperationException();
	}

	public List<String> getAllEntities(boolean originalId) {

		return Txn.calculateRead(dataset, () -> {

			// @formatter:off
			String queryString = "SELECT DISTINCT ?s \n" 
			                   + "WHERE { \n"
			                   + "    ?s ?p ?o. \n"
			                   + "}";
			// @formatter:on

			ArrayList<String> entitiesList = new ArrayList<String>();
			Query query = QueryFactory.create(queryString);
			getReadModel().stream().forEach(model -> query.addGraphURI(model));
			try (QueryExecution qexec =
			        QueryExecutionFactory.create(query, dataset)) {
				ResultSet results = qexec.execSelect();
				while (results.hasNext()) {
					QuerySolution soln = results.nextSolution();
					RDFNode node = soln.get("s");
					String iri = node.toString();
					if (storageManager.isManagedEntity(iri)) {
						addToEntityIdList(entitiesList, iri, originalId);
					}
				}
				return entitiesList;
			}
		});
	}

	public List<String> getEntities(String clazz, boolean originalId) {
		return getEntitiesSparql(clazz, originalId);
	}

	// SELECT ?entity
	// WHERE {
	// ?entity rdf:type ?type.
	// ?type rdfs:subClassOf* :C.
	// }
	private List<String> getEntitiesSparql(String clazz, boolean originalId) {
		// TODO: Include in file an use String.format
		// @formatter:off
		String queryString =
		        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
		                     + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
		                     + "SELECT DISTINCT ?entity ?type \n" + "WHERE { \n"
		                     + "    ?entity rdf:type ?type. \n"
		                     + "    ?type rdfs:subClassOf* " 
		                     +
							 // "SELECT ?entity WHERE { ?entity
							 // rdf:type/rdfs:subClassOf* " +
		                     "<" + clazz + ">" 
							 + " }";
		// @formatter:on

		Query query = QueryFactory.create(queryString);
		getReadModel().stream().forEach(model -> query.addGraphURI(model));
		
		return Txn.calculateRead(dataset, () -> {
			ArrayList<String> entitiesList = new ArrayList<String>();
			try (QueryExecution qexec =
			        QueryExecutionFactory.create(query, dataset)) {
				ResultSet results = qexec.execSelect();
				while (results.hasNext()) {
					QuerySolution soln = results.nextSolution();
					RDFNode node = soln.get("entity");
					addToEntityIdList(entitiesList, node.toString(),
					                  originalId);
				}
				return entitiesList;
			}
		});
	}

	private void addToEntityIdList(ArrayList<String> resourcesList, String iri,
	                               boolean originalId) {
		if (storageManager.isManagedEntity(iri)) {
			try {
				if (originalId) {
					iri = storageManager.getOriginalEntityId(iri);
				}
				resourcesList.add(iri);
			} catch (InvalidEntityIdException e) {
			}
		}
	}

	public Model getEntity(String iri) {
		return getEntitySparql(iri);
	}

	private Model getEntitySparql(String iri) {
		return Txn.calculateRead(dataset, () -> {
			// Using DESCRIBE SPARQL
			String queryString = "DESCRIBE <" + iri + ">";
			Query query = QueryFactory.create(queryString);
			try (QueryExecution qExec =
			        QueryExecutionFactory.create(query, dataset)) {
				Model resultModel = qExec.execDescribe();
				// resultModel.write(out, "JSONLD");
				return resultModel;
			}
		});
	}

	/**
	 * This function creates a model out of a resource.
	 *
	 * @param resource
	 * @return default model with resource
	 */
	protected static Model createModelFromResource(Resource resource) {
		StmtIterator stmts = resource.listProperties();
		if (!stmts.hasNext()) {
			// Empty means no resource was found
			return null;
		}

		// Create model from resource
		Model m = ModelFactory.createDefaultModel();
		while (stmts.hasNext()) {
			Statement stmt = stmts.next();
			m.add(stmt);
		}

		return m;
	}

	public Model deleteEntity(String iri) {
		throw new UnsupportedOperationException();
	}
}
