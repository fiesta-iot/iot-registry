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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class JaxBDateThreadSafeAdapter extends XmlAdapter<String, Date> {

	/**
	 * Thread safe {@link DateFormat}.
	 */
	private static final ThreadLocal<DateFormat> DATE_FORMAT_TL =
	        new ThreadLocal<DateFormat>() {

		        @Override
		        protected DateFormat initialValue() {
			        // return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	                // ISO 8601 format
			        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		        }

	        };

	@Override
	public String marshal(Date v) throws Exception {
		return DATE_FORMAT_TL.get().format(v);
	}

	@Override
	public Date unmarshal(String v) throws Exception {
		return DATE_FORMAT_TL.get().parse(v);
	}
}
