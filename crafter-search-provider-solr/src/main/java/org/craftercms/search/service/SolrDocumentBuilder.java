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

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.craftercms.search.v3.service.DocumentBuilder;

/**
 * <p/>
 * The purpose of this class is to convert a generic XML document:<br>
 * <pre>
 *     &lt;page&gt;
 *      &lt;file-name&gt;index.xml&lt;/file-name&gt;
 *      &lt;showAboutAuthor&gt;false&lt;/showAboutAuthor&gt;
 *      &lt;showSearchBox&gt;true&lt;/showSearchBox&gt;
 *     &lt;/page&gt;
 * </pre>
 * into a Solr Document:
 * <pre>
 *     &lt;add&gt;
 *      &lt;doc&gt;
 *          &lt;field name="file-name"&gt;index.xml&lt;/field&gt;
 *          &lt;field name="showAboutAuthor"&gt;false&lt;/field&gt;
 *          &lt;field name="showSearchBox"&gt;true&lt;/field&gt;
 *      &lt;/doc&gt;
 *     &lt;/add&gt;
 * </pre>
 * <p/>
 * The Solr document is represent in Java as a {@link SolrInputDocument} object, which then can be sent to the server.
 *
 * @author Michael Chen
 * @author Alfonso Vásquez
 */
public interface SolrDocumentBuilder extends DocumentBuilder<SolrInputDocument> {

    /**
     * Builds a set of SolrParams for an update request from the provided multi value map of fields
     *
     * @param site      the Crafter site name the content belongs to
     * @param prefix    the common prefix for the fields (null or empty for no prefix)
     * @param suffix    the common suffix for the fields (null or empty for no suffix)
     * @param id        value for the "localId" field in the Solr document (final doc id is built as site:localId)
     * @param fields    fields to add to solr document.
     *
     * @return the Solr document
     */
    ModifiableSolrParams buildParams(String site, String id, String prefix, String suffix,
                                     Map<String, List<String>> fields);

}
