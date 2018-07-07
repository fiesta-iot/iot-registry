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
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name = "observations")
public class ObservationListSerializerHelper
        implements ListSerializerHelper<String> {
	@XmlElement(name = "observation")
	@JsonProperty(value = "observations")
	public List<String> list;

	public ObservationListSerializerHelper() {
		list = new ArrayList<String>();
	}

	public ObservationListSerializerHelper(List<String> list) {
		this.list = Collections.unmodifiableList(list);;
	}

	public ListSerializerHelper<String> getInstance(List<String> list) {
		return new ObservationListSerializerHelper(list);
	}

}
