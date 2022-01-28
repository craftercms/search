/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.search.commons.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.collections4.ListUtils.union;

/**
 * @author joseross
 * @since 4.0.0
 */
public abstract class MapUtils {

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mergeMaps(Map<String, Object> a, Map<String, Object> b) {
        if (org.apache.commons.collections.MapUtils.isEmpty(a)) {
            return b;
        }

        if (org.apache.commons.collections.MapUtils.isEmpty(b)) {
            return a;
        }

        var map = new TreeMap<>(a);
        b.forEach((key, value) -> map.merge(key, value, (oldValue, newValue) -> {
            if (oldValue instanceof Map && newValue instanceof Map) {
                return mergeMaps((Map<String, Object>) oldValue, (Map<String, Object>) newValue);
            } else if (oldValue instanceof Map || newValue instanceof Map) {
                // can't be merged, just return the original
                return oldValue;
            } else if (oldValue instanceof List && newValue instanceof List) {
                return union((List<Object>) oldValue, (List<Object>) newValue);
            } else if (oldValue instanceof List) {
                var list = new LinkedList<>((List<Object>) oldValue);
                list.add(newValue);
                return list;
            } else if (newValue instanceof List) {
                var list = new LinkedList<>((List<Object>) newValue);
                list.add(oldValue);
                return list;
            } else {
                // single properties are not merged, only overwritten
                return newValue;
            }
        }));

        return map;
    }

}
