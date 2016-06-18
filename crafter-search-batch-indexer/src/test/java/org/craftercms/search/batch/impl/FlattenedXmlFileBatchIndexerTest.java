package org.craftercms.search.batch.impl;

import java.util.Collections;

import org.craftercms.search.batch.utils.xml.DefaultFlatteningDocumentProcessorChain;
import org.craftercms.search.batch.utils.xml.DocumentProcessor;
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
                                               "<title_s tokenize=\"true\">Test</title_s>" +
                                               "<date>11/10/2015 00:00:00</date>" +
                                               "<component>" +
                                               "<fileName>component.xml</fileName>" +
                                               "<title_s tokenize=\"true\">Test</title_s>" +
                                               "<date>11/11/2015 10:00:00</date>" +
                                               "<title_t tokenize=\"true\">Test</title_t>" +
                                               "</component>" +
                                               "<include>test.xml</include>" +
                                               "<title_t tokenize=\"true\">Test</title_t>" +
                                               "</page>";

    protected DocumentProcessor getDocumentProcessor() throws Exception {
        DefaultFlatteningDocumentProcessorChain processor = new DefaultFlatteningDocumentProcessorChain();
        processor.setFieldMappings(Collections.singletonMap("//name", "fileName"));

        return processor;
    }

    protected XmlFileBatchIndexer getBatchIndexer(SearchService searchService) throws Exception {
        XmlFileBatchIndexer batchIndexer = new FlattenedXmlFileBatchIndexer();
        batchIndexer.setDocumentProcessor(getDocumentProcessor());
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
