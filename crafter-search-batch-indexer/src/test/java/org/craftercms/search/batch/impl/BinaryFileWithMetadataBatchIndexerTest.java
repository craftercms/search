package org.craftercms.search.batch.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.craftercms.search.service.SearchService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link BinaryFileWithMetadataBatchIndexer}.
 *
 * @author avasquez
 */
public class BinaryFileWithMetadataBatchIndexerTest {

    private static final String SITE_NAME = "test";
    private static final String METADATA_FILENAME = "metadata.xml";
    private static final String BINARY_FILENAME = "logo.jpg";
    private static final String DELETE_FILENAME = "oldlogo.jpg";

    private SearchService searchService;
    private BinaryFileWithMetadataBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        searchService = getSearchService();
        batchIndexer = getBatchIndexer(searchService);
    }

    @Test
    public void testProcess() throws Exception {
        String rootFolder = getRootFolder();
        String indexId = SITE_NAME + "-default";
        File binaryFile = new File(rootFolder, BINARY_FILENAME);
        List<String> updatedFiles = Collections.singletonList(METADATA_FILENAME);
        List<String> deletedFiles = Collections.singletonList(DELETE_FILENAME);

        int updated = batchIndexer.updateIndex(indexId, SITE_NAME, rootFolder, updatedFiles, false);

        assertEquals(1, updated);
        verify(searchService).updateFile(indexId, SITE_NAME, BINARY_FILENAME, binaryFile, getExpectedMetadata());

        updated = batchIndexer.updateIndex(indexId, SITE_NAME, rootFolder, deletedFiles, true);

        assertEquals(1, updated);
        verify(searchService).delete(indexId, SITE_NAME, DELETE_FILENAME);
    }

    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    protected BinaryFileWithMetadataBatchIndexer getBatchIndexer(SearchService searchService) throws Exception {
        BinaryFileWithMetadataBatchIndexer batchIndexer = new BinaryFileWithMetadataBatchIndexer();
        batchIndexer.setMetadataPathPatterns(Collections.singletonList(".*\\.xml$"));
        batchIndexer.setBinaryPathPatterns(Collections.singletonList(".*\\.jpg$"));
        batchIndexer.setExcludeMetadataProperties(Collections.singletonList("objectId"));
        batchIndexer.setReferenceXPaths(Collections.singletonList("//attachment"));
        batchIndexer.setSearchService(searchService);

        return batchIndexer;
    }

    protected String getRootFolder() throws IOException {
        return new ClassPathResource("/docs").getFile().getAbsolutePath();
    }

    protected Map<String, List<String>> getExpectedMetadata() {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        metadata.add("fileName", "metadata.xml");
        metadata.add("attachmentText", "Logo");
        metadata.add("attachment", "logo.jpg");

        return metadata;
    }

}
