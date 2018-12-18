/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.search.service.impl;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.craftercms.search.exception.SolrDocumentBuildException;
import org.craftercms.search.service.FieldValueConverter;

/**
 * {@link FieldValueConverter} that strips all HTML tags from a field.
 *
 * @author avasquez
 */
public class HtmlStrippingConverter implements FieldValueConverter {

    private static final int BUFFER_SIZE = 1024 * 10;

    @Override
    public Object convert(String name, String value) {
        StringReader reader = new StringReader(value);
        HTMLStripCharFilter htmlStripper = new HTMLStripCharFilter(reader);
        char[] buffer = new char[BUFFER_SIZE];
        StringBuilder strippedValue = new StringBuilder();

        try {
            int charsRead;
            do {
                charsRead = htmlStripper.read(buffer);
                if (charsRead > 0) {
                    strippedValue.append(buffer, 0, charsRead);
                }
            } while (charsRead >= 0);
        } catch (IOException e) {
            throw new SolrDocumentBuildException("Error while performing HTML stripping for field '" + name + "'", e);
        }

        return strippedValue.toString();
    }

}
