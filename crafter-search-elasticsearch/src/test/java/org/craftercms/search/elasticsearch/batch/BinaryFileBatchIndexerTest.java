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

import org.craftercms.core.service.Content;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ElasticsearchBinaryFileBatchIndexer}.
 *
 * @author avasquez
 */
public class BinaryFileBatchIndexerTest extends BatchIndexerTestBase {

    private static final String SITE_NAME = "test";
    private static final String SUPPORTED_FILENAME = "crafter-wp-7-reasons.pdf";
    private static final String NON_SUPPORTED_FILENAME = "image.jpg";

    private ElasticsearchBinaryFileBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        when(contentStoreService.findContent(any(), anyString())).thenAnswer(
                invocationOnMock -> {
                    Object[] args = invocationOnMock.getArguments();
                    String path = (String)args[1];

                    return findContent(path);
                }
        );

        batchIndexer = getBatchIndexer();
    }

    @Test
    public void testProcess() throws Exception {
        String indexId = SITE_NAME;
        UpdateSet updateSet = new UpdateSet(Collections.singletonList(SUPPORTED_FILENAME), Collections.singletonList(NON_SUPPORTED_FILENAME));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(indexId, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(SUPPORTED_FILENAME, updateStatus.getSuccessfulUpdates().get(0));
        verify(searchService)
                .indexBinary(eq(indexId), eq(SITE_NAME), eq(SUPPORTED_FILENAME), any(Content.class), eq(null));
        verify(searchService, never()).delete(indexId, SITE_NAME, NON_SUPPORTED_FILENAME);
    }

    protected ElasticsearchBinaryFileBatchIndexer getBatchIndexer() throws Exception {
        ElasticsearchBinaryFileBatchIndexer batchIndexer = new ElasticsearchBinaryFileBatchIndexer();
        batchIndexer.setElasticsearchService(searchService);
        batchIndexer.setSupportedMimeTypes(Collections.singletonList("application/pdf"));
        batchIndexer.setMaxFileSize(Long.MAX_VALUE);

        return batchIndexer;
    }

}
