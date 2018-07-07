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

import javax.xml.bind.annotation.adapters.XmlAdapter;

import eu.fiesta_iot.platform.iot_registry.storage.sql.SparqlQuery.Scope;

public class JaxBCaseInsensitiveEnumAdapter extends XmlAdapter<String, Enum> {

	private Class[] enumClasses = { Scope.class };

	@Override
	public Enum unmarshal(String v) throws Exception {
		for (Class enumClass : enumClasses) {
			try {
				return (Enum) Enum.valueOf(enumClass, v.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
			}
		}
		return null;
	}

	@Override
	public String marshal(Enum v) throws Exception {
		return v.toString();
	}

}