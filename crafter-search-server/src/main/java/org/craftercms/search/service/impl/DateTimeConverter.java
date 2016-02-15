package org.craftercms.search.service.impl;

import javax.annotation.PostConstruct;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 4/2/16.
 */
public class DateTimeConverter implements FieldValueConverter {

    protected String dateTimeFieldPattern;

    @PostConstruct
    public void init() {
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    @Required
    public void setDateTimeFieldPattern(String dateTimeFieldPattern) {
        this.dateTimeFieldPattern = dateTimeFieldPattern;
    }

    @Override
    public Object convert(String name, String value) {
        DateTimeFormatter incomingFormatter = DateTimeFormat.forPattern(dateTimeFieldPattern);
        DateTimeFormatter outgoingFormatter = ISODateTimeFormat.dateTime();

        return outgoingFormatter.print(incomingFormatter.parseDateTime(value));
    }

}
