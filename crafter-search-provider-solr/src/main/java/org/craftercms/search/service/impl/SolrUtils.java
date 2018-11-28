/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Utility class for converting a Solr document list into a simple map
 */
public abstract class SolrUtils {

    public static Collection<Map<String, Object>> extractDocs(SolrDocumentList docList) {
        Collection<Map<String, Object>> docs = new ArrayList<>(docList.size());

        for (SolrDocument doc : docList) {
            Map<String, Object> docMap = new LinkedHashMap<>();

            for (Map.Entry<String, Object> field : doc) {
                String name = field.getKey();
                Object value = field.getValue();

                // If the value is an Iterable with a single value, return just that one value. This is done for backwards
                // compatibility since Solr 4 did this for us before
                if (value instanceof Iterable) {
                    Iterator<?> iter = ((Iterable<?>)value).iterator();
                    if (iter.hasNext()) {
                        Object first = iter.next();
                        if (!iter.hasNext()) {
                            value = first;
                        }
                    }
                }

                docMap.put(name, value);
            }

            docs.add(docMap);
        }

        return docs;
    }

}
