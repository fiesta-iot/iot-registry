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
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalDateTimeParamConverterProvider
        implements ParamConverterProvider {

	private static final Logger log =
	        LoggerFactory.getLogger(LocalDateTimeParamConverterProvider.class);

	private static class LocalDateTimeParamConverter
	        implements ParamConverter<LocalDateTime> {
		private static final DateTimeFormatterBuilder NUMBER_STRING_FORMAT =
		        new DateTimeFormatterBuilder().parseCaseInsensitive()
		                .appendValue(ChronoField.YEAR, 4)
		                .appendPattern("MMdd[HH[mm[ss]]]");

		private static final DateTimeFormatterBuilder DASHED_STRING_FORMAT =
		        new DateTimeFormatterBuilder().parseCaseInsensitive()
		                .appendValue(ChronoField.YEAR, 4)
		                .appendPattern("-MM-dd");

		private final DateTimeFormatterBuilder[] AVAILABLE_FORMATS =
		        { NUMBER_STRING_FORMAT, DASHED_STRING_FORMAT };

		private static final String START_DATE_REGEX_QUERY_PARAM = "(?i)from|start";
		
		private static DateTimeFormatter
		        asStartOfDay(DateTimeFormatterBuilder builder) {
			return builder.parseDefaulting(ChronoField.HOUR_OF_DAY, 00)
			        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 00)
			        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 00)
			        .toFormatter();
		}

		private static DateTimeFormatter
		        asEndOfDay(DateTimeFormatterBuilder builder) {
			return builder.parseDefaulting(ChronoField.HOUR_OF_DAY, 23)
			        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 59)
			        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 59)
			        .toFormatter();
		}

		private Annotation[] annotations;
		
		public LocalDateTimeParamConverter() {
		}
		
		public LocalDateTimeParamConverter(Annotation[] annotations) {
			this.annotations = annotations;
		}
		
		@Override
		public LocalDateTime fromString(String value) {
			LocalDateTime dateTime = null;
			String queryParamName = null;
			for (DateTimeFormatterBuilder formatterBuilder : AVAILABLE_FORMATS) {
				try {
					DateTimeFormatter formatter = null;
					if (queryParamName.matches(START_DATE_REGEX_QUERY_PARAM)) {
						formatter = asStartOfDay(formatterBuilder);
					} else {
						formatter = asEndOfDay(formatterBuilder);
					}

					dateTime = LocalDateTime.parse(value, formatter);
					break;
				} catch (DateTimeParseException e) {
					log.error(e.toString());
				}
			}

			if (dateTime == null) {
				throw new IllegalArgumentException("Unable to obtain date from "
				                                   + value
				                                   + ". Unsupported string format.");
			} else {
				return dateTime;
			}
		}

		@Override
		public String toString(LocalDateTime value) {
			// Default using NUMBER_STRING_FORMAT
			String queryParamName = null;
			DateTimeFormatter formatter = (queryParamName.matches(START_DATE_REGEX_QUERY_PARAM))
			        ? asStartOfDay(NUMBER_STRING_FORMAT)
			        : asEndOfDay(NUMBER_STRING_FORMAT);

			return value.format(formatter);
		}

		private String getQueryParamName() {
			for (Annotation a : annotations) {
				Class<? extends Annotation> type = a.annotationType();
				if (type.isAssignableFrom(javax.ws.rs.QueryParam.class)) {
					return ((javax.ws.rs.QueryParam) a).value();
				}
			}
			return null;
		}
	}

	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType,
	                                          Type genericType,
	                                          Annotation[] annotations) {
		
		if (rawType.getName().equals(LocalDateTime.class.getName())) {
			return (ParamConverter<T>) new LocalDateTimeParamConverter(annotations);
		}
		return null;
	}

}
