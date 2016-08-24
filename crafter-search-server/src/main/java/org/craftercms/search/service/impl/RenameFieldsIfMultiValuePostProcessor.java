package org.craftercms.search.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.craftercms.search.service.SolrDocumentPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_ID_FIELD_NAME;

/**
 * Created by alfonsovasquez on 19/8/16.
 */
public class RenameFieldsIfMultiValuePostProcessor implements SolrDocumentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RenameFieldsIfMultiValuePostProcessor.class);

    protected String idFieldName;
    protected Map<String, String> singleToMultiValueSuffixMappings;

    public RenameFieldsIfMultiValuePostProcessor() {
        idFieldName = DEFAULT_ID_FIELD_NAME;
    }

    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    @Required
    public void setSingleToMultiValueSuffixMappings(Map<String, String> singleToMultiValueSuffixMappings) {
        this.singleToMultiValueSuffixMappings = singleToMultiValueSuffixMappings;
    }

    @Override
    public void postProcess(SolrInputDocument solrDoc) {
        String id = solrDoc.getFieldValue(idFieldName).toString();
        List<SolrInputField> renamedFields = new ArrayList<>();

        // Remap single value fields to multi value fields if they have more than one value
        for (Iterator<SolrInputField> iter = solrDoc.iterator(); iter.hasNext();) {
            SolrInputField field = iter.next();
            SolrInputField renamedField = renameFieldIfMultiValue(id, field);

            if (renamedField != null) {
                renamedFields.add(renamedField);

                iter.remove();
            }
        }

        for (SolrInputField renamedField : renamedFields) {
            solrDoc.put(renamedField.getName(), renamedField);
        }

        // Do the same for child docs
        if (solrDoc.hasChildDocuments()) {
            for (SolrInputDocument childDoc : solrDoc.getChildDocuments()) {
                postProcess(childDoc);
            }
        }
    }

    protected SolrInputField renameFieldIfMultiValue(String docId, SolrInputField field) {
        if (field.getValue() instanceof Collection) {
            String fieldName = field.getName();

            for (Map.Entry<String, String> entry : singleToMultiValueSuffixMappings.entrySet()) {
                String singleValueSuffix = entry.getKey();
                if (fieldName.endsWith(singleValueSuffix)) {
                    String multiValueSuffix = entry.getValue();
                    String fieldWithoutSuffix = StringUtils.substringBefore(fieldName, singleValueSuffix);
                    String newFieldName = fieldWithoutSuffix + multiValueSuffix;

                    logger.warn("Field '{}' is declared as single value, but multiple values where provided in {}. " +
                                "Renaming to multi value field '{}'", fieldName, docId, newFieldName);

                    SolrInputField newField = new SolrInputField(newFieldName);
                    newField.setValue(field.getValue(), field.getBoost());

                    return newField;
                }
            }
        }

        return null;
    }

}
