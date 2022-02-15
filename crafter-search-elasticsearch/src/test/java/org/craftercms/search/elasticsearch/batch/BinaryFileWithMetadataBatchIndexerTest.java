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

package org.craftercms.search.elasticsearch.batch;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.craftercms.core.exception.PathNotFoundException;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.core.service.Content;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ElasticsearchBinaryFileWithMetadataBatchIndexer}.
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

    private ElasticsearchBinaryFileWithMetadataBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        when(contentStoreService.getContent(any(Context.class), anyString())).thenAnswer(
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

        batchIndexer = getBatchIndexer();
    }

    @Test
    public void testUpdateMetadata() {
        UpdateSet updateSet = new UpdateSet(Collections.singletonList(METADATA_XML_FILENAME), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(3, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME2));
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).indexBinary(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadata()));
        verify(searchService).indexBinary(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2), any(Content.class), eq(getExpectedMetadata()));
        verify(searchService).indexBinary(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class), eq(getExpectedMetadata()));
    }

    @Test
    public void testUpdateMetadataWithRemovedBinaries() {
        setupBinariesSearchResults();

        UpdateSet updateSet = new UpdateSet(Collections.singletonList(METADATA_WITH_REMOVED_BINARIES_XML_FILENAME), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(3, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        assertTrue(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME2));
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).indexBinary(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadataWithRemovedBinaries()));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2));
        verify(searchService).indexBinary(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class), any());
    }

    @Test
    public void testUpdateBinary() throws Exception {
        setupMetadataSearchResult();

        UpdateSet updateSet = new UpdateSet(Collections.singletonList(BINARY_FILENAME1), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        verify(searchService).indexBinary(
            eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadata()));
    }

    @Test
    public void testDeleteBinary() throws Exception {
        UpdateSet updateSet = new UpdateSet(Collections.emptyList(), Collections.singletonList(BINARY_FILENAME1));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME1));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1));
    }

    @Test
    public void testDeleteMetadata() throws Exception {
        setupBinariesSearchResults();

        UpdateSet updateSet = new UpdateSet(Collections.emptyList(), Collections.singletonList(METADATA_XML_FILENAME));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(3, updateStatus.getAttemptedUpdatesAndDeletes());
        assertTrue(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME1));
        assertTrue(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME2));
        assertTrue(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME3));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1));
        verify(searchService).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME2));
        verify(searchService).indexBinary(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME3), any(Content.class), any());
    }

    protected void setupBinariesSearchResults() {
        when(searchService.searchField(eq(INDEX_ID), eq("localId"), any()))
                .thenReturn(List.of(BINARY_FILENAME1, BINARY_FILENAME2, BINARY_FILENAME3));
    }

    protected void setupMetadataSearchResult() {
        when(searchService.searchField(eq(INDEX_ID), eq("metadataPath"), any()))
                .thenReturn(List.of(getExpectedMetadata().get("metadataPath").toString()));
    }

    protected ElasticsearchBinaryFileWithMetadataBatchIndexer getBatchIndexer() throws Exception {
        ElasticsearchBinaryFileWithMetadataBatchIndexer batchIndexer =
            new ElasticsearchBinaryFileWithMetadataBatchIndexer();
        batchIndexer.setElasticsearchService(searchService);
        batchIndexer.setMetadataPathPatterns(Collections.singletonList(".*metadata.*\\.xml$"));
        batchIndexer.setBinaryPathPatterns(Arrays.asList(".*\\.pdf$", ".*\\.txt$"));
        batchIndexer.setChildBinaryPathPatterns(Collections.singletonList(".*\\.pdf$"));
        batchIndexer.setIncludePropertyPatterns(Collections.singletonList("copyright.*"));
        batchIndexer.setExcludePropertyPatterns(Collections.singletonList("copyright\\.ignore"));
        batchIndexer.setReferenceXPaths(Collections.singletonList("//file"));
        batchIndexer.setMaxFileSize(Long.MAX_VALUE);

        return batchIndexer;
    }

    protected Map<String, Object> getExpectedMetadata() {
        var map = new HashMap<String, Object>();
        map.put("copyright", Map.of(
        "company", "CrafterCMS",
        "text", "All rights reserved",
        "year", "2017"
        ));
        map.put("metadataPath", METADATA_XML_FILENAME);
        return map;
    }

    protected Map<String, Object> getExpectedMetadataWithRemovedBinaries() {
        Map<String, Object> metadata = getExpectedMetadata();
        metadata.put("metadataPath", METADATA_WITH_REMOVED_BINARIES_XML_FILENAME);

        return metadata;
    }

}
