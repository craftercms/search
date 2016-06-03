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
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;

/**
 * Default composite {@link DocumentProcessor}, which executes field renaming with a
 * {@link FieldRenamingDocumentProcessor} and tokenize attribute parsing with a
 * {@link TokenizeAttributeParsingDocumentProcessor}.
 *
 * @author avasquez
 */
public class DefaultDocumentProcessorChain implements DocumentProcessor {

    protected FieldRenamingDocumentProcessor fieldRenamingDocumentProcessor;
    protected TokenizeAttributeParsingDocumentProcessor tokenizeAttributeParsingDocumentProcessor;

    public DefaultDocumentProcessorChain() {
        fieldRenamingDocumentProcessor = new FieldRenamingDocumentProcessor();
        tokenizeAttributeParsingDocumentProcessor = new TokenizeAttributeParsingDocumentProcessor();
    }

    public void setFieldMappings(Map<String, String> fieldMappings) {
        fieldRenamingDocumentProcessor.setFieldMappings(fieldMappings);
    }

    public void setTokenizeAttribute(String tokenizeAttribute) {
        tokenizeAttributeParsingDocumentProcessor.setTokenizeAttribute(tokenizeAttribute);
    }

    public void setTokenizeSubstitutionMap(Map<String, String> tokenizeSubstitutionMap) {
        tokenizeAttributeParsingDocumentProcessor.setTokenizeSubstitutionMap(tokenizeSubstitutionMap);
    }

    @Override
    public Document process(Document document, File file, String rootFolder) throws DocumentException {
        document = fieldRenamingDocumentProcessor.process(document, file, rootFolder);
        document = tokenizeAttributeParsingDocumentProcessor.process(document, file, rootFolder);

        return document;
    }

}
