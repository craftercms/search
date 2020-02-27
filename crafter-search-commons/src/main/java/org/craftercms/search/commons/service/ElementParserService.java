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
 * Implementations normally use several {@link ElementParser}s to parse the elements and it's sub-elements to generate
 * new fields on a document for indexing.
 *
 * @author avasquez
 */
public interface ElementParserService<T> {

    /**
     * Parses the given element, generating one or more Solr fields and adding them to the given document.
     *
     * @param element           the element to parse
     * @param parentFieldName   the field name of the parent
     * @param doc               the document to add the generated fields
     */
    void parse(Element element, String parentFieldName, T doc);

}
