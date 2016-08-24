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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.craftercms.search.service.SolrDocumentPostProcessor;

import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_ID_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_LOCAL_ID_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_SITE_FIELD_NAME;
import static org.craftercms.search.service.impl.SubDocumentElementParser.DEFAULT_CONTENT_TYPE_FIELD_NAME;
import static org.craftercms.search.service.impl.SubDocumentElementParser.DEFAULT_PARENT_ID_FIELD_NAME;

/**
 * Implementation of {@link SolrDocumentPostProcessor} that can "denormalize" the parent and it's children documents,
 * that means, it copies the fields from the parent to the children and the fields of the children to the parent. This
 * is very helpful, for example, when you're doing a search on the children, but you need also certain fields from
 * the parent.
 *
 * @author avasquez
 */
public class DenormalizingPostProcessor implements SolrDocumentPostProcessor {

    public static final String[] DEFAULT_FIELDS_TO_IGNORE = { DEFAULT_ID_FIELD_NAME, DEFAULT_SITE_FIELD_NAME,
        DEFAULT_LOCAL_ID_FIELD_NAME, DEFAULT_CONTENT_TYPE_FIELD_NAME, DEFAULT_PARENT_ID_FIELD_NAME };

    protected String[] fieldsToIgnore;
    protected boolean copyChildrenFieldsToParent;
    protected boolean copyParentFieldsToChildren;

    public DenormalizingPostProcessor() {
        fieldsToIgnore = DEFAULT_FIELDS_TO_IGNORE;
        copyChildrenFieldsToParent = true;
        copyParentFieldsToChildren = true;
    }

    public void setFieldsToIgnore(String[] fieldsToIgnore) {
        this.fieldsToIgnore = fieldsToIgnore;
    }

    public void setCopyChildrenFieldsToParent(boolean copyChildrenFieldsToParent) {
        this.copyChildrenFieldsToParent = copyChildrenFieldsToParent;
    }

    public void setCopyParentFieldsToChildren(boolean copyParentFieldsToChildren) {
        this.copyParentFieldsToChildren = copyParentFieldsToChildren;
    }

    @Override
    public void postProcess(SolrInputDocument solrDoc) {
        if (solrDoc.hasChildDocuments()) {
            Collection<SolrInputDocument> childDocs = solrDoc.getChildDocuments();
            Collection<SolrInputField> parentFields = getParentFields(solrDoc);
            Collection<SolrInputField> childrenFields = getChildrenFields(childDocs);

            if (copyChildrenFieldsToParent) {
                copyChildrenFieldsToParent(childrenFields, solrDoc);
            }
            if (copyParentFieldsToChildren) {
                copyParentFieldsToChildren(parentFields, childDocs);
            }
        }
    }

    protected Collection<SolrInputField> getParentFields(SolrInputDocument parentDoc) {
        List<SolrInputField> fields = new ArrayList<>();

        for (SolrInputField field : parentDoc) {
            if (!ArrayUtils.contains(fieldsToIgnore, field.getName())) {
                fields.add(field);
            }
        }

        return fields;
    }

    protected Collection<SolrInputField> getChildrenFields(Collection<SolrInputDocument> childDocs) {
        Map<String, SolrInputField> childrenFields = new LinkedHashMap<>();

        for (SolrInputDocument childDoc : childDocs) {
            for (SolrInputField field : childDoc) {
                String fieldName = field.getName();
                if (!ArrayUtils.contains(fieldsToIgnore, fieldName)) {
                    if (childrenFields.containsKey(fieldName)) {
                        childrenFields.get(fieldName).addValue(field.getValue(), field.getBoost());
                    } else {
                        childrenFields.put(fieldName, field.deepCopy());
                    }
                }
            }
        }

        return childrenFields.values();
    }

    protected void copyChildrenFieldsToParent(Collection<SolrInputField> childrenFields, SolrInputDocument parentDoc) {
        for (SolrInputField childField : childrenFields) {
            String fieldName = childField.getName();
            if (!parentDoc.containsKey(fieldName)) {
                parentDoc.put(fieldName, childField);
            }
        }
    }

    protected void copyParentFieldsToChildren(Collection<SolrInputField> parentFields,
                                              Collection<SolrInputDocument> childDocs) {
        for (SolrInputDocument childDoc : childDocs) {
            for (SolrInputField parentField : parentFields) {
                String fieldName = parentField.getName();
                if (!childDoc.containsKey(fieldName)) {
                    childDoc.put(fieldName, parentField);
                }
            }
        }
    }

}
