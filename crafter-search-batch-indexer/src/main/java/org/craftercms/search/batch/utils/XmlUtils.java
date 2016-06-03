/*
 * Copyright (C) 2007-2015 Crafter Software Corporation.
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
package org.craftercms.search.batch.utils;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

/**
 * Utility methods for XML.
 *
 * @author avasquez
 */
public class XmlUtils {

    private XmlUtils() {
    }

    /**
     * Reads an XML to generate a DOM.
     *
     * @param file      the file to read
     * @param encoding  the encoding of the file
     *
     * @return the DOM
     */
    public static Document readXml(File file, String encoding) throws DocumentException {
        SAXReader reader = new SAXReader();
        reader.setMergeAdjacentText(true);

        if (StringUtils.isNotEmpty(encoding)) {
            reader.setEncoding(encoding);
        }

        return reader.read(file);
    }

}
