package org.craftercms.search.batch.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.craftercms.search.batch.exception.BatchIndexingException;
import org.craftercms.search.batch.utils.XmlUtils;
import org.craftercms.search.batch.utils.xml.DocumentProcessor;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that updates/deletes XML files from a search index.
 *
 * @author avasquez
 */
public class XmlFileBatchIndexer extends AbstractBatchIndexer {

    private static final Log logger = LogFactory.getLog(XmlFileBatchIndexer.class);

    public static final List<String> DEFAULT_INCLUDE_FILENAME_PATTERNS = Collections.singletonList("^.*\\.xml$");

    protected List<DocumentProcessor> documentProcessors;
    protected String charset;

    public XmlFileBatchIndexer() {
        includeFileNamePatterns = DEFAULT_INCLUDE_FILENAME_PATTERNS;
        charset = "UTF-8";
    }

    public void setDocumentProcessors(List<DocumentProcessor> documentProcessors) {
        this.documentProcessors = documentProcessors;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    protected boolean doSingleFileUpdate(String indexId, String siteName, String rootFolder, String fileName,
                                         boolean delete) throws BatchIndexingException {
        File file = new File(rootFolder, fileName);

        if (delete) {
            return doDelete(indexId, siteName, fileName);
        } else {
            try {
                String xml = processXml(rootFolder, file);

                return doUpdate(indexId, siteName, fileName, xml);
            } catch (DocumentException e) {
                logger.warn("Cannot process XML file " + siteName + ":" + fileName + ". Continuing index update...", e);
            }
        }

        return false;
    }

    protected String processXml(String rootFolder, File file) throws DocumentException {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing XML file " + file + " before indexing");
        }

        Document document = processDocument(XmlUtils.readXml(file, charset), file, rootFolder);
        String xml = documentToString(document);

        if (logger.isDebugEnabled()) {
            logger.debug("XML file " + file + " processed successfully:\n" + xml);
        }

        return xml;
    }

    protected Document processDocument(Document document, File file, String rootFolder) throws DocumentException {
        if (CollectionUtils.isNotEmpty(documentProcessors)) {
            for (DocumentProcessor processor : documentProcessors) {
                document = processor.process(document, file, rootFolder);
            }
        }

        return document;
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
