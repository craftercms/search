package org.craftercms.search.batch.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.craftercms.core.service.Content;
import org.craftercms.search.batch.IndexingStatus;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.QueryFactory;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.utils.SearchResultUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BinaryFileWithMetadataBatchIndexer}.
 *
 * @author avasquez
 */
public class BinaryFileWithMetadataBatchIndexerTest extends BatchIndexerTestBase {

    private static final String SITE_NAME = "test";
    private static final String INDEX_ID = SITE_NAME;
    private static final String METADATA_XML_FILENAME = "metadata.xml";
    private static final String METADATA_WITH_REMOVED_BINARIES_XML_FILENAME = "metadata-with-removed-binaries.xml";
    private static final String BINARY_FILENAME1 = "crafter-wp-7-reasons.pdf";
    private static final String BINARY_FILENAME2 = "crafter-wp-wem-v2.pdf";
    private static final String BINARY_FILENAME3 = "notes.txt";

    private QueryFactory<Query> queryFactory;
    private BinaryFileWithMetadataBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        queryFactory = getQueryFactory();
        batchIndexer = getBatchIndexer();
    }

    @Test
    public void testUpdateMetadata() {
        List<String> paths = Collections.singletonList(METADATA_XML_FILENAME);
        IndexingStatus status = new IndexingStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, paths, false, status);

        assertEquals(3, status.getAttemptedUpdatesAndDeletes());
        assertTrue(status.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        assertTrue(status.getSuccessfulUpdates().contains(BINARY_FILENAME2));
        assertTrue(status.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).updateFile(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadata()));
        verify(searchService).updateFile(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2), any(Content.class), eq(getExpectedMetadata()));
        verify(searchService).updateFile(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class), eq(getExpectedMetadata()));
    }

    @Test
    public void testUpdateMetadataWithRemovedBinaries() {
        setupBinariesSearchResults();

        List<String> paths = Collections.singletonList(METADATA_WITH_REMOVED_BINARIES_XML_FILENAME);
        IndexingStatus status = new IndexingStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, paths, false, status);

        assertEquals(3, status.getAttemptedUpdatesAndDeletes());
        assertTrue(status.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        assertTrue(status.getSuccessfulDeletes().contains(BINARY_FILENAME2));
        assertTrue(status.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).updateFile(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadataWithRemovedBinaries()));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2));
        verify(searchService).updateFile(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class));
    }

    @Test
    public void testUpdateBinary() throws Exception {
        setupMetadataSearchResult();

        List<String> paths = Collections.singletonList(BINARY_FILENAME1);
        IndexingStatus status = new IndexingStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, paths, false, status);

        assertEquals(1, status.getAttemptedUpdatesAndDeletes());
        assertTrue(status.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        verify(searchService).updateFile(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadata()));
    }

    @Test
    public void testDeleteBinary() throws Exception {
        List<String> paths = Collections.singletonList(BINARY_FILENAME1);
        IndexingStatus status = new IndexingStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, paths, true, status);

        assertEquals(1, status.getAttemptedUpdatesAndDeletes());
        assertTrue(status.getSuccessfulDeletes().contains(BINARY_FILENAME1));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1));
    }

    @Test
    public void testDeleteMetadata() throws Exception {
        setupBinariesSearchResults();

        List<String> paths = Collections.singletonList(METADATA_XML_FILENAME);
        IndexingStatus status = new IndexingStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, paths, true, status);

        assertEquals(3, status.getAttemptedUpdatesAndDeletes());
        assertTrue(status.getSuccessfulDeletes().contains(BINARY_FILENAME1));
        assertTrue(status.getSuccessfulDeletes().contains(BINARY_FILENAME2));
        assertTrue(status.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2));
        verify(searchService).updateFile(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class));
    }

    @SuppressWarnings("unchecked")
    protected QueryFactory<Query> getQueryFactory() throws Exception {
        QueryFactory<Query> queryFactory = mock(QueryFactory.class);
        when(queryFactory.createQuery()).thenReturn(mock(Query.class));

        return queryFactory;
    }

    @SuppressWarnings("unchecked")
    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    @SuppressWarnings("unchecked")
    protected void setupBinariesSearchResults() {
        Map<String, String> binary1Doc = Collections.singletonMap("localId", BINARY_FILENAME1);
        Map<String, String> binary2Doc = Collections.singletonMap("localId", BINARY_FILENAME2);
        Map<String, String> binary3Doc = Collections.singletonMap("localId", BINARY_FILENAME3);

        List<Object> documents = Arrays.<Object>asList(binary1Doc, binary2Doc, binary3Doc);
        Map<String, Object> response = Collections.<String, Object>singletonMap(SearchResultUtils.DOCUMENTS_KEY, documents);
        Map<String, Object> result = Collections.<String, Object>singletonMap(SearchResultUtils.RESPONSE_KEY, response);

        when(searchService.search(anyString(), any(Query.class))).thenReturn(result);
    }

    @SuppressWarnings("unchecked")
    protected void setupMetadataSearchResult() {
        Map<String, String> doc = getExpectedMetadata().toSingleValueMap();
        List<Object> documents = Collections.<Object>singletonList(doc);
        Map<String, Object> response = Collections.<String, Object>singletonMap(SearchResultUtils.DOCUMENTS_KEY, documents);
        Map<String, Object> result = Collections.<String, Object>singletonMap(SearchResultUtils.RESPONSE_KEY, response);

        when(searchService.search(anyString(), any(Query.class))).thenReturn(result);
    }

    protected BinaryFileWithMetadataBatchIndexer getBatchIndexer() throws Exception {
        BinaryFileWithMetadataBatchIndexer batchIndexer = new BinaryFileWithMetadataBatchIndexer();
        batchIndexer.setMetadataPathPatterns(Collections.singletonList(".*metadata.*\\.xml$"));
        batchIndexer.setBinaryPathPatterns(Arrays.asList(".*\\.pdf$", ".*\\.txt$"));
        batchIndexer.setChildBinaryPathPatterns(Collections.singletonList(".*\\.pdf$"));
        batchIndexer.setExcludeMetadataProperties(Collections.singletonList("files"));
        batchIndexer.setReferenceXPaths(Collections.singletonList("//file"));
        batchIndexer.setSearchService(searchService);
        batchIndexer.setQueryFactory(queryFactory);

        return batchIndexer;
    }

    protected MultiValueMap<String, String> getExpectedMetadata() {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        metadata.set("copyright.company", "Crafter Software");
        metadata.set("copyright.text", "All rights reserved");
        metadata.set("copyright.year", "2017");
        metadata.set("metadataPath", METADATA_XML_FILENAME);

        return metadata;
    }

    protected MultiValueMap<String, String> getExpectedMetadataWithRemovedBinaries() {
        MultiValueMap<String, String> metadata = getExpectedMetadata();
        metadata.set("metadataPath", METADATA_WITH_REMOVED_BINARIES_XML_FILENAME);

        return metadata;
    }

}
