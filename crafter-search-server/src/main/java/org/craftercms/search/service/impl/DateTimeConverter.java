package org.craftercms.search.service.impl;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link FieldValueConverter} that formats a field in a source pattern to the ISO date format.
 *
 * @author avasquez
 */
public class DateTimeConverter implements FieldValueConverter {

    private String dateTimeFieldPattern;

    @Required
    public void setDateTimeFieldPattern(String dateTimeFieldPattern) {
        this.dateTimeFieldPattern = dateTimeFieldPattern;
    }

    @Override
    public Object convert(String name, String value) {
        DateTimeFormatter incomingFormatter = DateTimeFormat.forPattern(dateTimeFieldPattern).withZoneUTC();
        DateTimeFormatter outgoingFormatter = ISODateTimeFormat.dateTime();

        return outgoingFormatter.print(incomingFormatter.parseDateTime(value));
    }

}
