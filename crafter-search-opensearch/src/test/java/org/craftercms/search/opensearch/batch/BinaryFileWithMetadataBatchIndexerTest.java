/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.opensearch.batch;

import org.craftercms.core.exception.PathNotFoundException;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.Context;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OpenSearchBinaryFileWithMetadataBatchIndexer}.
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

    private OpenSearchBinaryFileWithMetadataBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        when(contentStoreService.findContent(any(Context.class), anyString())).thenAnswer(
                invocationOnMock -> {
                    Object[] args = invocationOnMock.getArguments();
                    String path = (String)args[1];
                    Content content = findContent(path);

                    if (content != null) {
                        return content;
                    }
                    throw new PathNotFoundException();
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


    // TODO: JM: Revisit test case
    @Test
    public void testUpdateBinary() {
        setupMetadataSearchResult();

        UpdateSet updateSet = new UpdateSet(Collections.singletonList(BINARY_FILENAME1), Collections.emptyList());
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        // Binary files are no longer processed by AbstractBinaryFileWithMetadataBatchIndexer
        assertEquals(0, updateStatus.getAttemptedUpdatesAndDeletes());
        assertFalse(updateStatus.getSuccessfulUpdates().contains(BINARY_FILENAME1));
        verify(searchService, times(0)).indexBinary(
                eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1), any(Content.class), eq(getExpectedMetadata()));
    }

    @Test
    public void testDeleteBinary() {
        UpdateSet updateSet = new UpdateSet(Collections.emptyList(), Collections.singletonList(BINARY_FILENAME1));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(INDEX_ID, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        // Binary files are no longer processed by AbstractBinaryFileWithMetadataBatchIndexer
        assertEquals(0, updateStatus.getAttemptedUpdatesAndDeletes());
        assertFalse(updateStatus.getSuccessfulDeletes().contains(BINARY_FILENAME1));
        verify(searchService, times(0)).delete(eq(INDEX_ID), eq(SITE_NAME), eq(BINARY_FILENAME1));
    }

    @Test
    public void testDeleteMetadata() {
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
        lenient().when(searchService.searchField(eq(INDEX_ID), eq("metadataPath"), any()))
                .thenReturn(List.of(getExpectedMetadata().get("metadataPath").toString()));
    }

    protected OpenSearchBinaryFileWithMetadataBatchIndexer getBatchIndexer() {
        OpenSearchBinaryFileWithMetadataBatchIndexer batchIndexer =
            new OpenSearchBinaryFileWithMetadataBatchIndexer(searchService);
        batchIndexer.setMetadataPathPatterns(Collections.singletonList(".*metadata.*\\.xml$"));
        batchIndexer.setChildBinaryPathPatterns(Collections.singletonList(".*\\.pdf$"));
        batchIndexer.setIncludePropertyPatterns(Collections.singletonList("copyright.*"));
        batchIndexer.setExcludePropertyPatterns(Collections.singletonList("copyright\\.ignore"));
        batchIndexer.setReferenceXPaths(Collections.singletonList("//file"));

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
