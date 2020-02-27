/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.search.commons.service.impl;

import org.craftercms.search.commons.service.FieldValueConverter;
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