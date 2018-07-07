/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.semantic.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapper;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapperFactory;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMapperType;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMappers;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityIdMappersFactory;
import eu.fiesta_iot.platform.iot_registry.idmapper.EntityType;
import eu.fiesta_iot.platform.iot_registry.idmapper.InvalidEntityIdException;
import eu.fiesta_iot.platform.iot_registry.model.FiestaIoTOntModel;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.FiestaIoTIri;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotRegisteredResourceException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumModelException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumObservationModelException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStore;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;
import eu.fiesta_iot.utils.semantics.vocabulary.IotLite;
import eu.fiesta_iot.utils.semantics.vocabulary.Ssn;

public class ObservationStorageManager extends StorageManager {

	private static Logger log =
	        LoggerFactory.getLogger(ObservationStorageManager.class);

	public ObservationStorageManager(EntityIdMapperType mapperType) {
		super(mapperType);
	}

	// Need to check what happens with blank nodes as probably
	// they are directly managed. The problem is we want those
	// nodes to be also accesible
	@Override
	public Model adaptEntity(Model input) {
		Model newObservation = ModelFactory.createDefaultModel();

		// Observation to be used for ontology inference.
		// This way it is possible to know the inheritance,
		// if a class or individual, etc.
		OntModel ontObservation = FiestaIoTOntModel.getInstance();
		ontObservation.add(input);

		// Model modelQuantityKind =
		// materializeInferenceForQuantityKind(ontObservation);
		// input.add(modelQuantityKind);

		// Do not use listIndividuals as it is more probable that
		// only list the Subjects and not the ones in the Object
		StmtIterator stmts = input.listStatements();
		while (stmts.hasNext()) {
			Statement stmt = stmts.next();

			// TODO: Depending on speed it might be advisable to keep a list
			// of individuals, so once discovered it is not required to
			// semantically ask again for them

			// Every subject has to be referenceable
			// We don't mind whether it is blank or named node
			if (checkSubjectIsClass(stmt, ontObservation)) {
				newObservation.add(stmt);
				continue;
			}
			
			Statement newStmt;
			if (checkSubjectIsResource(stmt, ontObservation)) {
				newStmt = adaptAsResource(stmt, ontObservation);
			} else {
				newStmt = adaptAsObservation(stmt, ontObservation);	
			}
			
			newObservation.add(newStmt);
		}

		// ontObservation.close();

		return newObservation;
	}

	private boolean checkSubjectIsClass(final Statement stmt, final OntModel ontModel) {
		Resource s = stmt.getSubject();

		if (s.isURIResource()) {
			String subjectForStmt = s.toString();
			Individual individual = ontModel.getIndividual(subjectForStmt);
			if (individual.isClass()) {
				// If is a class definition skip to next
				// This code shouldn't be called if everything is as
				// expected
				return true;
			}
		}
		
		return false;
	}
		
	private boolean checkSubjectIsResource(final Statement stmt, final OntModel ontModel) {
		String propertyForStmt = stmt.getPredicate().toString();
		
		if (propertyForStmt.equalsIgnoreCase(Ssn.madeObservation.toString())) {
			return true;
		}
		
		return false;
	}
	
	private Statement adaptAsResource(final Statement stmt, final OntModel ontModel) {
		Resource s = stmt.getSubject();
		String subjectForStmt = s.toString();
		Property p = stmt.getPredicate();
		String propertyForStmt = p.toString();
		RDFNode o = stmt.getObject();
		String objectForStmt = o.toString();

		EntityIdMapper rm = EntityIdMapperFactory
		        .createResourceIdMapper(getEntityIdMapperType(),
		                                subjectForStmt);
		Resource r = ResourceFactory.createResource(FiestaIoTIri.create(rm).asIri());
		
		Statement newStmt = ResourceFactory.createStatement(r, p, o);
		
		if (propertyForStmt.equalsIgnoreCase(Ssn.madeObservation.toString())) {
			EntityIdMapper om = EntityIdMapperFactory
			        .createObservationIdMapper(getEntityIdMapperType(),
			                                objectForStmt);
			r = ResourceFactory.createResource(FiestaIoTIri.create(om).asIri());
			newStmt = newStmt.changeObject(r);
		}
		
		return newStmt;
	}

	private Statement adaptAsObservation(final Statement stmt, final OntModel ontModel) {
		Resource s = stmt.getSubject();
		String subjectForStmt = s.toString();
		Property p = stmt.getPredicate();
		String propertyForStmt = p.toString();
		RDFNode o = stmt.getObject();
		String objectForStmt = o.toString();

		Individual individual;

		EntityIdMapper rm = EntityIdMapperFactory
		        .createObservationIdMapper(getEntityIdMapperType(),
		                                   subjectForStmt);
		Resource r =
		        ResourceFactory.createResource(FiestaIoTIri.create(rm).asIri());
		Statement newStmt = ResourceFactory.createStatement(r, p, o);

		// Property analysis
		if (propertyForStmt.equalsIgnoreCase(Ssn.observedBy.toString())) {
			EntityIdMapper om = EntityIdMapperFactory
			        .createResourceIdMapper(getEntityIdMapperType(),
			                                objectForStmt);
			r = ResourceFactory.createResource(FiestaIoTIri.create(om).asIri());
			newStmt = newStmt.changeObject(r);
		} else if (propertyForStmt.equalsIgnoreCase(IotLite.hasUnit.toString())
		           || propertyForStmt
		                   .equalsIgnoreCase(Ssn.observedProperty.toString())) {
			EntityIdMapper om = EntityIdMapperFactory
			        .createObservationIdMapper(getEntityIdMapperType(),
			                                   objectForStmt);
			r = ResourceFactory.createResource(FiestaIoTIri.create(om).asIri());
			newStmt = newStmt.changeObject(r);
		} else {

			if (o.isURIResource()) {
				individual = ontModel.getIndividual(objectForStmt);
				// Check if there is enough information to determine if an
				// URI Resource is an individual or a class
				// If null then no information can be extracted from the
				// statement and therefore we consider this Object as an
				// entity and not a class, which should be define in
				// the included ontologies
				if (individual == null || !individual.isClass()) {
					EntityIdMapper om = EntityIdMapperFactory
					        .createObservationIdMapper(getEntityIdMapperType(),
					                                   objectForStmt);
					r = ResourceFactory
					        .createResource(FiestaIoTIri.create(om).asIri());
					newStmt = newStmt.changeObject(r);
				}
			} else if (o.isAnon()) {
				EntityIdMapper om = EntityIdMapperFactory
				        .createObservationIdMapper(getEntityIdMapperType(),
				                                   objectForStmt);
				r = ResourceFactory
				        .createResource(FiestaIoTIri.create(om).asIri());
				newStmt = newStmt.changeObject(r);
			}
		}

		return newStmt;
	}

	private Model materializeInferenceForQuantityKind(Model resources) {
		// Con el rdfs:subClassOf+ me quito que salga tambien la clase actual.
		// Con * tambien saldría la actual.
		// Realmente pondría sensing device ya que los devices no creo que los
		// busquen por System
		String queryString =
		        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
		                     + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
		                     + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
		                     + "PREFIX qu: <http://purl.org/NET/ssnx/qu/qu#>\n"
		                     + "CONSTRUCT { ?qk rdf:type ?superClass .} "
		                     + "WHERE { " + "?qk rdf:type ?type  ."
		                     + "?type rdfs:subClassOf* qu:QuantityKind . "
		                     + "?type rdfs:subClassOf+ ?superClass . "
		                     + "FILTER (!isBlank(?superClass) && ?superClass != owl:Thing && ?superClass != rdfs:Resource)}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec =
		        QueryExecutionFactory.create(query, resources)) {
			return qexec.execConstruct();
		}
	}

	@Override
	public String
	        getOriginalEntityId(String iri) throws InvalidEntityIdException {
		FiestaIoTIri fiesta = FiestaIoTIri.create(iri);
		if (fiesta.isObservation()) {
			return getOriginalEntityId(fiesta.getHash(),
			                           fiesta.getEntityType());
		} else {
			return null;
		}
	}

	@Override
	public String
	        getOriginalEntityId(String hash,
	                            EntityType type) throws InvalidEntityIdException {
		FiestaIoTIri iri = FiestaIoTIri.create(hash, EntityType.OBSERVATION);

		return iri.asOriginalTestbedIri(getEntityIdMapperType());
	}

	@Override
	public boolean isManagedEntity(String iri) {
		FiestaIoTIri fiestaIri;
		try {
			fiestaIri = FiestaIoTIri.create(iri);
		} catch (IllegalArgumentException ex) {
			return false;
		}

		EntityIdMappers<?> entityMappers;
		if (fiestaIri.isObservation()) {
			entityMappers = EntityIdMappersFactory
			        .createObservationIdMappers(getEntityIdMapperType());
		} else {
			return false;
		}

		try {
			EntityIdMapper entity = entityMappers.get(fiestaIri.getHash());
		} catch (InvalidEntityIdException ex) {
			return false;
		}

		return true;
	}

	// @Override
	// public void
	// validateRegisteredParentEntity(Model observationInput) throws
	// NotRegisteredResourceException {
	// List<String> notRegisteredDevices =
	// validateRegisteredSensingDevices(observationInput);
	// if (!notRegisteredDevices.isEmpty()) {
	// throw new NotRegisteredResourceException(notRegisteredDevices);
	// }
	// }
	//
	// @Override
	// public void
	// validateMinimumDocument(Model observationInput) throws
	// NotValidMinimumModelException {
	// List<String> notValidObservations =
	// validateObservationMinimumDocument(observationInput);
	// if (!notValidObservations.isEmpty()) {
	// throw new NotValidMinimumObservationModelException(notValidObservations);
	// }
	// }

	@Override
	public void
	        validateRegisteredParentEntity(Model observationInput) throws NotRegisteredResourceException {
	}

	@Override
	public void
	        validateMinimumDocument(Model observationInput) throws NotValidMinimumModelException {
	}

	/**
	 * 
	 * @param observationInput
	 * @return list with not registered sensing devices
	 * @throws NotRegisteredResourceException
	 */
	private List<String>
	        validateRegisteredSensingDevices(Model observationInput) throws NotRegisteredResourceException {
		TripleStore rts = TripleStoreFactory.createResourcesTripleStore();

		// Find entities with property
		List<String> notValidSensingDevices = new ArrayList<String>();
		NodeIterator it =
		        observationInput.listObjectsOfProperty(Ssn.observedBy);
		if (!it.hasNext()) {
			// No reference to testbed
			throw new NotRegisteredResourceException();
		}
		while (it.hasNext()) {
			RDFNode node = it.next();
			if (node.isURIResource()) {
				// Validate linked resource is available
				EntityIdMapper om = EntityIdMapperFactory
				        .createResourceIdMapper(getEntityIdMapperType(),
				                                node.asResource().getURI());
				if (rts.getEntity(FiestaIoTIri.create(om).asIri()) != null) {
					continue;
				}
			}
			notValidSensingDevices.add(node.toString());
		}

		return notValidSensingDevices;
	}

	/**
	 * 
	 * @param observationInput
	 * @return list with not valid observations
	 * @throws NotValidMinimumObservationModelException
	 */
	private List<String>
	        validateObservationMinimumDocument(Model input) throws NotValidMinimumObservationModelException {
		// TODO: Check which is better: to use union between this
		// document and the ResourceModel or add the document
		// or even use just one SPARQL against the Resource model to avoid
		// checking sensing devices
		// OntModel ontObservation = FiestaIoTOntModel.getInstance();
		// ontObservation.add(input);
		Model ontObservation = ModelFactory
		        .createUnion(input, FiestaIoTOntModel.getInstance());
		try {
			List<String> totalObservations =
			        getTotalObservations(ontObservation);
			if (totalObservations.isEmpty()) {
				// There is no observation
				throw new NotValidMinimumObservationModelException();
			}

			List<String> validObservations =
			        getValidObservations(ontObservation);

			totalObservations.removeAll(validObservations);

			return totalObservations;
		} finally {
			// ontObservation.close();
		}
	}

	private List<String> getValidObservations(Model ontInput) {
		String sparqlObservationMinimumValidModel = IoTRegistryConfiguration
		        .getInstance().getSparqlObservationMinimumValidModel();

		List<String> validObservations = new ArrayList<>();
		Query queryMinimum =
		        QueryFactory.create(sparqlObservationMinimumValidModel);
		try (QueryExecution qExec =
		        QueryExecutionFactory.create(queryMinimum, ontInput)) {
			ResultSet results = qExec.execSelect();
			String col = results.getResultVars().get(0);
			results.forEachRemaining(x -> validObservations
			        .add(x.get(col).toString()));
		}

		return validObservations;
	}

	private List<String> getTotalObservations(Model ontInput) {
		String queryTotalString = IoTRegistryConfiguration.getInstance()
		        .getSparqlObservationTotalSsnObservation();
		// "SELECT ?o WHERE { ?o a <" + Ssn.Observation.getURI() + "> . }";

		List<String> totalObservations = new ArrayList<>();
		Query queryTotal = QueryFactory.create(queryTotalString);

		try (QueryExecution qExec =
		        QueryExecutionFactory.create(queryTotal, ontInput)) {
			ResultSet results = qExec.execSelect();
			String col = results.getResultVars().get(0);
			results.forEachRemaining(x -> totalObservations
			        .add(x.get(col).toString()));
		}

		return totalObservations;
	}

}
