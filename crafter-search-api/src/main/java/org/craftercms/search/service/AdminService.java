/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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
package org.craftercms.search.service;

import java.util.Map;

import org.craftercms.search.exception.SearchException;

/**
 * Created by alfonsovasquez on 2/3/17.
 */
public interface AdminService {

    enum IndexDeleteMode {
        ALL_DATA,
        ALL_DATA_AND_CONFIG;
    }

    void createIndex(String id) throws SearchException;

    Map<String, Object> getIndexInfo(String id) throws SearchException;

    void deleteIndex(String id, IndexDeleteMode mode) throws SearchException;

}
