/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.search.batch.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.craftercms.search.batch.UpdateDetail;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.core.exception.CrafterException;
import org.craftercms.core.exception.XmlException;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.processors.impl.ItemProcessorPipeline;
import org.craftercms.core.service.Content;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.service.Item;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that updates/deletes XML files from a search index.
 *
 * @author avasquez
 */
public abstract class AbstractXmlFileBatchIndexer extends AbstractBatchIndexer {

    public static final List<String> DEFAULT_INCLUDE_FILENAME_PATTERNS = Collections.singletonList("^.*\\.xml$");

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ItemProcessor itemProcessor;

    public AbstractXmlFileBatchIndexer() {
        includePathPatterns = DEFAULT_INCLUDE_FILENAME_PATTERNS;
    }

    public void setItemProcessor(ItemProcessor itemProcessor) {
        this.itemProcessor = itemProcessor;
    }

    public void setItemProcessors(List<ItemProcessor> itemProcessors) {
        this.itemProcessor = new ItemProcessorPipeline(itemProcessors);
    }

    @Override
    protected void doSingleFileUpdate(String indexId, String siteName, ContentStoreService contentStoreService,
                                      Context context, String path, boolean delete,
                                      UpdateDetail updateDetail, UpdateStatus updateStatus,
                                      Map<String, String> metadata) {
        if (delete) {
            doDelete(indexId, siteName, path, updateStatus);
        } else {
            String xml = processXml(siteName, contentStoreService, context, path);

            doUpdate(indexId, siteName, path, xml, updateDetail, updateStatus, metadata);
        }
    }

    protected abstract void doDelete(String indexId, String siteName, String path, UpdateStatus updateStatus);

    protected abstract void doUpdate(String indexId, String siteName, String path, String xml,
                                     UpdateDetail updateDetail, UpdateStatus updateStatus,
                                     Map<String, String> metadata);

    protected String processXml(String siteName, ContentStoreService contentStoreService, Context context,
                                String path) throws CrafterException {
        logger.debug("Processing XML @ {}:{} before indexing", siteName, path);

        Item item = contentStoreService.getItem(context, null, path, itemProcessor);
        Document doc = item.getDescriptorDom();

        if (doc != null) {
            String xml = documentToString(item.getDescriptorDom());

            logger.debug("XML @ {} processed successfully:\n{}:{}", siteName, path, xml);

            return xml;
        } else {
            throw new XmlException("Item @ " + siteName + ":" + path + " doesn't seem to be an XML file");
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

    public static class EmptyContent implements Content {

        @Override
        public long getLastModified() {
            return System.currentTimeMillis();
        }

        @Override
        public long getLength() {
            return 0;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

    }

}
