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
package org.craftercms.search.batch.utils.xml;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 * Processes an XML DOM to modify or enhance it.
 *
 * @author avasquez
 */
public interface DocumentProcessor {

    /**
     * Processes the specified XML DOM.
     *
     * @param document      the DOM
     * @param file          the XML file
     * @param rootFolder    the root folder where this file is located
     *
     * @return the processed DOM
     */
    Document process(Document document, File file, String rootFolder) throws DocumentException;

}
