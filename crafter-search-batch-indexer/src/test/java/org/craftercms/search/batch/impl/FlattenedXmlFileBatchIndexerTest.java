package org.craftercms.search.batch.impl;

import org.craftercms.search.service.SearchService;

/**
 * Unit tests for {@link FlattenedXmlFileBatchIndexer}.
 *
 * @author avasquez
 */
public class FlattenedXmlFileBatchIndexerTest extends XmlFileBatchIndexerTest {

    private static final String UPDATE_FILENAME = "test2.xml";
    private static final String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                               "<page>" +
                                               "<fileName>test2.xml</fileName>" +
                                               "<title>Test</title>" +
                                               "<date format=\"MM/DD/YYY HH:MM:SS\">11/10/2015 00:00:00</date>" +
                                               "<component>" +
                                               "<fileName>component.xml</fileName>" +
                                               "<title>Test</title>" +
                                               "<date format=\"MM/DD/YYY HH:MM:SS\">11/11/2015 10:00:00</date>" +
                                               "</component>" +
                                               "<include>test.xml</include>" +
                                               "</page>";

    protected XmlFileBatchIndexer getBatchIndexer(SearchService searchService) throws Exception {
        FlattenedXmlFileBatchIndexer batchIndexer = new FlattenedXmlFileBatchIndexer();
        batchIndexer.setDocumentProcessors(getDocumentProcessors());
        batchIndexer.setSearchService(searchService);

        return batchIndexer;
    }

    @Override
    protected String getUpdateFilename() {
        return UPDATE_FILENAME;
    }

    @Override
    protected String getExpectedXml() {
        return EXPECTED_XML;
    }

}
