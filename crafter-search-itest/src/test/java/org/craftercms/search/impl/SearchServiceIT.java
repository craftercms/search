/*
 * Copyright (C) 2007-2013 Crafter Software Corporation.
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
package org.craftercms.search.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.craftercms.search.service.impl.SolrQuery;
import org.craftercms.search.service.impl.SolrRestClientSearchService;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test of the search service client/server.
 *
 * @author Alfonso Vásquez
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/spring/application-context.xml")
public class SearchServiceIT {

    private static final String SITE = "testsite";
    private static final String IPAD_DOC_ID = "ipad.xml";
    private static final String KINDLE_DOC_ID = "kindle.xml";
    private static final String WP_REASONS_PDF_ID = "crafter-wp-7-reasons.pdf";

    private static final String[] TEST_DOC_IDS = {IPAD_DOC_ID, KINDLE_DOC_ID};

    @Autowired
    private SolrRestClientSearchService searchService;

    @Before
    public void setUp() throws Exception {
        for (Map.Entry<String, String> docEntry : getTestDocs().entrySet()) {
            searchService.update(SITE, docEntry.getKey(), docEntry.getValue(), true);
        }

        Resource wpReasonsPdf = new ClassPathResource("/docs/" + WP_REASONS_PDF_ID);

        Map<String, String> additionalFields = new HashMap<String, String>(2);
        additionalFields.put("testField1", "This is a test");
        additionalFields.put("testField2", "This is another test");

        searchService.updateDocument(SITE, WP_REASONS_PDF_ID, wpReasonsPdf.getFile(), additionalFields);

        searchService.commit();
    }

    @After
    public void tearDown() throws Exception {
        for (String docId : TEST_DOC_IDS) {
            searchService.delete(SITE, docId);
        }

        searchService.delete(SITE, WP_REASONS_PDF_ID);

        searchService.commit();
    }

    @Test
    public void testSearch() throws Exception {
        SolrQuery query = searchService.createQuery();
        query.setQuery("*:*");

        Map<String, Object> results = searchService.search(query);
        assertNotNull(results);

        Map<String, Object> response = (Map<String, Object>)results.get("response");
        assertEquals(3, ((Integer)response.get("numFound")).intValue());

        List<Map<String, Object>> docs = (List<Map<String, Object>>)response.get("documents");
        Map<String, Map<String, Object>> docsMap = new HashMap<String, Map<String, Object>>(3);

        for (Map<String, Object> doc : docs) {
            docsMap.put((String) doc.get("localId"), doc);
        }

        long ipadDate = ISODateTimeFormat.dateTime().parseDateTime("2012-11-30T10:00:00.000Z").getMillis();
        long kindleDate = ISODateTimeFormat.dateTime().parseDateTime("2012-12-15T16:30:00.000Z").getMillis();

        assertDoc(docsMap.get(IPAD_DOC_ID), "iPad", "Apple iPad MC705LL/A (16GB, Wi-Fi, Black) NEWEST MODEL",
                1.4, 517.77, 4, ipadDate);
        assertDoc(docsMap.get(KINDLE_DOC_ID), "Kindle Fire", "Kindle Fire, Full Color 7\" Multi-touch Display, " +
                "Wi-Fi", 0.91, 199.0, 4, kindleDate);

        assertEquals(docsMap.get(WP_REASONS_PDF_ID).get("testField1"), "This is a test");
        assertEquals(docsMap.get(WP_REASONS_PDF_ID).get("testField2"), "This is another test");
    }

    private Map<String, String> getTestDocs() throws IOException {
        Map<String, String> docs = new HashMap<String, String>(1);
        docs.put(IPAD_DOC_ID, getClasspathFileContent("/docs/" + IPAD_DOC_ID));
        docs.put(KINDLE_DOC_ID, getClasspathFileContent("/docs/" + KINDLE_DOC_ID));

        return docs;
    }

    private String getClasspathFileContent(String path) throws IOException {
        return IOUtils.toString(new ClassPathResource(path).getInputStream());
    }

    private void assertDoc(Map<String, Object> doc, String name, String description, Double weight, Double price,
                           Integer rating, long date) {
        assertTrue(!doc.containsKey("code"));
        assertEquals(name, doc.get("name"));
        assertEquals(description, doc.get("description_html"));
        assertEquals(weight, doc.get("weight_f"));
        assertEquals(price, doc.get("price_f"));
        assertEquals(rating, doc.get("rating_i"));
        assertEquals(date, doc.get("date_dt"));
    }

}
