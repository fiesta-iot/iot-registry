/*******************************************************************************
 * Copyright (c) 2018 Jorge Lanza, 
 *                    David Gomez, 
 *                    Luis Sanchez,
 *                    Juan Ramon Santana
 *
 * For the full copyright and license information, please view the LICENSE
 * file that is distributed with this source code.
 *******************************************************************************/
package eu.fiesta_iot.platform.iot_registry.storage.sql;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

// TODO: Think on including https://github.com/migesok/jaxb-java-time-adapters
public class JaxBOffsetDateTimeAdapter
        extends XmlAdapter<String, OffsetDateTime> {

	@Override
	public String marshal(OffsetDateTime v) throws Exception {

		return v.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	@Override
	public OffsetDateTime unmarshal(String v) throws Exception {
		return OffsetDateTime.parse(v, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
