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

package org.craftercms.core.processors.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.exception.ItemProcessingException;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.service.CachingOptions;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.core.xml.mergers.DescriptorMergeStrategy;
import org.craftercms.core.xml.mergers.DescriptorMergeStrategyResolver;
import org.craftercms.core.xml.mergers.MergeableDescriptor;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * Implementation of {@link ItemProcessor} that adds a field with the paths of the descriptors that are being inherited
 *  *
 * @author joseross
 * @since 3.1.4
 */
public class InheritedDescriptorsItemProcessor implements ItemProcessor {

    /**
     * The name of the field to add
     */
    protected String inheritsFromElementName;

    /**
     * The merge strategy resolver
     */
    protected DescriptorMergeStrategyResolver mergeStrategyResolver;

    public InheritedDescriptorsItemProcessor(final String inheritsFromElementName,
                                             final DescriptorMergeStrategyResolver mergeStrategyResolver) {
        this.inheritsFromElementName = inheritsFromElementName;
        this.mergeStrategyResolver = mergeStrategyResolver;
    }

    @Override
    public Item process(final Context context, final CachingOptions cachingOptions, final Item item)
        throws ItemProcessingException {
        if (item.getDescriptorDom() != null) {
            DescriptorMergeStrategy mergeStrategy = mergeStrategyResolver.
                getStrategy(item.getDescriptorUrl(), item.getDescriptorDom());
            if (mergeStrategy != null) {
                List<MergeableDescriptor> inheritedDescriptors = mergeStrategy.
                    getDescriptors(context, cachingOptions, item.getDescriptorUrl(), item.getDescriptorDom());
                if (CollectionUtils.isNotEmpty(inheritedDescriptors)) {
                    inheritedDescriptors.stream()
                        .filter(descriptor -> !StringUtils.equals(descriptor.getUrl(), item.getDescriptorUrl()))
                        .forEach(descriptor -> {
                            Element inheritedFromElement = DocumentHelper.createElement(inheritsFromElementName);
                            inheritedFromElement.setText(descriptor.getUrl());
                            item.getDescriptorDom().getRootElement().add(inheritedFromElement);
                        });
                }
            }
        }
        return item;
    }

}
