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
package org.craftercms.search.service;

import org.apache.solr.common.SolrInputDocument;
import org.dom4j.Element;

/**
 * Implementations normally use several {@link ElementParser}s to parse the elements and it's sub-elements to generate
 * a Solr fields.
 *
 * @author avasquez
 */
public interface ElementParserService {

    /**
     * Parses the given element, generating one or more Solr fields and adding them to the given document.
     *
     * @param element           the element to parse
     * @param parentFieldName   the field name of the parent
     * @param solrDoc           the Solr document to add the generated fields
     */
    void parse(Element element, String parentFieldName, SolrInputDocument solrDoc);

}
