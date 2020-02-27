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

import java.util.List;
import java.util.Map;

import org.craftercms.search.commons.exception.DocumentBuildException;

/**
 * Transforms an XML document to te appropriate format for the search engine.
 * @param <T> the document type for the search engine
 */
public interface DocumentBuilder<T> {

    /**
     * Builds a document from the input XML.
     *
     * @param site                   the Crafter site name the content belongs to
     * @param id                     value for the "localId" field in the document (final doc id is built as
     *                               site:localId)
     * @param xml                    the input XML
     * @param ignoreRootInFieldNames ignore the root element of the input XML in field names
     * @return the document
     * @throws DocumentBuildException
     *
     */
    T build(String site, String id, String xml, boolean ignoreRootInFieldNames) throws DocumentBuildException;

    /**
     * Builds a document from the provided multi value map of fields
     *
     * @param site      the Crafter site name the content belongs to
     * @param id        value for the "localId" field in the document (final doc id is built as site:localId)
     * @param fields    fields to add to solr document.
     *
     * @return the document
     */
    T build(String site, String id, Map<String, List<String>> fields) throws DocumentBuildException;

}
