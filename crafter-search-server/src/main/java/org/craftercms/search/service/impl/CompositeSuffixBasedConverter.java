package org.craftercms.search.service.impl;

import java.util.Map;

import org.apache.commons.collections.MapUtils;

/**
 * Created by alfonsovasquez on 5/2/16.
 */
public class CompositeSuffixBasedConverter implements FieldValueConverter {

    private Map<String, FieldValueConverter> converterMappings;
    private FieldValueConverter defaultConverter;

    public void setConverterMappings(Map<String, FieldValueConverter> converterMappings) {
        this.converterMappings = converterMappings;
    }

    public void setDefaultConverter(FieldValueConverter defaultConverter) {
        this.defaultConverter = defaultConverter;
    }

    @Override
    public Object convert(String name, String value) {
        if (MapUtils.isNotEmpty(converterMappings)) {
            for (Map.Entry<String, FieldValueConverter> entry : converterMappings.entrySet()) {
                if (name.endsWith(entry.getKey())) {
                    return entry.getValue().convert(name, value);
                }
            }
        }

        if (defaultConverter != null) {
            return defaultConverter.convert(name, value);
        } else {
            return value;
        }
    }

}
