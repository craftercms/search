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
import java.util.List;
import java.util.Map;

import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.search.metadata.MetadataExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

/**
 * Base implementation of {@link MetadataExtractor}
 *
 * @author joseross
 * @since 3.1.1
 */
public abstract class AbstractMetadataExtractor implements MetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMetadataExtractor.class);

    /**
     * Pattern of files that should be included
     */
    protected List<String> includePatterns;

    public void setIncludePatterns(final List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> extract(final String path, final ContentStoreService contentStoreService,
                                  final Context context) {
        logger.debug("Start processing {}", path);

        Map<String, String> metadata = Collections.emptyMap();

        if ((CollectionUtils.isEmpty(includePatterns) || RegexUtils.matchesAny(path, includePatterns))
            && isCompatible(path, contentStoreService, context)) {
            logger.debug("Extracting metadata from {}", path);
            metadata = doExtract(path, contentStoreService, context);
        }

        logger.debug("Completed processing {}", path);

        return metadata;
    }

    /**
     * Checks if a given file should be processed by the current instance
     * @param path the path of the file to check
     * @param contentStoreService the content store service
     * @param context the current context
     * @return true if the file should be processed
     */
    protected abstract boolean isCompatible(String path, ContentStoreService contentStoreService, Context context);

    /**
     * Performs the actual metadata extraction
     * @param path the path of the file
     * @param contentStoreService the content store service
     * @param context the current context
     * @return the extracted metadata
     */
    protected abstract Map<String, String> doExtract(String path, ContentStoreService contentStoreService,
                                                     Context context);

}
