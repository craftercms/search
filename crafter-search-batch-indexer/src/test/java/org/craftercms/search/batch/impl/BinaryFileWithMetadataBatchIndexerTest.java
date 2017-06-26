package org.craftercms.search.batch.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.utils.SearchResultUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
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
    private static final String METADATA_XML_FILENAME1 = "crafter-wp-7-reasons-metadata.xml";
    private static final String METADATA_XML_FILENAME2 = "crafter-wp-wem-v2-metadata.xml";
    private static final String NON_METADATA_XML_FILENAME = "test.xml";
    private static final String BINARY_FILENAME1 = "crafter-wp-7-reasons.pdf";
    private static final String BINARY_FILENAME2 = "crafter-wp-wem-v2.pdf";

    private BinaryFileWithMetadataBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        batchIndexer = getBatchIndexer();
    }

    @Test
    public void testUpdateOfBinaryAndMetadata() {
        UpdateSet updateSet = new UpdateSet(Arrays.asList(METADATA_XML_FILENAME1, BINARY_FILENAME1), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(BINARY_FILENAME1, updateStatus.getSuccessfulUpdates().get(0));
        verify(searchService).updateContent(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(InputStream.class), eq(getExpectedMetadata1()));
    }

    @Test
    public void testUpdateOfBinaryAndNoMetadata() {
        UpdateSet updateSet = new UpdateSet(Collections.singletonList(BINARY_FILENAME1), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(BINARY_FILENAME1, updateStatus.getSuccessfulUpdates().get(0));
        verify(searchService).updateContent(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(InputStream.class));
    }

    @Test
    public void testUpdateOfMetadataAndNoBinary() {
        UpdateSet updateSet = new UpdateSet(Collections.singletonList(METADATA_XML_FILENAME1), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(BINARY_FILENAME1, updateStatus.getSuccessfulUpdates().get(0));
        verify(searchService).updateContent(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(InputStream.class), eq(getExpectedMetadata1()));
    }

    @Test
    public void testUpdateOfBinaryExistingBinaryInIndex() {
        setupBinary2SearchResult();

        UpdateSet updateSet = new UpdateSet(Collections.singletonList(BINARY_FILENAME2), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(BINARY_FILENAME2, updateStatus.getSuccessfulUpdates().get(0));
        verify(searchService).updateContent(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2), any(InputStream.class), eq(getExpectedMetadata2()));
    }

    @Test
    public void testUpdateNonMetadataXml() {
        setupBinary2SearchResult();

        UpdateSet updateSet = new UpdateSet(Collections.singletonList(NON_METADATA_XML_FILENAME), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(0, updateStatus.getAttemptedUpdatesAndDeletes());
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testDeleteBinary() {
        UpdateSet updateSet = new UpdateSet(Collections.emptyList(), Collections.singletonList(BINARY_FILENAME1));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(BINARY_FILENAME1, updateStatus.getSuccessfulDeletes().get(0));
        verify(searchService).delete(INDEX_ID, SITE_NAME, BINARY_FILENAME1);
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void testDeleteBinaryFromMetadata() {
        UpdateSet updateSet = new UpdateSet(Collections.emptyList(), Collections.singletonList(METADATA_XML_FILENAME1));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(BINARY_FILENAME1, updateStatus.getSuccessfulDeletes().get(0));
        verify(searchService).delete(INDEX_ID, SITE_NAME, BINARY_FILENAME1);
    }

    @SuppressWarnings("unchecked")
    protected SearchService getSearchService() throws Exception {
        SearchService searchService = mock(SearchService.class);
        when(searchService.createQuery()).thenReturn(mock(Query.class));

        return searchService;
    }

    @SuppressWarnings("unchecked")
    protected void setupBinary2SearchResult() {
        Map<String, String> document = getExpectedMetadata2().toSingleValueMap();
        List<Object> documents = Collections.singletonList(document);
        Map<String, Object> response = Collections.singletonMap(SearchResultUtils.DOCUMENTS_KEY, documents);
        Map<String, Object> result = Collections.singletonMap(SearchResultUtils.RESPONSE_KEY, response);

        when(searchService.search(anyString(), any(Query.class))).thenReturn(result);
    }

    protected BinaryFileWithMetadataBatchIndexer getBatchIndexer() throws Exception {
        BinaryFileWithMetadataBatchIndexer batchIndexer = new BinaryFileWithMetadataBatchIndexer();
        batchIndexer.setMetadataPathPatterns(Collections.singletonList(".*metadata.*\\.xml$"));
        batchIndexer.setBinaryPathPatterns(Collections.singletonList(".*\\.pdf$"));
        batchIndexer.setExcludeMetadataProperties(Collections.singletonList("objectId"));
        batchIndexer.setReferenceXPaths(Collections.singletonList("//attachment"));

        return batchIndexer;
    }

    protected MultiValueMap<String, String> getExpectedMetadata1() {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        metadata.add("fileName", METADATA_XML_FILENAME1);
        metadata.add("attachmentText", "Crafter White Paper 7 Reasons");
        metadata.add("attachment", BINARY_FILENAME1);
        metadata.add("metadataPath", METADATA_XML_FILENAME1);

        return metadata;
    }

    protected MultiValueMap<String, String> getExpectedMetadata2() {
        MultiValueMap<String, String> metadata = new LinkedMultiValueMap<>();
        metadata.add("fileName", METADATA_XML_FILENAME2);
        metadata.add("attachmentText", "Building and Optimizing Multi-Channel Web Experiences");
        metadata.add("attachment", BINARY_FILENAME2);
        metadata.add("metadataPath", METADATA_XML_FILENAME2);

        return metadata;
    }

}
