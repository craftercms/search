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

package org.craftercms.search.batch.impl;

import java.io.File;
import java.io.IOException;

import org.craftercms.core.exception.PathNotFoundException;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.service.CachingOptions;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.core.store.impl.filesystem.FileSystemContent;
import org.craftercms.core.store.impl.filesystem.FileSystemFile;
import org.craftercms.search.service.SearchService;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for {@link org.craftercms.search.batch.BatchIndexer} tests.
 *
 * @author avasquez
 */
public class BatchIndexerTestBase {

    protected FileSystemFile rootFolder;
    protected ContentStoreService contentStoreService;
    protected Context context;
    protected SearchService searchService;

    @Before
    public void setUp() throws Exception {
        rootFolder = getRootFolder();
        contentStoreService = getContentStoreService();
        context = getContext();
        searchService = getSearchService();
    }

    protected ContentStoreService getContentStoreService() {
        ContentStoreService storeService = mock(ContentStoreService.class);

        when(storeService.getItem(any(Context.class), any(CachingOptions.class), anyString(), any(ItemProcessor.class))).thenAnswer(
            invocationOnMock -> {
                Object[] args = invocationOnMock.getArguments();
                Context context = (Context)args[0];
                String path = (String)args[2];
                ItemProcessor processor = (ItemProcessor)args[3];
                Item item = findItem(path, context, processor);

                if (item != null) {
                    return item;
                } else {
                    throw new PathNotFoundException();
                }
            }
        );
        when(storeService.findItem(any(Context.class), any(CachingOptions.class), anyString(), any(ItemProcessor.class))).thenAnswer(
            invocationOnMock -> {
                Object[] args = invocationOnMock.getArguments();
                Context context = (Context)args[0];
                String path = (String)args[2];
                ItemProcessor processor = (ItemProcessor)args[3];

                return findItem(path, context, processor);
            }
        );
        when(storeService.getContent(any(Context.class), anyString())).thenAnswer(
            invocationOnMock -> {
                Object[] args = invocationOnMock.getArguments();
                String path = (String)args[1];
                Content content = findContent(path);

                if (content != null) {
                    return content;
                } else {
                    throw new PathNotFoundException();
                }
            }
        );
        when(storeService.findContent(any(Context.class), anyString())).thenAnswer(
            invocationOnMock -> {
                Object[] args = invocationOnMock.getArguments();
                String path = (String)args[1];

                return findContent(path);
            }
        );

        return storeService;
    }

    protected Context getContext() {
        return mock(Context.class);
    }

    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    protected FileSystemFile getRootFolder() throws IOException {
        return new FileSystemFile(new ClassPathResource("/docs").getFile());
    }

    protected Item findItem(String path, Context context, ItemProcessor processor) throws DocumentException, SAXException {
        File file = new File(rootFolder.getFile(), path);
        if (file.exists()){
            SAXReader reader = new SAXReader();

            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = reader.read(file);
            Item item = new Item();

            item.setDescriptorUrl(path);
            item.setDescriptorDom(document);

            if (processor != null) {
                item = processor.process(context, null, item);
            }

            return item;
        } else {
           return null;
        }
    }

    protected Content findContent(String path) {
        FileSystemFile file = new FileSystemFile(rootFolder, path);

        if (file.getFile().exists()) {
            return new FileSystemContent(file.getFile());
        } else {
            return null;
        }
    }

}
