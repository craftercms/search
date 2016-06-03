/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.search.batch.utils.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 * Represents a chain of {@link DocumentProcessor}.
 *
 * @author avasquez
 */
public class DocumentProcessorChain implements DocumentProcessor {

    protected List<DocumentProcessor> processors;

    public DocumentProcessorChain() {
    }

    public DocumentProcessorChain(List<DocumentProcessor> processors) {
        this.processors = processors;
    }

    public void setProcessors(List<DocumentProcessor> processors) {
        this.processors = processors;
    }

    public void addProcessor(DocumentProcessor processor) {
        if (processors == null) {
            processors = new ArrayList<>();
        }

        processors.add(processor);
    }

    @Override
    public Document process(Document document, File file, String rootFolder) throws DocumentException {
        if (CollectionUtils.isNotEmpty(processors)) {
            for (DocumentProcessor processor : processors) {
                document = processor.process(document, file, rootFolder);
            }
        }

        return document;
    }

}
