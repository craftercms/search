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

import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.craftercms.search.service.FieldValueConverter;

/**
 * Based on the suffix of a field, picks an actual {@link FieldValueConverter} from a set of suffix -> converter
 * mappings. For example, if a field ends with _dt, this converter can delegate to a {@link DateTimeConverter} to
 * convert the field.
 *
 * @author avasquez
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
