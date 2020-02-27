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
package org.craftercms.search.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.craftercms.search.service.AdminService;
import org.craftercms.search.service.impl.v2.RestClientAdminService;
import org.craftercms.search.service.impl.SolrQuery;
import org.craftercms.search.service.impl.v2.SolrRestClientSearchService;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration test of the search service client/server.
 *
 * @author Alfonso Vásquez
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/spring/application-context.xml")
public class SearchServiceIT {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceIT.class);

    private static final String DEFAULT_INDEX_ID = "default";
    private static final String PLUTON_SITE = "pluton";
    private static final String PLUTON_INDEX_ID = "pluton";
    private static final String IPAD_DOC_ID = "ipad.xml";
    private static final String DISABLED_DOC_ID = "disabled.xml";
    private static final String EXPIRED_DOC_ID = "expired.xml";
    private static final String IPAD_DOC_ID_ACCESSORIES0 = IPAD_DOC_ID + "_accessories_0";
    private static final String IPAD_DOC_ID_ACCESSORIES1 = IPAD_DOC_ID + "_accessories_1";
    private static final String WP_REASONS_PDF_DOC_ID = "crafter-wp-7-reasons.pdf";

    private static final List<String> WP_REASONS_PDF_TAGS = Arrays.asList("Crafter", "reasons", "white paper");
    
    @Autowired
    private SolrRestClientSearchService searchService;
    @Autowired
    private RestClientAdminService adminService;

    @Before
    public void setUp() throws Exception {
        adminService.createIndex(PLUTON_INDEX_ID);
    }

    @After
    public void tearDown() throws Exception {
        adminService.deleteIndex(PLUTON_INDEX_ID, AdminService.IndexDeleteMode.ALL_DATA_AND_CONFIG);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMethods() throws Exception {
        SolrQuery query = searchService.createQuery();
        query.setQuery("*:*");

        Map<String, Object> results = searchService.search(PLUTON_INDEX_ID, query);
        assertNotNull(results);

        Map<String, Object> response = getQueryResponse(results);
        assertEquals(0, getNumDocs(response));

        String xml = getClasspathFileContent("docs/" + IPAD_DOC_ID);
        searchService.update(PLUTON_INDEX_ID, PLUTON_SITE, IPAD_DOC_ID, xml, true);

        xml = getClasspathFileContent("docs/" + DISABLED_DOC_ID);
        searchService.update(PLUTON_INDEX_ID, PLUTON_SITE, DISABLED_DOC_ID, xml, true);

        xml = getClasspathFileContent("docs/" + EXPIRED_DOC_ID);
        searchService.update(PLUTON_INDEX_ID, PLUTON_SITE, EXPIRED_DOC_ID, xml, true);

        File file = getClasspathFile("docs/" + WP_REASONS_PDF_DOC_ID);
        searchService.updateContent(PLUTON_INDEX_ID, PLUTON_SITE, WP_REASONS_PDF_DOC_ID, file);

        searchService.commit(PLUTON_INDEX_ID);

        query.setDisableAdditionalFilters(true);

        results = searchService.search(PLUTON_INDEX_ID, query);
        assertNotNull(results);

        response = getQueryResponse(results);
        assertEquals(6, getNumDocs(response));

        query.setDisableAdditionalFilters(false);

        results = searchService.search(PLUTON_INDEX_ID, query);
        assertNotNull(results);

        response = getQueryResponse(results);
        assertEquals(4, getNumDocs(response));

        Map<String, Map<String, Object>> docs = getDocs(response);
        Map<String, Object> ipadDoc = docs.get(IPAD_DOC_ID);
        Map<String, Object> ipadAccessoriesSubDoc1 = docs.get(IPAD_DOC_ID_ACCESSORIES0);
        Map<String, Object> ipadAccessoriesSubDoc2 = docs.get(IPAD_DOC_ID_ACCESSORIES1);
        Map<String, Object> wpReasonsPdfDoc = docs.get(WP_REASONS_PDF_DOC_ID);

        assertNotNull(ipadDoc);
        assertNotNull(ipadAccessoriesSubDoc1);
        assertNotNull(ipadAccessoriesSubDoc2);
        assertNotNull(wpReasonsPdfDoc);
        assertIPadDoc(ipadDoc);
        assertIPadAccessoriesSubDoc1(ipadAccessoriesSubDoc1);
        assertIPadAccessoriesSubDoc2(ipadAccessoriesSubDoc2);
        assertWpReasonsPdfDoc(wpReasonsPdfDoc);

        MultiValueMap<String, String> additionalFields = new LinkedMultiValueMap<>();
        additionalFields.put("tags.value_smv", WP_REASONS_PDF_TAGS);

        searchService.updateContent(PLUTON_INDEX_ID, PLUTON_SITE, WP_REASONS_PDF_DOC_ID, file, additionalFields);
        searchService.commit(PLUTON_INDEX_ID);

        query = searchService.createQuery();
        query.setQuery("localId:\"" + WP_REASONS_PDF_DOC_ID + "\"");

        results = searchService.search(PLUTON_INDEX_ID, query);
        assertNotNull(results);
        
        response = getQueryResponse(results);
        assertEquals(1, getNumDocs(response));

        docs = getDocs(response);
        wpReasonsPdfDoc = docs.get(WP_REASONS_PDF_DOC_ID);

        assertNotNull(wpReasonsPdfDoc);
        assertWpReasonsPdfDocWithAdditionalFields(wpReasonsPdfDoc);

        searchService.delete(PLUTON_INDEX_ID, PLUTON_SITE, IPAD_DOC_ID);
        searchService.delete(PLUTON_INDEX_ID, PLUTON_SITE, WP_REASONS_PDF_DOC_ID);
        searchService.commit(PLUTON_INDEX_ID);

        query = searchService.createQuery();
        query.setQuery("*:*");

        results = searchService.search(query.setQuery("*:*"));
        assertNotNull(results);

        response = getQueryResponse(results);
        assertEquals(0, getNumDocs(response));
    }

    private File getClasspathFile(String path) throws IOException {
        return new ClassPathResource(path).getFile();
    }

    private String getClasspathFileContent(String path) throws IOException {
        return IOUtils.toString(new ClassPathResource(path).getInputStream(), Charset.defaultCharset());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getQueryResponse(Map<String, Object> results) {
        return (Map<String, Object>)results.get("response");
    }

    private int getNumDocs(Map<String, Object> response) {
        return (Integer)response.get("numFound");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> getDocs(Map<String, Object> response) {
        List<Map<String, Object>> docList = (List<Map<String, Object>>)response.get("documents");
        Map<String, Map<String, Object>> docs = new HashMap<>(3);

        for (Map<String, Object> doc : docList) {
            docs.put((String)doc.get("localId"), doc);
        }

        return docs;
    }

    private void assertIPadDocCommonFields(Map<String, Object> doc) {
        long date = ISODateTimeFormat.dateTime().parseDateTime("2014-10-01T00:00:00.000Z").getMillis();

        assertNotNull(doc.get("crafterPublishedDate"));
        assertNotNull(doc.get("crafterPublishedDate_dt"));
        assertEquals(PLUTON_SITE, doc.get("crafterSite"));
        assertEquals("iPad Air 64GB", doc.get("name_s"));
        assertEquals("iPad Air 64GB", doc.get("name_t"));
        assertEquals("Apple MH182LL/A iPad Air 9.7-Inch Retina Display 64GB, Wi-Fi (Gold)", doc.get("description_html").toString().trim());
        assertEquals(date, doc.get("availableDate_dt"));
        assertEquals(Arrays.asList("Apple", "iPad", "Tablet"), doc.get("tags.value_smv"));
    }

    @SuppressWarnings("unchecked")
    private void assertIPadDoc(Map<String, Object> doc) {
        assertIPadDocCommonFields(doc);
        assertEquals(PLUTON_SITE + ":" + IPAD_DOC_ID, doc.get("id"));
        assertEquals(PLUTON_SITE + ":" + IPAD_DOC_ID, doc.get("rootId"));
        assertEquals(IPAD_DOC_ID, doc.get("localId"));
        assertEquals("product", doc.get("content-type"));
        assertEquals(Arrays.asList("Apple", "iPad", "Tablet"), doc.get("tags.value_smv"));
        assertEquals(Arrays.asList("Silicon case with stand for iPad Air 64GB", "Lighting cable for iPad"),
                     trimValues((Collection<Object>)doc.get("accessories.item.description_html")));
        assertEquals(Arrays.asList("Case", "Lighting Cable"), doc.get("accessories.item.name_smv"));
        assertEquals(Arrays.asList("Black", "Blue", "Red"), doc.get("accessories.item.colors.color_smv"));
    }

    private void assertIPadAccessoriesSubDoc1(Map<String, Object> doc) {
        assertIPadDocCommonFields(doc);
        assertEquals(PLUTON_SITE + ":" + IPAD_DOC_ID_ACCESSORIES0, doc.get("id"));
        assertEquals(IPAD_DOC_ID_ACCESSORIES0, doc.get("localId"));
        assertEquals(PLUTON_SITE + ":" + IPAD_DOC_ID, doc.get("parentId"));
        assertEquals(PLUTON_SITE + ":" + IPAD_DOC_ID, doc.get("rootId"));
        assertEquals("product_accessories", doc.get("content-type"));
        assertEquals("Case", doc.get("accessories.item.name_s"));
        assertEquals("Silicon case with stand for iPad Air 64GB", doc.get("accessories.item.description_html").toString().trim());
        assertEquals(Arrays.asList("Black", "Blue", "Red"), doc.get("accessories.item.colors.color_smv"));
    }

    private void assertIPadAccessoriesSubDoc2(Map<String, Object> doc) {
        assertIPadDocCommonFields(doc);
        assertEquals(PLUTON_SITE + ":" + IPAD_DOC_ID_ACCESSORIES1, doc.get("id"));
        assertEquals(IPAD_DOC_ID_ACCESSORIES1, doc.get("localId"));
        assertEquals(PLUTON_SITE + ":" + IPAD_DOC_ID, doc.get("parentId"));
        assertEquals(PLUTON_SITE + ":" + IPAD_DOC_ID, doc.get("rootId"));
        assertEquals("product_accessories", doc.get("content-type"));
        assertEquals("Lighting Cable", doc.get("accessories.item.name_s"));
        assertEquals("Lighting cable for iPad", doc.get("accessories.item.description_html").toString().trim());
    }

    private void assertWpReasonsPdfDoc(Map<String, Object> doc) {
        assertNotNull(doc.get("crafterPublishedDate"));
        assertNotNull(doc.get("crafterPublishedDate_dt"));
        assertEquals(PLUTON_SITE, doc.get("crafterSite"));
        assertEquals(PLUTON_SITE + ":" + WP_REASONS_PDF_DOC_ID, doc.get("id"));
        assertEquals(PLUTON_SITE + ":" + WP_REASONS_PDF_DOC_ID, doc.get("rootId"));
        assertEquals(WP_REASONS_PDF_DOC_ID, doc.get("localId"));
        assertNotNull(doc.get("content"));
    }

    private void assertWpReasonsPdfDocWithAdditionalFields(Map<String, Object> doc) {
        assertWpReasonsPdfDoc(doc);

        assertEquals(WP_REASONS_PDF_TAGS, doc.get("tags.value_smv"));
    }

    private Collection<String> trimValues(Collection<Object> values) {
        List<String> trimmedValues = new ArrayList<>(values.size());
        for (Object value : values) {
            trimmedValues.add(value.toString().trim());
        }

        return trimmedValues;
    }

}
