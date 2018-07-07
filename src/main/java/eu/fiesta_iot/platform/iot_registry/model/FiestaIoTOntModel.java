/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.model;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FiestaIoTOntModel {

	private static Logger log =
	        LoggerFactory.getLogger(FiestaIoTOntModel.class);

	// private final OntModel ontModel;

	// private FiestaIoTOntModel() {
	// ontModel = ModelFactory
	// .createOntologyModel(FiestaIoTOntModelSpec.getInstance());
	// ontModel.read("etc/fiesta_iot.owl", null);
	// }

	public static OntModel getInstance() {
		OntModel ontModel = ModelFactory
		        .createOntologyModel(FiestaIoTOntModelSpec.getInstance());
		// Automatically load all the ontologies
		// included in the etc/fiesta_iot.owl file
		ontModel.read("etc/fiesta_iot.owl", null);

		// In the future we should try to use the following,
		// so this way there is no need to read from files again
		// We have tried and seems that it takes longer than using the above.
		// m =
		// ModelFactory.createOntologyModel(fiestaIotModel.getOntModelSpec(),
		// fiestaIotModel.getOntModel());

		return ontModel;
	}
}
