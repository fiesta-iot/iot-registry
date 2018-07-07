/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.rest.serializers;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "graphs")
public class GraphsXmlSerializer {
	@XmlElement(name = "graph")
	public List<String> list;

	public GraphsXmlSerializer() {
		list = new ArrayList<String>();
	}
	
	public GraphsXmlSerializer(List<String> list) {
		this.list = new ArrayList<String>(list);
	}
}
