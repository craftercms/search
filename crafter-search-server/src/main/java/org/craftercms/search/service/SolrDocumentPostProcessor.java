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

/**
 * Used to modify or enhance a Solr document after it's built.
 *
 * @author
 */
public interface SolrDocumentPostProcessor {

    /**
     * Processes the specified document to modify or enhance it.
     *
     * @param solrDoc the Solr document to process
     */
    void postProcess(SolrInputDocument solrDoc);

}
