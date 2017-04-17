package org.craftercms.search.batch.impl;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.craftercms.search.batch.IndexingStatus;
import org.craftercms.search.service.SearchService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link BinaryFileWithMetadataBatchIndexer}.
 *
 * @author avasquez
 */
public class BinaryFileWithMetadataBatchIndexerTest extends BatchIndexerTestBase {

    private static final String SITE_NAME = "test";
    private static final String METADATA_FILENAME = "metadata.xml";
    private static final String BINARY_FILENAME = "crafter-wp-7-reasons.pdf";
    private static final String DELETE_FILENAME = BINARY_FILENAME;

    private BinaryFileWithMetadataBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        batchIndexer = getBatchIndexer(searchService);
    }

    @Test
    public void testProcess() throws Exception {
        String indexId = SITE_NAME;
        List<String> updatedFiles = Collections.singletonList(METADATA_FILENAME);
        List<String> deletedFiles = Collections.singletonList(DELETE_FILENAME);
        IndexingStatus status = new IndexingStatus();

        batchIndexer.updateIndex(indexId, SITE_NAME, contentStoreService, context, updatedFiles, false, status);

        assertEquals(1, status.getAttemptedUpdatesAndDeletes());
        assertEquals(BINARY_FILENAME, status.getSuccessfulUpdates().get(0));
        verify(searchService).updateContent(eq(indexId), eq(SITE_NAME), eq(BINARY_FILENAME), any(InputStream.class),
                                            eq(getExpectedMetadata()));

        status = new IndexingStatus();

        batchIndexer.updateIndex(indexId, SITE_NAME, contentStoreService, context, deletedFiles, true, status);

        assertEquals(1, status.getAttemptedUpdatesAndDeletes());
        assertEquals(DELETE_FILENAME, status.getSuccessfulDeletes().get(0));
        verify(searchService).delete(indexId, SITE_NAME, DELETE_FILENAME);
    }

    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    protected BinaryFileWithMetadataBatchIndexer getBatchIndexer(SearchService searchService) throws Exception {
        BinaryFileWithMetadataBatchIndexer batchIndexer = new BinaryFileWithMetadataBatchIndexer();
        batchIndexer.setMetadataPathPatterns(Collections.singletonList(".*\\.xml$"));
        batchIndexer.setBinaryPathPatterns(Collections.singletonList(".*\\.pdf$"));
        batchIndexer.setExcludeMetadataProperties(Collections.singletonList("objectId"));
        batchIndexer.setReferenceXPaths(Collections.singletonList("//attachment"));
        batchIndexer.setSearchService(searchService);

        return batchIndexer;
    }

    protected Map<String, List<String>> getExpectedMetadata() {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        metadata.add("fileName", "metadata.xml");
        metadata.add("attachmentText", "Crafter White Paper 7 Reasons");
        metadata.add("attachment", BINARY_FILENAME);

        return metadata;
    }

}
