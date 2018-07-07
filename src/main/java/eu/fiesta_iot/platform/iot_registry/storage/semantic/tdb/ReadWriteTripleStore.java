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

import java.util.EnumSet;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotRegisteredResourceException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.exceptions.NotValidMinimumModelException;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.manager.StorageManager;
import eu.fiesta_iot.platform.iot_registry.storage.semantic.namedgraph.NamedGraphsManager;

public class ReadWriteTripleStore extends ReadOnlyTripleStore {

	private static Logger log =
	        LoggerFactory.getLogger(ReadWriteTripleStore.class);

	public ReadWriteTripleStore(NamedGraphsManager namedGraphsManager,
	        StorageManager storageManager) {

		super(namedGraphsManager, storageManager);
	}

	@Override
	public NamedGraphsManager getGraphsManager() {
		return (NamedGraphsManager) super.getGraphsManager();
	}

	@Override
	public Model
	        addEntity(Model m,
	                  EnumSet<StorageManager.ValidationLevel> validate) throws NotRegisteredResourceException,
	                                                                    NotValidMinimumModelException {
		storageManager.validateEntity(m, validate);

		// Adapt resource based on associated manager
		Model adapted = storageManager.adaptEntity(m);

		// Update model with new resource
		// TODO: check whether to use Dataset or Model transactions
		// Answer: it seems model transactions is the old way [1]
		// [1]:
		// http://stackoverflow.com/questions/11658818/difference-between-model-commit-and-dataset-commit

		try (RDFConnection conn = TripleStoreFactory.getRdfConnection()) {
			conn.load(getWriteModel(), adapted);
		}

		return adapted;
	}

	@Override
	public Model deleteEntity(String iri) {
		return deleteEntityJena(iri);
	}

	private Model deleteEntityJena(String iri) {
		return Txn.calculateWrite(dataset, () -> {
			// TODO: Delete entity is only available on the currently
			// considered working graph. We have to think on how to extend
			// for the others.
			Resource resource = dataset.getNamedModel(getWriteModel()).getResource(iri);
			Model out = createModelFromResource(resource);
			if (out != null) {
				resource.removeProperties();
				dataset.commit();
			} else {
				dataset.abort();
			}

			// // From original PoC
			// if (model.contains(resource, null) == true) {
			// out = createModelFromResource(resource);
			// resource.removeProperties();
			// }
			return out;
		});
	}
}
