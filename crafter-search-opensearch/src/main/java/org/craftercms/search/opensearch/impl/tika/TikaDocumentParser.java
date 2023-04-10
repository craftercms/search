/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.opensearch.impl.tika;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.craftercms.search.commons.exception.SearchException;
import org.craftercms.search.opensearch.DocumentParser;
import org.craftercms.search.opensearch.MetadataExtractor;
import org.craftercms.search.opensearch.impl.AbstractDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.craftercms.search.commons.utils.MapUtils.mergeMaps;

/**
 * Implementation of {@link DocumentParser} that uses Apache Tika
 * @author joseross
 */
public class TikaDocumentParser extends AbstractDocumentParser {

    private static final Logger logger = LoggerFactory.getLogger(TikaDocumentParser.class);

    /**
     * The maximum number of characters to parse from the document.
     * Defaults to 0 to parse only metadata.
     */
    protected int charLimit = 0;

    /**
     * Jackson {@link ObjectMapper} instance
     */
    protected ObjectMapper objectMapper = new XmlMapper();

    /**
     * List of metadata extractors to apply after parsing documents
     */
    protected final List<MetadataExtractor<Metadata>> metadataExtractors;

    /**
     * Apache {@link Tika} instance
     */
    protected Tika tika = new Tika();

    protected final FileTypeMap fileTypeMap = new MimetypesFileTypeMap();



    public void setCharLimit(final int charLimit) {
        this.charLimit = charLimit;
    }

    public void setObjectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TikaDocumentParser(final List<MetadataExtractor<Metadata>> metadataExtractors) {
        this.metadataExtractors = metadataExtractors;
    }

    public void setTika(final Tika tika) {
        this.tika = tika;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String parseToXml(final String filename, final Resource resource,
                             final Map<String, Object> additionalFields) {
        Metadata metadata = new Metadata();
        try {
            // Tika will close the stream, so it can't be used for anything after this, can't use auto close ...
            InputStream in = resource.getInputStream();
            String parsedContent = tika.parseToString(in, metadata, charLimit);
            return extractMetadata(filename, resource, parsedContent, metadata, additionalFields);
        } catch (IOException | TikaException e) {
            logger.error("Error parsing file", e);
            throw new SearchException("Error parsing file", e);
        }
    }

    /**
     * Prepares the document to be indexed
     * @param resource the content of the parsed file
     * @param metadata the metadata of the parsed file
     * @param additionalFields additional fields to be added
     * @return the XML ready to be indexed
     */
    protected String extractMetadata(String filename, Resource resource, String parsedContent, Metadata metadata,
                                     Map<String, Object> additionalFields) {
        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isNotEmpty(parsedContent)) {
            map.put(fieldNameContent, parsedContent);
        }

        String type = fileTypeMap.getContentType(filename);
        if (!"application/octet-stream".equals(type)) {
            map.put("contentType", type);
        }

        try {
            map.put("contentLength", resource.contentLength());
        } catch (IOException e) {
            logger.warn("Could not find file size for {}", resource);
        }
        metadataExtractors.forEach(extractor -> extractor.extract(resource, metadata, map));

        Map<String, Object> mergedMap = mergeMaps(map, additionalFields);

        try {
            return objectMapper.writeValueAsString(mergedMap);
        } catch (JsonProcessingException e) {
            logger.error("Error writing parsed document as XML");
            throw new SearchException("Error writing parsed document as XML", e);
        }
    }

}
