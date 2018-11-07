package org.craftercms.search.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

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
