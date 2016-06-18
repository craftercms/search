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

import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 * Extension of {@link DefaultDocumentProcessorChain} that first flattens the document using a
 * {@link FlatteningDocumentProcessor} before executing the default chain.
 *
 * @author avasquez
 */
public class DefaultFlatteningDocumentProcessorChain extends DefaultDocumentProcessorChain {

    protected FlatteningDocumentProcessor flatteningDocumentProcessor;

    public DefaultFlatteningDocumentProcessorChain() {
        flatteningDocumentProcessor = new FlatteningDocumentProcessor();
    }

    public void setIncludeElementXPathQuery(String includeElementXPathQuery) {
        flatteningDocumentProcessor.setIncludeElementXPathQuery(includeElementXPathQuery);
    }

    public void setDisableFlatteningElement(String disableFlatteningElement) {
        flatteningDocumentProcessor.setDisableFlatteningElement(disableFlatteningElement);
    }

    public void setCharEncoding(String charEncoding) {
        flatteningDocumentProcessor.setCharEncoding(charEncoding);
    }

    @Override
    public Document process(Document document, File file, String rootFolder) throws DocumentException {
        document = flatteningDocumentProcessor.process(document, file, rootFolder);

        return super.process(document, file, rootFolder);
    }

}
