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
package org.craftercms.search.service.impl;

import org.apache.solr.common.SolrInputDocument;
import org.craftercms.search.service.SolrDocumentPostProcessor;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by alfonsovasquez on 25/8/16.
 */
public class AddIndexingDatePostProcessor implements SolrDocumentPostProcessor {

    public static final String DEFAULT_INDEXING_DATE_FIELD_NAME = "indexingDate_dt";

    protected String indexingDateFieldName;

    public AddIndexingDatePostProcessor() {
        indexingDateFieldName = DEFAULT_INDEXING_DATE_FIELD_NAME;
    }

    public void setIndexingDateFieldName(String indexingDateFieldName) {
        this.indexingDateFieldName = indexingDateFieldName;
    }

    @Override
    public void postProcess(SolrInputDocument solrDoc) {
        solrDoc.addField(indexingDateFieldName, formatAsIso(DateTime.now()));
    }

    protected String formatAsIso(DateTime dateTime) {
        return ISODateTimeFormat.dateTime().withZoneUTC().print(dateTime);
    }

}
