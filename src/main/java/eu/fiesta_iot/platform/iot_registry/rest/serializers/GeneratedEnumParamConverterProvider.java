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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GeneratedEnumParamConverterProvider
        implements ParamConverterProvider {

	Logger log = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T> getConverter(final Class<T> rawType,
	                                          final Type genericType,
	                                          final Annotation[] annotations) {

		// TODO: Analyze how we can do that using a provider class like the
		// examples in
		// http://www.programcreek.com/java-api-examples/index.php?api=javax.ws.rs.ext.ParamConverterProvider
		final class EnumParamConverter implements ParamConverter<Enum<?>> {

			@Override
			public Enum<?> fromString(String value) {
				try {
					final Method fromValueMethod =
					        rawType.getMethod("fromString", String.class);
					// rawType.getMethod("fromValue", String.class);

					return (Enum<?>) rawType
					        .cast(fromValueMethod.invoke(null, value));
				} catch (IllegalAccessException | IllegalArgumentException
				         | InvocationTargetException e) {
					String param = null;
					for (Annotation a : annotations) {
						Class<? extends Annotation> type = a.annotationType();
						if (type.isAssignableFrom(javax.ws.rs.QueryParam.class)) {
							param = ((javax.ws.rs.QueryParam) a).value();
							break;
						}
					}

					throw new BadRequestException("Given value <" + value
					                              + "> for parameter <" + param
					                              + "> is not valid");
				} catch (Exception e) {
					log.error("Error while converting String to Enum", e);
					return null;
				}
			}

			@Override
			public String toString(Enum<?> value) {
				return value.toString();
			}
		}

		if (rawType.isEnum()) {
			return (ParamConverter<T>) new EnumParamConverter();
		} else {
			return null;
		}
	}
}
