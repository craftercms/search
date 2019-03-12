/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.elasticsearch.impl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.collections.MapUtils;
import org.craftercms.search.elasticsearch.jackson.MixedMultivaluedMap;
import org.craftercms.search.exception.SearchException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of {@link org.craftercms.search.elasticsearch.DocumentBuilder} for ElasticSearch
 * @author joseross
 */
public class ElasticSearchDocumentBuilder extends AbstractDocumentBuilder<Map<String, Object>> {

    protected ObjectMapper objectMapper;

    /**
     * Mapping of fields that require a copy before indexing
     */
    protected Map<String, String> copyFields;

    /**
     * Pattern for fields containing HTML markup
     */
    protected String markupFieldPattern;

    @Required
    public void setObjectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setCopyFields(final Map<String, String> copyFields) {
        this.copyFields = copyFields;
    }

    @Required
    public void setMarkupFieldPattern(final String markupFieldPattern) {
        this.markupFieldPattern = markupFieldPattern;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> build(final String site, final String id, final String xml,
                                     MultiValueMap additionalFields) {
        try {
            Map<String, Object> map = objectMapper.readValue(xml, MixedMultivaluedMap.class);
            addFields(map, site, id, additionalFields);
            if(MapUtils.isNotEmpty(copyFields)) {
                addCopyFields(map);
            }
            updateFields(map);
            return map;
        } catch (Exception e) {
            throw new SearchException("Error building json for document " + id, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFields(Map<String, Object> map, String site, String id, Map<String, List<String>> additionalFields) {
        String now = DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.now(ZoneId.of("UTC")));

        map.put(siteFieldName, site);
        map.put(localIdFieldName, id);
        map.put(publishingDateFieldName, now);
        map.put(publishingDateAltFieldName, now);

        if(MapUtils.isNotEmpty(additionalFields)) {
            additionalFields.forEach((key, values) -> map.put(key, values.size() == 1? values.get(0) : values));
        }
    }

    /**
     * Iterates recursively the given map to apply updates on single value nodes
     * @param map the map to iterate
     * @param consumer the update to apply
     */
    @SuppressWarnings("unchecked")
    protected void updateMap(Map<String, Object> map,
                             BiConsumer<Map.Entry<String, Object>, Map<String, Object>> consumer) {
        Map<String, Object> temp = new HashMap<>();
        map.entrySet().forEach(entry -> {
            if(entry.getValue() instanceof Map) {
                updateMap((Map<String, Object>)entry.getValue(), consumer);
            } else if(entry.getValue() instanceof List) {
                List list = (List) entry.getValue();
                list.forEach(item -> {
                    if(item instanceof Map) {
                        updateMap((Map<String, Object>) item, consumer);
                    }
                });
            } else {
                consumer.accept(entry, temp);
            }
        });
        if(MapUtils.isNotEmpty(temp)) {
            map.putAll(temp);
        }
    }

    /**
     * Creates copies of all fields according to the mapping
     * @param map the map to update
     */
    protected void addCopyFields(Map<String, Object> map) {
        updateMap(map, (entry, temp) -> {
            copyFields.forEach((pattern, name) -> {
                if(entry.getKey().matches(pattern)) {
                    temp.put(entry.getKey() + name, entry.getValue());
                }
            });
        });
    }

    /**
     * Updates fields according to the configuration
     * @param map the map to update
     */
    protected void updateFields(Map<String, Object> map) {
        updateMap(map, (entry, temp) -> {
            if(entry.getKey().matches(markupFieldPattern)) {
                temp.put(entry.getKey(), Jsoup.clean(entry.getValue().toString(), Whitelist.none()));
            }
        });
    }

}