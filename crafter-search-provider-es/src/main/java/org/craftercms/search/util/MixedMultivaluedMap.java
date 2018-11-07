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

package org.craftercms.search.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link Map} that can hold a single value or a list for a given key.
 * @author joseross
 */
public class MixedMultivaluedMap implements Map<String, Object> {

    protected Map<String, Object> values = new HashMap<>();

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return values.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return values.containsValue(value);
    }

    @Override
    public Object get(final Object key) {
        return values.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object put(final String key, final Object value) {
        if(values.containsKey(key)) {
            Object v = values.get(key);
            if(v instanceof List) {
                List<Object> original = new LinkedList<>((List<Object>)v);
                ((List)v).add(value);
                return original;
            } else {
                List<Object> list = new LinkedList<>();
                list.add(v);
                list.add(value);
                return values.put(key, list);
            }
        } else {
            return values.put(key, value);
        }
    }

    @Override
    public Object remove(final Object key) {
        return values.remove(key);
    }

    @Override
    public void putAll(final Map<? extends String, ?> m) {
        values.putAll(m);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public Collection<Object> values() {
        return values.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

}