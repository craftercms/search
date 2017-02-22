package org.craftercms.search.batch.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.core.exception.CrafterException;
import org.craftercms.core.exception.XmlException;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.processors.impl.ItemProcessorPipeline;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.craftercms.search.batch.IndexingStatus;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that updates/deletes XML files from a search index.
 *
 * @author avasquez
 */
public class XmlFileBatchIndexer extends AbstractBatchIndexer {

    private static final Log logger = LogFactory.getLog(XmlFileBatchIndexer.class);

    public static final List<String> DEFAULT_INCLUDE_FILENAME_PATTERNS = Collections.singletonList("^.*\\.xml$");

    protected ItemProcessor itemProcessor;

    public XmlFileBatchIndexer() {
        includeFileNamePatterns = DEFAULT_INCLUDE_FILENAME_PATTERNS;
    }

    public void setItemProcessor(ItemProcessor itemProcessor) {
        this.itemProcessor = itemProcessor;
    }

    public void setItemProcessors(List<ItemProcessor> itemProcessors) {
        this.itemProcessor = new ItemProcessorPipeline(itemProcessors);
    }

    @Override
    protected void doSingleFileUpdate(String indexId, String siteName, ContentStoreService contentStoreService, Context context,
                                      String path, boolean delete, IndexingStatus status) throws Exception {
        if (delete) {
            doDelete(indexId, siteName, path, status);
        } else {
            doUpdate(indexId, siteName, path, processXml(siteName, contentStoreService, context, path), status);
        }
    }

    protected String processXml(String siteName, ContentStoreService contentStoreService, Context context,
                                String path) throws CrafterException {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing XML @ " + getSiteBasedPath(siteName, path) + " before indexing");
        }

        Item item = contentStoreService.getItem(context, null, path, itemProcessor);
        Document doc = item.getDescriptorDom();

        if (doc != null) {
            String xml = documentToString(item.getDescriptorDom());

            if (logger.isDebugEnabled()) {
                logger.debug("XML @ " + getSiteBasedPath(siteName, path) + " processed successfully:\n" + xml);
            }

            return xml;
        } else {
            throw new XmlException("Item @ " + getSiteBasedPath(siteName, path) + " doesn't seem to be an XML file");
        }
    }

    protected String documentToString(Document document) {
        StringWriter stringWriter = new StringWriter();
        OutputFormat format = OutputFormat.createCompactFormat();
        XMLWriter xmlWriter = new XMLWriter(stringWriter, format);

        try {
            xmlWriter.write(document);
        } catch (IOException e) {
            // Ignore, shouldn't happen.
        }

        return stringWriter.toString();
    }

}
