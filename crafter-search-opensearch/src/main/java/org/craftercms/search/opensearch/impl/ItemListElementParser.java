/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.search.opensearch.impl;

import org.craftercms.search.commons.service.ElementParser;
import org.craftercms.search.commons.service.ElementParserService;
import org.dom4j.Element;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

/**
 * Implementation of {@link ElementParser} that turns single objects into lists if the 'item-list' attribute is present
 *
 * @author joseross
 * @since 4.0.0
 */
public class ItemListElementParser implements ElementParser<Map<String, Object>> {

    public static final String DEFAULT_ITEM_LIST_ATTRIBUTE = "item-list";

    protected String attributeName = DEFAULT_ITEM_LIST_ATTRIBUTE;

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public boolean parse(Element element, String fieldName, String parentFieldName, Map<String, Object> doc,
                         ElementParserService<Map<String, Object>> parserService) {
        var attribute = element.attribute(attributeName);
        // If the attribute is present & the value is true
        if (attribute == null || !Boolean.parseBoolean(attribute.getText())) {
            return false;
        }
        var items = element.elements();
        // If there is a single item in the list
        if (items.size() == 1) {
            var itemElement = items.get(0);
            var itemElementName = itemElement.getName();

            // parse the item into a temporary map instead the main document
            var itemMap = new HashMap<String, Object>();
            parserService.parse(itemElement, parentFieldName, itemMap);

            // add the item as a singleton list in the main document
            doc.put(fieldName, singletonMap(itemElementName, singletonList(itemMap.get(itemElementName))));

            return true;
        }
        return false;
    }

}
