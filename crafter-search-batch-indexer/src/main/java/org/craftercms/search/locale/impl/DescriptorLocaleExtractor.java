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
package org.craftercms.search.locale.impl;

import org.craftercms.commons.locale.LocaleUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.search.locale.LocaleExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.ConstructorProperties;
import java.util.Locale;

/**
 * Implementation of {@link LocaleExtractor} for XML descriptors
 *
 * @author joseross
 * @since 4.0.0
 */
public class DescriptorLocaleExtractor implements LocaleExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DescriptorLocaleExtractor.class);

    /**
     * The content store service
     */
    protected ContentStoreService contentStoreService;

    /**
     * The XPath to extract the locale from the descriptor
     */
    protected String localeXPath;

    @ConstructorProperties({"contentStoreService", "localeXPath"})
    public DescriptorLocaleExtractor(ContentStoreService contentStoreService, String localeXPath) {
        this.contentStoreService = contentStoreService;
        this.localeXPath = localeXPath;
    }

    @Override
    public Locale extract(Context context, String path) {
        Item item = contentStoreService.getItem(context, path);
        String localeValue = item.queryDescriptorValue(localeXPath);
        Locale locale = LocaleUtils.parseLocale(localeValue);
        logger.debug("Resolved locale {} for item {}", locale, path);
        return locale;
    }

}
