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
package org.craftercms.search.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.craftercms.search.commons.service.impl.AbstractDocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * The Solr document is represent in Java as object, which then can be sent to the server.
 *
 * @author Michael Chen
 * @author Alfonso Vásquez
 */
public class SolrDocumentBuilderImpl extends AbstractDocumentBuilder<SolrInputDocument> {

    private static final Logger logger = LoggerFactory.getLogger(SolrDocumentBuilderImpl.class);

    public ModifiableSolrParams buildParams(String site, String id, String prefix, String suffix,
                                            Map<String, List<String>> fields) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        String finalId = site + ":" + id;

        prefix = prefix != null? prefix : "";
        suffix = suffix != null? suffix : "";

        logger.debug("Building params for update request for {}", finalId);

        String now = formatAsIso(Instant.now());

        params.set(prefix + idFieldName + suffix, finalId);
        params.set(prefix + rootIdFieldName + suffix, finalId);
        params.set(prefix + siteFieldName + suffix, site);
        params.set(prefix + localIdFieldName + suffix, id);
        params.set(prefix + publishingDateFieldName + suffix, now);
        params.set(prefix + publishingDateAltFieldName + suffix, now);

        if (MapUtils.isNotEmpty(fields)) {
            for (Map.Entry<String, List<String>> field : fields.entrySet()) {
                String fieldName = field.getKey();
                List<String> fieldValues = field.getValue();
                String[] values = new String[fieldValues.size()];

                for (int i = 0; i < fieldValues.size(); i++) {
                    values[i] = fieldValueConverter.convert(fieldName, fieldValues.get(i)).toString();
                }

                params.set(prefix + fieldName + suffix, values);
            }
        }

        return params;
    }

    @Override
    protected SolrInputDocument createDoc() {
        return new SolrInputDocument();
    }

    @Override
    protected void addField(final SolrInputDocument doc, final String fieldName, final Object fieldValue) {
        doc.addField(fieldName, fieldValue);
    }

}
