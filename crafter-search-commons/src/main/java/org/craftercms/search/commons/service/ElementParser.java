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
package org.craftercms.search.commons.service;

import org.dom4j.Element;

/**
 * Implementations parse the specified element to generate fields for the document.
 *
 * @author avasquez
 */
public interface ElementParser<T> {

    /**
     * Parses the given element, generating one or more Solr fields and adding them to the given document.
     *
     * @param element           the element to parse
     * @param fieldName         the field name that should be used for the main Solr field (by default will be the
     *                          path of the element in the tree plus the element name)
     * @param parentFieldName   the field name of the parent element
     * @param doc               the document to add the generated fields
     * @param parserService     the parser service used normally to parse sub elements
     *
     * @return true if the element was parsed or handled, false otherwise
     */
    boolean parse(Element element, String fieldName, String parentFieldName, T doc,
                  ElementParserService<T> parserService);

}
