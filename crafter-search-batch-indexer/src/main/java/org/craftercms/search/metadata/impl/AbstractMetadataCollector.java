/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.metadata.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.metadata.MetadataExtractor;

/**
 * Base implementation for classes that use a list of {@link MetadataExtractor}s to process items
 *
 * @author joseross
 * @since 3.1.1
 */
public abstract class AbstractMetadataCollector {

    /**
     * List of metadata extractors
     */
    protected List<MetadataExtractor> metadataExtractors;

    public void setMetadataExtractors(final List<MetadataExtractor> metadataExtractors) {
        this.metadataExtractors = metadataExtractors;
    }

    /**
     * Executes all metadata extractors on the given file and returns an aggregation of all results
     * @param path the path of the file
     * @param contentStoreService the content store service
     * @param context the current context
     * @return all extracted metadata
     */
    protected Map<String, String> collectMetadata(final String path, final ContentStoreService contentStoreService,
                                                  final Context context) {
        if (CollectionUtils.isEmpty(metadataExtractors)) {
            return Collections.emptyMap();
        } else {
            return metadataExtractors
                .stream()
                .map(metadataExtractor -> metadataExtractor.extract(path, contentStoreService, context))
                .reduce(new HashMap<>(), (map1, map2) -> {
                    map1.putAll(map2);
                    return map1;
                });
        }
    }

}
