package org.craftercms.search.batch.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.processors.impl.AttributeAddingProcessor;
import org.craftercms.core.processors.impl.FieldRenamingProcessor;
import org.craftercms.core.processors.impl.PageAwareIncludeDescriptorsProcessor;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.service.SearchService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link XmlFileBatchIndexer}.
 *
 * @author avasquez
 */
public class XmlFileBatchIndexerTest extends BatchIndexerTestBase {

    private static final String SITE_NAME = "test";
    private static final String UPDATE_FILENAME = "test2.xml";
    private static final String DELETE_FILENAME = "deleteme.xml";
    private static final String EXPECTED_XML =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                "<page>" +
                                                    "<fileName>test2.xml</fileName>" +
                                                    "<title>Test</title>" +
                                                    "<date format=\"MM/DD/YYY HH:MM:SS\">11/10/2015 00:00:00</date>" +
                                                    "<include>component.xml</include>" +
                                                    "<component>" +
                                                        "<fileName>component.xml</fileName>" +
                                                        "<title>Test</title>" +
                                                        "<date format=\"MM/DD/YYY HH:MM:SS\">11/11/2015 10:00:00</date>" +
                                                    "</component>" +
                                                    "<include>test.xml</include>" +
                                                "</page>";

    private XmlFileBatchIndexer batchIndexer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        batchIndexer = getBatchIndexer();
    }

    @Test
    public void testUpdateIndex() throws Exception {
        String indexId = SITE_NAME;
        UpdateSet updateSet = new UpdateSet(Collections.singletonList(UPDATE_FILENAME), Collections.singletonList(DELETE_FILENAME));
        UpdateStatus updateStatus = new UpdateStatus();

        batchIndexer.updateIndex(searchService, indexId, SITE_NAME, contentStoreService, context, updateSet, updateStatus);

        assertEquals(2, updateStatus.getAttemptedUpdatesAndDeletes());
        assertEquals(UPDATE_FILENAME, updateStatus.getSuccessfulUpdates().get(0));
        assertEquals(DELETE_FILENAME, updateStatus.getSuccessfulDeletes().get(0));
        verify(searchService).update(indexId, SITE_NAME, UPDATE_FILENAME, EXPECTED_XML, true);
        verify(searchService).delete(indexId, SITE_NAME, DELETE_FILENAME);
    }

    protected List<ItemProcessor> getDocumentProcessors() throws Exception {
        PageAwareIncludeDescriptorsProcessor proc1 = new PageAwareIncludeDescriptorsProcessor();
        proc1.setIncludeElementXPathQuery("//include");
        proc1.setDisabledIncludeNodeXPathQuery("@disabled");
        proc1.setPagesPathPattern("test\\.xml|test2\\.xml");
        proc1.setIncludedItemsProcessor(proc1);
        proc1.setContentStoreService(contentStoreService);

        FieldRenamingProcessor proc2 = new FieldRenamingProcessor();
        proc2.setFieldMappings(Collections.singletonMap("//name", "fileName"));

        Map<String, String> attributes = Collections.singletonMap("format", "MM/DD/YYY HH:MM:SS");

        AttributeAddingProcessor proc3 = new AttributeAddingProcessor();
        proc3.setAttributeMappings(Collections.singletonMap("//date", attributes));

        return Arrays.asList(proc1, proc2, proc3);
    }

    protected SearchService getSearchService() throws Exception {
        return mock(SearchService.class);
    }

    protected XmlFileBatchIndexer getBatchIndexer() throws Exception {
        XmlFileBatchIndexer batchIndexer = new XmlFileBatchIndexer();
        batchIndexer.setItemProcessors(getDocumentProcessors());

        return batchIndexer;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
