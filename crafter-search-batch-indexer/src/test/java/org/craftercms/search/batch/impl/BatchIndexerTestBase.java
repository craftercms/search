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
import org.craftercms.core.store.impl.filesystem.FileSystemFile;
import org.craftercms.search.service.SearchService;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.springframework.core.io.ClassPathResource;

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

    protected Item findItem(String path, Context context, ItemProcessor processor) throws DocumentException {
        File file = new File(rootFolder.getFile(), path);
        if (file.exists()){
            SAXReader reader = new SAXReader();
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
            return file;
        } else {
            return null;
        }
    }

}
