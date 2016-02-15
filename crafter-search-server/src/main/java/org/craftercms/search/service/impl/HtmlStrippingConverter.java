package org.craftercms.search.service.impl;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.craftercms.search.exception.SolrDocumentBuildException;

/**
 * Created by alfonsovasquez on 4/2/16.
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
