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

package org.craftercms.search.batch.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.CachingOptions;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.core.service.Tree;
import org.craftercms.core.xml.mergers.DescriptorMergeStrategy;
import org.craftercms.core.xml.mergers.DescriptorMergeStrategyResolver;
import org.craftercms.core.xml.mergers.MergeableDescriptor;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

/**
 * Implementation of {@link BatchIndexer} that handles descriptor inheritance during indexing
 *
 * @author joseross
 * @since 3.1.4
 */
public class MergeStrategyAwareBatchIndexer implements BatchIndexer {

    private static final Logger logger = LoggerFactory.getLogger(MergeStrategyAwareBatchIndexer.class);

    /**
     * The name of the level descriptor file
     */
    protected String levelDescriptorName;

    /**
     * The merge strategy resolver
     */
    protected DescriptorMergeStrategyResolver mergeStrategyResolver;

    public MergeStrategyAwareBatchIndexer(final String levelDescriptorName,
                                          final DescriptorMergeStrategyResolver mergeStrategyResolver) {
        this.levelDescriptorName = levelDescriptorName;
        this.mergeStrategyResolver = mergeStrategyResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateIndex(final String indexId, final String siteName, final ContentStoreService contentStoreService,
                            final Context context, final UpdateSet updateSet, final UpdateStatus updateStatus)
        throws BatchIndexingException {

        if (!context.isMergingOn()) {
            logger.debug("Merging is disabled, skipping inheriting descriptors");
            return;
        }

        List<String> changedPaths =
            (List<String>) ListUtils.union(updateSet.getUpdatePaths(), updateSet.getDeletePaths());
        List<String> levelDescriptors = changedPaths.stream()
            .filter(path -> path.endsWith(levelDescriptorName))
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(levelDescriptors)) {
            logger.debug("No level descriptors found in the update set files");
            return;
        }

        levelDescriptors.forEach(levelDescriptor -> {
            StopWatch watch = new StopWatch(levelDescriptor);

            logger.debug("Looking for descriptors that inherit from {}", levelDescriptor);
            watch.start("finding all children descriptors");
            Tree parentTree = contentStoreService.getTree(context, FilenameUtils.getPath(levelDescriptor));
            watch.stop();

            watch.start("finding all inheriting descriptors");
            findMergedDescriptors(context, levelDescriptor, parentTree, updateSet);
            watch.stop();

            if(logger.isTraceEnabled()) {
                logger.trace(watch.prettyPrint());
            }
        });

    }

    /**
     * Looks into all possible inheriting descriptors to add them to the update set
     * @param context the site context
     * @param levelDescriptor the originally changed level descriptor
     * @param item the child item to look into
     * @param updateSet the update set
     */
    protected void findMergedDescriptors(Context context, String levelDescriptor, Item item, UpdateSet updateSet) {
        if (item.isFolder()) {
            // keep looking recursively
            ((Tree) item).getChildren().forEach(child ->
                findMergedDescriptors(context, levelDescriptor, child, updateSet));
        } else {
            if (StringUtils.equals(levelDescriptor, item.getDescriptorUrl())) {
                // if it's the original level descriptor file just skip it
                return;
            }

            DescriptorMergeStrategy mergeStrategy =
                mergeStrategyResolver.getStrategy(item.getDescriptorUrl(), item.getDescriptorDom());

            logger.debug("Found merge strategy '{}' for descriptor at {}", mergeStrategy, item);

            List<MergeableDescriptor> mergeableDescriptors = mergeStrategy.getDescriptors(context,
                CachingOptions.CACHE_OFF_CACHING_OPTIONS, item.getDescriptorUrl(), item.getDescriptorDom());

            if (CollectionUtils.isEmpty(mergeableDescriptors)) {
                logger.debug("No mergeable descriptors found for item {}", item);
                return;
            }

            Optional<MergeableDescriptor> originalLevelDescriptor = mergeableDescriptors.stream()
                .filter(descriptor -> StringUtils.equals(descriptor.getUrl(), levelDescriptor))
                .findFirst();

            if (originalLevelDescriptor.isPresent()) {
                logger.debug("Adding new descriptor for indexing {}", item);
                updateSet.getUpdatePaths().add(item.getDescriptorUrl());
            }

        }
    }

}
