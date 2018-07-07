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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* From https://stackoverflow.com/questions/9520716/cxf-jaxrs-how-do-i-pass-date-as-queryparam

Options are : 

1) Change your method signature to accept a string. Attempt to construct a Date object out 
of that and if that fails, use your own custom SimpleDateFormat class to parse it.

static final DateFormat CRAZY_FORMAT = new SimpleDateFormat("");

public String getData(@QueryParam("date") String dateString) {
    final Date date;
    try {
        date = new Date(dateString); // yes, I know this is a deprecated method
    } catch(Exception e) {
        date = CRAZY_FORMAT.parse(dateString);
    }
}

2) Define your own parameter class that does the logic mentioned above. Give it a string 
constructor or static valueOf(String) method that invokes the logic. And an additional 
method to get the Date when all is said and done.

public class DateParameter implements Serializable {
    public static DateParameter valueOf(String dateString) {
        try {
            date = new Date(dateString); // yes, I know this is a deprecated method
        } catch(Exception e) {
            date = CRAZY_FORMAT.parse(dateString);
        }
    }

    private Date date;
    // Constructor, Getters, Setters
}

public String getData(@QueryParam("date") DateParameter dateParam) {
    final Date date = dateParam.getDate();
}


3) You can register a ParamConverterProvider for dates. Where its logic is simply the same 
as mentioned for the other options above. The parameter handler is evaluated before it tries 
the default unbundling logic.
  
public class DateParameterConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> type, Type type1, Annotation[] antns) {
        if (Date.class.equals(type)) {
            @SuppressWarnings("unchecked")
            ParamConverter<T> paramConverter = (ParamConverter<T>) new DateParameterConverter();
            return paramConverter;
        }
        return null;
    }

}

public class DateParameterConverter implements ParamConverter<Date> {

    public static final String format = "yyyy-MM-dd"; // set the format to whatever you need

    @Override
    public Date fromString(String string) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        try {
            return simpleDateFormat.parse(string);
        } catch (ParseException ex) {
            throw new WebApplicationException(ex);
        }
    }

    @Override
    public String toString(Date t) {
        return new SimpleDateFormat(format).format(t);
    }

}

*/
public class StartLocalDateTimeParameter {
	Logger log = LoggerFactory.getLogger(getClass());

	private static final DateTimeFormatter NUMBER_STRING_FORMAT = new DateTimeFormatterBuilder()
		    .parseCaseInsensitive()
		    .appendValue(ChronoField.YEAR, 4).appendPattern("MMdd[HH[mm[ss]]]")
		    .parseDefaulting(ChronoField.HOUR_OF_DAY, 00)
		    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 00)
		    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 00)
		    .toFormatter();	

	private static final DateTimeFormatter DASHED_STRING_FORMAT =
			 new DateTimeFormatterBuilder()
			    .parseCaseInsensitive()
			    .appendValue(ChronoField.YEAR, 4).appendPattern("-MM-dd")
			    .parseDefaulting(ChronoField.HOUR_OF_DAY, 00)
			    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 00)
			    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 00)
			    .toFormatter();	

	private LocalDateTime date;
	private final DateTimeFormatter[] AVAILABLE_FORMATS =
	        { NUMBER_STRING_FORMAT, DASHED_STRING_FORMAT};

	public StartLocalDateTimeParameter(String dateString) {
		for (DateTimeFormatter format : AVAILABLE_FORMATS) {
			try {
				date = LocalDateTime.parse(dateString, format);
				break;
			} catch (DateTimeParseException e) {
				log.error(e.toString());
			}
		}

		if (date == null) {
			throw new IllegalArgumentException("Unable to obtain date from "
			                                   + dateString
			                                   + ". Unsupported string format.");
		}
	}

	public LocalDateTime getDateTime() {
		return date;
	}
}
