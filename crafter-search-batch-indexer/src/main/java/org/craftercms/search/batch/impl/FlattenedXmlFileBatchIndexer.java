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
package org.craftercms.search.batch.impl;

import java.util.ArrayList;
import java.util.List;

import org.craftercms.search.batch.utils.xml.DocumentProcessor;
import org.craftercms.search.batch.utils.xml.FlatteningDocumentProcessor;

/**
 * {@link org.craftercms.search.batch.BatchIndexer} that updates/deletes XML files from a search index. The XML files
 * are first processed by the XML flattening document processor.
 *
 * @author avasquez
 */
public class FlattenedXmlFileBatchIndexer extends XmlFileBatchIndexer {

    public FlattenedXmlFileBatchIndexer() {
       this(new FlatteningDocumentProcessor());
    }

    public FlattenedXmlFileBatchIndexer(FlatteningDocumentProcessor flatteningDocumentProcessor) {
        documentProcessors = new ArrayList<>();
        documentProcessors.add(flatteningDocumentProcessor);
    }

    @Override
    public void setDocumentProcessors(List<DocumentProcessor> documentProcessors) {
        this.documentProcessors.addAll(documentProcessors);
    }

}
