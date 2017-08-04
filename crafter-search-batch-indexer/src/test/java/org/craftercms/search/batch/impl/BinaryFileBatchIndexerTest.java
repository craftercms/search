package org.craftercms.search.batch.impl;

import java.util.Collections;

import org.craftercms.core.service.Content;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link BinaryFileBatchIndexer}.
 *
 * @author avasquez
 */
public class BinaryFileBatchIndexerTest extends BatchIndexerTestBase {

    private static final String SITE_NAME = "test";
    private static final String SUPPORTED_FILENAME = "crafter-wp-7-reasons.pdf";
    private static final String NON_SUPPORTED_FILENAME = "image.jpg";

    private BinaryFileBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        batchIndexer = getBatchIndexer();
    }

    @Test
    public void testProcess() throws Exception {
        String indexId = SITE_NAME;
        UpdateSet updateSet = new UpdateSet(Collections.singletonList(SUPPORTED_FILENAME), Collections.singletonList(NON_SUPPORTED_FILENAME));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, indexId, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(1, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(SUPPORTED_FILENAME, updateStatus.getSuccessfulUpdates().get(0));
        verify(searchService).updateContent(eq(indexId), eq(SITE_NAME), eq(SUPPORTED_FILENAME), any(Content.class));
        verify(searchService, never()).delete(indexId, SITE_NAME, NON_SUPPORTED_FILENAME);
    }

    protected BinaryFileBatchIndexer getBatchIndexer() throws Exception {
        BinaryFileBatchIndexer batchIndexer = new BinaryFileBatchIndexer();
        batchIndexer.setSupportedMimeTypes(Collections.singletonList("application/pdf"));

        return batchIndexer;
    }

}
