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

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.riot.system.stream.LocatorFile;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.iot_registry.config.IoTRegistryConfiguration;

public class FiestaIoTOntModelSpec extends OntModelSpec {

	private static Logger log =
	        LoggerFactory.getLogger(FiestaIoTOntModelSpec.class);

	private static final OntModelSpec fiestaIoTOntModelSpec =
	        new FiestaIoTOntModelSpec();

	private FiestaIoTOntModelSpec() {
		super(OntModelSpec.OWL_MEM_MICRO_RULE_INF);

		// Old way of retrieving resources path
		// It doesn't always work so it's better to user ServletContext
		// String locatorPath =
		// TripleStore.class.getClassLoader().getResource("etc").getPath();
		// locatorPath = (new File(locatorPath)).getParent();

		String locatorPath = IoTRegistryConfiguration.getInstance()
		        .getServletContext().getRealPath("/WEB-INF/classes");

		log.debug("FIESTA IoT default configuration at " + locatorPath);

		// Locate the ont-policy.rdf file and the ontologies to be used
		FileManager fm = FileManager.get().clone();
		// TODO: There is a bug to be reported that LocatorFiles are not
		// considered when added
		fm.addLocatorFile(locatorPath);
		OntDocumentManager docMngr =
		        new OntDocumentManager(fm, "etc/fiesta_iot-ont-policy.rdf");
		// Last tests seem to be working without the FileManager
		// but let's preserve the former till futher tests acomplished
		// OntDocumentManager docMngr = new
		// OntDocumentManager("etc/fiesta_iot-ont-policy.rdf");

		// Parsing data inside the ont-policy.rdf file
		StreamManager sm = StreamManager.makeDefaultStreamManager();
		sm.addLocator(new LocatorFile(locatorPath));
		StreamManager.setGlobal(sm);

		this.setDocumentManager(docMngr);
	}

	public static OntModelSpec getInstance() {
		// if (fiestaIoTOntModelSpec == null) {
		// log.info("Creating FIESTA IoT Ontology Model Spec");
		// fiestaIoTOntModelSpec = new FiestaIoTOntModelSpec();
		// }

		return fiestaIoTOntModelSpec;

		// return new FiestaIoTOntModelSpec();
	}

}
