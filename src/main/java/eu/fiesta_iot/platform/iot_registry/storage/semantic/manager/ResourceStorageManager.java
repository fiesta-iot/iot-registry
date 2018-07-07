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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
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
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumResourceModelException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStore;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.tdb.TripleStoreFactory;
import eu.fiesta_iot.utils.semantics.vocabulary.IotLite;
import eu.fiesta_iot.utils.semantics.vocabulary.Ssn;

public class ResourceStorageManager extends StorageManager {

	private static Logger log =
	        LoggerFactory.getLogger(ResourceStorageManager.class);

	public ResourceStorageManager(EntityIdMapperType mapperType) {
		super(mapperType);
	}

	public Model adaptEntity(Model input) {
		Set<EntityIdMapper> endpointSet = new HashSet<EntityIdMapper>();

		Model newResource = ModelFactory.createDefaultModel();

		// Resource to be used for ontology inference.
		// This way it is possible to know the inheritance,
		// if a class or individual, etc.
		OntModel ontResource = FiestaIoTOntModel.getInstance();
		ontResource.add(input);

//		Model modelSensingDevice =
//		        materializeInferenceForSensingDevice(ontResource);
//		Model modelQuantityKind =
//		        materializeInferenceForQuantityKind(ontResource);
//		input.add(modelSensingDevice);
//		input.add(modelQuantityKind);

		// Do not use listIndividuals as it is more probable that
		// only list the Subjects and not the ones in the Object
		StmtIterator stmts = input.listStatements();
		while (stmts.hasNext()) {
			Statement stmt = stmts.next();
			Resource s = stmt.getSubject();
			Property p = stmt.getPredicate();
			RDFNode o = stmt.getObject();

			Individual individual;

			String subjectForStmt = s.toString();
			String propertyForStmt = p.toString();
			String objectForStmt = o.toString();

			// TODO: Depending on speed it might be advisable to keep a list
			// of individuals, so once discovered it is not required to
			// semantically ask again for them

			// Every subject has to be referenceable
			// We don't mind whether it is blank or named node
			if (s.isURIResource()) {
				individual = ontResource.getIndividual(subjectForStmt);
				if (individual.isClass()) {
					// If is a class definition skip to next
					// We don't allow class definition
					// This code shouldn't be called if everything is as
					// expected
					// newResource.add(stmt);
					continue;
				}
			}

			// Convert subject to FIESTA-IoT IRI format
			EntityIdMapper rm = EntityIdMapperFactory
			        .createResourceIdMapper(getEntityIdMapperType(),
			                                subjectForStmt);
			Resource r = ResourceFactory
			        .createResource(FiestaIoTIri.create(rm).asIri());
			Statement newStmt = ResourceFactory.createStatement(r, p, o);

			// Object analysis
			if (o.isLiteral()) {
				if (propertyForStmt
				        .equalsIgnoreCase(IotLite.endpoint.getURI())) {
					Literal l = o.asLiteral();
					if (!l.getDatatype().equals(XSDDatatype.XSDanyURI)) {
						log.warn("Endpoint not correctly defined and future errors may arise");
					}

					EntityIdMapper ep = EntityIdMapperFactory
					        .createEndpointIdMapper(getEntityIdMapperType(),
					                                l.getLexicalForm());
					endpointSet.add(ep);

					newStmt = newStmt.changeObject(ResourceFactory
					        .createTypedLiteral(FiestaIoTIri.create(ep)
					                .asUri()));
				}
			} else if (o.isURIResource()) {
				individual = ontResource.getIndividual(objectForStmt);
				// Check if there is enough information to determine if an URI
				// Resource is an individual or a class
				// If null then no information can be extracted from the
				// statement and therefore we consider this Object as an entity
				// and not a class, which should be define in the included
				// ontologies
				if (individual == null || !individual.isClass()) {
					EntityIdMapper om = (!propertyForStmt
					        .equalsIgnoreCase(Ssn.hasDeployment.getURI()))
					                ? EntityIdMapperFactory
					                        .createResourceIdMapper(getEntityIdMapperType(),
					                                                objectForStmt)
					                : EntityIdMapperFactory
					                        .createTestbedIdMapper(getEntityIdMapperType(),
					                                               objectForStmt);
					r = ResourceFactory
					        .createResource(FiestaIoTIri.create(om).asIri());
					newStmt = newStmt.changeObject(r);
				} else if (individual.isClass()) {
					individual = ontResource.getIndividual(subjectForStmt);
					// TODO: Do not register as ssn:Deployment already
					// registered
					if (individual.hasRDFType(Ssn.Deployment)) {
						EntityIdMapper tm = EntityIdMapperFactory
						        .createTestbedIdMapper(getEntityIdMapperType(),
						                               subjectForStmt);
						Resource t = ResourceFactory.createResource(FiestaIoTIri
						        .create(tm).asIri());
						newStmt = ResourceFactory
						        .createStatement(t, newStmt.getPredicate(),
						                         newStmt.getObject());
					}
				}
			} else if (o.isAnon()) {
				EntityIdMapper om = EntityIdMapperFactory
				        .createResourceIdMapper(getEntityIdMapperType(),
				                                objectForStmt);
				r = ResourceFactory
				        .createResource(FiestaIoTIri.create(om).asIri());
				newStmt = newStmt.changeObject(r);
			}

			newResource.add(newStmt);
		}

		// The amount of time that this takes is aprox the same as
		// doing it at the beginning.
		// ontResource = FiestaIoTOntModel.getInstance();
		// ontResource.add(newResource);
		//
		// Model modelSensingDevice =
		// materializeInferenceForSensingDevice(ontResource);
		// Model modelQuantityKind =
		// materializeInferenceForQuantityKind(ontResource);
		//
		// newResource.add(modelSensingDevice);
		// newResource.add(modelQuantityKind);

		// Update Endpoints Mapper storage
		EntityIdMappers<?> endpointMappers = EntityIdMappersFactory
		        .createEndpointIdMappers(getEntityIdMapperType());
		endpointMappers.sync(endpointSet);

		// Update Resources Mapper storage
		EntityIdMappers<?> resourceMappers = EntityIdMappersFactory
		        .createResourceIdMappers(getEntityIdMapperType());
		OntClass deviceClass = ontResource
		        .createClass("http://purl.oclc.org/NET/ssnx/ssn#Device");
		for (ExtendedIterator<Individual> i =
		        ontResource.listIndividuals(deviceClass); i.hasNext();) {
			Individual instance = i.next();
			EntityIdMapper om = EntityIdMapperFactory
			        .createResourceIdMapper(getEntityIdMapperType(),
			                                instance.getURI());
			resourceMappers.sync(om);
		}

		// ontResource.close();

		return newResource;
	}

	private Model materializeInferenceForSensingDevice(Model resources) {
		// Con el rdfs:subClassOf+ me quito que salga tambien la clase actual.
		// Con * tambien saldría la actual.
		// Realmente pondría sensing device ya que los devices no creo que los
		// busquen por System
		String queryString =
		        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
		                     + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
		                     + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
		                     + "PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#>\n"
		                     + "CONSTRUCT { ?sensor rdf:type ?superClass .} \n"
		                     + "WHERE { " + "?sensor rdf:type ?type . \n"
		                     + "?type rdfs:subClassOf* ssn:SensingDevice . \n"
		                     + "?type rdfs:subClassOf+ ?superClass . \n"
		                     + "FILTER (!isBlank(?superClass) && ?superClass != owl:Thing && ?superClass != rdfs:Resource)}";

		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec =
		        QueryExecutionFactory.create(query, resources)) {
			return qexec.execConstruct();
		}
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
		if (fiesta.isResource() || fiesta.isEndpoint() || fiesta.isTestbed()) {
			return fiesta.asOriginalTestbedIri(getEntityIdMapperType());
		} else {
			return null;
		}
	}

	@Override
	public String
	        getOriginalEntityId(String hash,
	                            EntityType type) throws InvalidEntityIdException {
		if (type != EntityType.RESOURCE && type != EntityType.ENDPOINT_URL
		    && type != EntityType.TESTBED) {
			// Throw exception not found?
			return null;
		}

		return FiestaIoTIri.create(hash, type)
		        .asOriginalTestbedIri(getEntityIdMapperType());
	}

	@Override
	public boolean isManagedEntity(String iri) {
		// Validate namespace
		// Testbeds and resources are in the same data, but better separate and
		// do things properly
		FiestaIoTIri fiestaIri;
		try {
			fiestaIri = FiestaIoTIri.create(iri);
		} catch (IllegalArgumentException ex) {
			return false;
		}

		EntityIdMappers<?> entityMappers;
		if (fiestaIri.isResource()) {
			entityMappers = EntityIdMappersFactory
			        .createResourceIdMappers(getEntityIdMapperType());
		} else if (fiestaIri.isTestbed()) {
			entityMappers = EntityIdMappersFactory
			        .createTestbedIdMappers(getEntityIdMapperType());
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

	@Override
	public void
	        validateRegisteredParentEntity(Model resourceInput) throws NotRegisteredResourceException {
		Collection<String> notRegisteredTestbeds =
		        validateRegisteredTestbeds(resourceInput);
		if (!notRegisteredTestbeds.isEmpty()) {
			throw new NotRegisteredResourceException(notRegisteredTestbeds);
		}
	}

	@Override
	public void
	        validateMinimumDocument(Model resourceInput) throws NotValidMinimumModelException {
		List<String> notValidResources =
		        validateResourceMinimumDocument(resourceInput);
		if (!notValidResources.isEmpty()) {
			throw new NotValidMinimumResourceModelException(notValidResources);
		}
	}

	/**
	 * 
	 * @param input
	 * @return list with not registered testbeds / deployments
	 * @throws NotRegisteredResourceException
	 */
	private Collection<String>
	        validateRegisteredTestbeds(Model input) throws NotRegisteredResourceException {
		Set<String> notValidDeployments = new HashSet<String>();

		OntModel ontResource = FiestaIoTOntModel.getInstance();
		ontResource.add(input);

		// Identify new deployments
		ExtendedIterator<Individual> itIndividuals =
		        ontResource.listIndividuals(Ssn.Deployment);
		itIndividuals
		        .forEachRemaining(x -> notValidDeployments.add(x.getURI()));

		// Identify references to deployments
		NodeIterator itNode =
		        ontResource.listObjectsOfProperty(Ssn.hasDeployment);
		if (!itNode.hasNext()) {
			// No reference to testbed
			throw new NotRegisteredResourceException();
		}
		while (itNode.hasNext()) {
			RDFNode node = itNode.next();
			if (node.isURIResource()) {
				notValidDeployments.add(node.asResource().getURI());
			}
		}

		// Validate linked resource is available
		TripleStore rts = TripleStoreFactory.createResourcesTripleStore();
		for (Iterator<String> i = notValidDeployments.iterator(); i
		        .hasNext();) {
			String iri = i.next();
			EntityIdMapper om = EntityIdMapperFactory
			        .createTestbedIdMapper(getEntityIdMapperType(), iri);
			if (rts.getEntity(FiestaIoTIri.create(om).asIri()) != null) {
				i.remove();
			}
		}

		// ontObservation.close();

		// Only not valid deployments are left
		return notValidDeployments;
	}

	/**
	 * 
	 * @param observationInput
	 * @return list with not valid observations
	 * @throws NotValidMinimumResourceModelException
	 */
	private List<String>
	        validateResourceMinimumDocument(Model input) throws NotValidMinimumResourceModelException {
		// TODO: Check which is better: to use union between this
		// document and the ResourceModel or add the document
		// or even use just one SPARQL against the Resource model to avoid
		// checking sensing devices
		// OntModel ontResource = FiestaIoTOntModel.getInstance();
		// ontResource.add(input);
		Model ontResource = ModelFactory
		        .createUnion(input, FiestaIoTOntModel.getInstance());

		try {
			List<String> totalResources = getTotalResources(ontResource);
			if (totalResources.isEmpty()) {
				throw new NotValidMinimumResourceModelException();
			}

			List<String> validResources = getValidResources(ontResource);

			totalResources.removeAll(validResources);

			return totalResources;
		} finally {
			// ontResource.close();
		}
	}

	private List<String> getValidResources(Model input) {
		String sparqlResourceMinimumValidModel = IoTRegistryConfiguration
		        .getInstance().getSparqlResourceMinimumValidModel();

		List<String> validResources = new ArrayList<>();
		Query queryMinimum =
		        QueryFactory.create(sparqlResourceMinimumValidModel);

		try (QueryExecution qExec =
		        QueryExecutionFactory.create(queryMinimum, input)) {
			ResultSet results = qExec.execSelect();
			String col = results.getResultVars().get(0);
			// System.out.println("getValidResources");
			// ResultSetFormatter.out(System.out, results);
			results.forEachRemaining(x -> validResources
			        .add(x.get(col).toString()));
		}

		return validResources;
	}

	private List<String> getTotalResources(Model input) {
		List<String> totalResources = new ArrayList<>();
		String queryTotalString = IoTRegistryConfiguration.getInstance()
		        .getSparqlResourceTotalSsnSensingDevice();

		Query queryTotal = QueryFactory.create(queryTotalString);
		try (QueryExecution qExec =
		        QueryExecutionFactory.create(queryTotal, input)) {
			ResultSet results = qExec.execSelect();
			String col = results.getResultVars().get(0);
			// System.out.println("getTotalResources");
			// ResultSetFormatter.out(System.out, results);
			results.forEachRemaining(x -> totalResources
			        .add(x.get(col).toString()));
		}

		return totalResources;
	}
}
