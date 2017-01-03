package org.craftercms.search.batch.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.craftercms.search.batch.IndexingStatus;
import org.craftercms.search.service.SearchService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link BinaryFileBatchIndexer}.
 *
 * @author avasquez
 */
public class BinaryFileBatchIndexerTest {

    private static final String SITE_NAME = "test";
    private static final String SUPPORTED_FILENAME = "document.pdf";
    private static final String NON_SUPPORTED_FILENAME = "image.jpg";

    private SearchService searchService;
    private BinaryFileBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        searchService = getSearchService();
        batchIndexer = getBatchIndexer(searchService);
    }

    @Test
    public void testProcess() throws Exception {
        String rootFolder = getRootFolder();
        String indexId = SITE_NAME + "-default";
        File supportedFile = new File(rootFolder, SUPPORTED_FILENAME);
        List<String> updatedFiles = Collections.singletonList(SUPPORTED_FILENAME);
        List<String> deletedFiles = Collections.singletonList(NON_SUPPORTED_FILENAME);
        IndexingStatus status = new IndexingStatus();

        batchIndexer.updateIndex(indexId, SITE_NAME, rootFolder, updatedFiles, false, status);

        assertEquals(1, status.getAttemptedUpdatesAndDeletes());
        verify(searchService).updateFile(indexId, SITE_NAME, SUPPORTED_FILENAME, supportedFile);

        status = new IndexingStatus();

        batchIndexer.updateIndex(indexId, SITE_NAME, rootFolder, deletedFiles, true, status);

        assertEquals(0, status.getAttemptedUpdatesAndDeletes());
        verify(searchService, never()).delete(indexId, SITE_NAME, NON_SUPPORTED_FILENAME);
    }

    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    protected BinaryFileBatchIndexer getBatchIndexer(SearchService searchService) throws Exception {
        BinaryFileBatchIndexer batchIndexer = new BinaryFileBatchIndexer();
        batchIndexer.setSupportedMimeTypes(Collections.singletonList("application/pdf"));
        batchIndexer.setSearchService(searchService);

        return batchIndexer;
    }

    protected String getRootFolder() throws IOException {
        return new ClassPathResource("/docs").getFile().getAbsolutePath();
    }

}
