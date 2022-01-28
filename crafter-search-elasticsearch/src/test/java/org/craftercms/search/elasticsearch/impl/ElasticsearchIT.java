/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.search.elasticsearch.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.ElasticsearchWrapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.util.stream.Collectors.toMap;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration test of the search service client/server.
 *
 * @author Alfonso Vásquez
 * @author joseross
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/spring/application-context.xml")
public class ElasticsearchIT {

    private static final String PLUTON_SITE = "pluton";
    private static final String PLUTON_INDEX_ID = "pluton";
    private static final String IPAD_DOC_ID = "ipad.xml";
    private static final String DISABLED_DOC_ID = "disabled.xml";
    private static final String EXPIRED_DOC_ID = "expired.xml";
    private static final String WP_REASONS_PDF_DOC_ID = "crafter-wp-7-reasons.pdf";

    private static final List<String> WP_REASONS_PDF_TAGS = Arrays.asList("Crafter", "reasons", "white paper");
    
    @Autowired
    private ElasticsearchWrapper searchClient;

    @Autowired
    private ElasticsearchService searchService;

    @Autowired
    private ElasticsearchAdminService adminService;

    @Before
    public void setUp() throws Exception {
        adminService.createIndex(PLUTON_INDEX_ID);
    }

    @After
    public void tearDown() throws Exception {
        adminService.deleteIndexes(PLUTON_INDEX_ID);
    }

    @Test
    public void testMethods() throws Exception {
        SearchRequest request = new SearchRequest(PLUTON_INDEX_ID);
        request.source(searchSource().query(matchAllQuery()));

        SearchResponse response = searchClient.search(request, RequestOptions.DEFAULT);
        assertNotNull(response);

        assertEquals(0, getNumDocs(response));

        String xml = getClasspathFileContent("docs/" + IPAD_DOC_ID);
        searchService.index(PLUTON_INDEX_ID, PLUTON_SITE, IPAD_DOC_ID, xml);

        xml = getClasspathFileContent("docs/" + DISABLED_DOC_ID);
        searchService.index(PLUTON_INDEX_ID, PLUTON_SITE, DISABLED_DOC_ID, xml);

        xml = getClasspathFileContent("docs/" + EXPIRED_DOC_ID);
        searchService.index(PLUTON_INDEX_ID, PLUTON_SITE, EXPIRED_DOC_ID, xml);

        Resource file = getClasspathFile("docs/" + WP_REASONS_PDF_DOC_ID);
        searchService.indexBinary(PLUTON_INDEX_ID, PLUTON_SITE, WP_REASONS_PDF_DOC_ID, file);

        searchService.refresh(PLUTON_INDEX_ID);

        response = searchClient.search(request, RequestOptions.DEFAULT);
        assertNotNull(response);

        assertEquals(2, getNumDocs(response));

        Map<String, DocumentContext> docs = getDocs(response);
        DocumentContext ipadDoc = docs.get(IPAD_DOC_ID);
        DocumentContext wpReasonsPdfDoc = docs.get(WP_REASONS_PDF_DOC_ID);

        assertNotNull(ipadDoc);
        assertNotNull(wpReasonsPdfDoc);
        assertIPadDoc(ipadDoc);
        assertWpReasonsPdfDoc(wpReasonsPdfDoc);

        Map<String, Object> additionalFields = Map.of("tags", Map.of("value_smv", WP_REASONS_PDF_TAGS));

        searchService.indexBinary(PLUTON_INDEX_ID, PLUTON_SITE, WP_REASONS_PDF_DOC_ID, file, additionalFields);
        searchService.refresh(PLUTON_INDEX_ID);

        request.source(searchSource().query(matchQuery("localId", WP_REASONS_PDF_DOC_ID)));

        response = searchClient.search(request, RequestOptions.DEFAULT);
        assertNotNull(response);

        assertEquals(1, getNumDocs(response));

        docs = getDocs(response);
        wpReasonsPdfDoc = docs.get(WP_REASONS_PDF_DOC_ID);

        assertNotNull(wpReasonsPdfDoc);
        assertWpReasonsPdfDocWithAdditionalFields(wpReasonsPdfDoc);

        searchService.delete(PLUTON_INDEX_ID, PLUTON_SITE, IPAD_DOC_ID);
        searchService.delete(PLUTON_INDEX_ID, PLUTON_SITE, WP_REASONS_PDF_DOC_ID);
        searchService.refresh(PLUTON_INDEX_ID);

        request = new SearchRequest(PLUTON_INDEX_ID);
        request.source(searchSource().query(matchAllQuery()));

        response = searchClient.search(request, RequestOptions.DEFAULT);
        assertNotNull(response);

        assertEquals(0, getNumDocs(response));
    }

    private Resource getClasspathFile(String path) {
        return new ClassPathResource(path);
    }

    private String getClasspathFileContent(String path) throws IOException {
        return IOUtils.toString(new ClassPathResource(path).getInputStream(), Charset.defaultCharset());
    }

    private long getNumDocs(SearchResponse response) {
        return response.getHits().getTotalHits().value;
    }

    // Parse each doc into JSON to speed up querying fields later
    private Map<String, DocumentContext> getDocs(SearchResponse response) {
        return Stream.of(response.getHits().getHits())
                .collect(toMap(hit -> hit.getSourceAsMap().get("localId").toString(),
                               hit -> JsonPath.parse(hit.getSourceAsString())));
    }

    private void assertIPadDocCommonFields(DocumentContext doc) {
        assertThat(doc, hasJsonPath("crafterPublishedDate", notNullValue()));
        assertThat(doc, hasJsonPath("crafterPublishedDate_dt", notNullValue()));
        assertThat(doc, hasJsonPath("crafterSite", equalTo(PLUTON_SITE)));
        assertThat(doc, hasJsonPath("name_s", equalTo("iPad Air 64GB")));
        assertThat(doc, hasJsonPath("name_t", equalTo("iPad Air 64GB")));
        assertThat(doc, hasJsonPath("description_html",
                equalTo("Apple MH182LL/A iPad Air 9.7-Inch Retina Display 64GB, Wi-Fi (Gold)")));
        assertThat(doc, hasJsonPath("availableDate_dt", equalTo("2014-10-01T00:00:00.000Z")));
        assertThat(doc, hasJsonPath("tags.value_s", equalTo(List.of("Apple", "iPad", "Tablet"))));
    }

    private void assertIPadDoc(DocumentContext doc) {
        assertIPadDocCommonFields(doc);

        assertThat(doc, hasJsonPath("id", equalTo(PLUTON_SITE + ":" + IPAD_DOC_ID)));
        assertThat(doc, hasJsonPath("rootId", equalTo(PLUTON_SITE + ":" + IPAD_DOC_ID)));
        assertThat(doc, hasJsonPath("localId", equalTo(IPAD_DOC_ID)));
        assertThat(doc, hasJsonPath("content-type", equalTo("product")));
        assertThat(doc, hasJsonPath("tags.value_s", equalTo(List.of("Apple", "iPad", "Tablet"))));
        assertThat(doc, hasJsonPath("accessories.item[*].description_html",
                equalTo(List.of("Silicon case with stand for iPad Air 64GB", "Lighting cable for iPad"))));
        assertThat(doc, hasJsonPath("accessories.item[*].name_s", equalTo(List.of("Case", "Lighting Cable"))));
        // wrapping list is required because of how jsonPath returns the results
        assertThat(doc, hasJsonPath("accessories.item[*].colors.color_s",
                equalTo(List.of(List.of("Black", "Blue", "Red")))));
    }

    private void assertWpReasonsPdfDoc(DocumentContext doc) {
        assertThat(doc, hasJsonPath("crafterPublishedDate", notNullValue()));
        assertThat(doc, hasJsonPath("crafterPublishedDate_dt", notNullValue()));
        assertThat(doc, hasJsonPath("crafterSite", equalTo(PLUTON_SITE)));
        assertThat(doc, hasJsonPath("id", equalTo(PLUTON_SITE + ":" + WP_REASONS_PDF_DOC_ID)));
        assertThat(doc, hasJsonPath("rootId", equalTo(PLUTON_SITE + ":" + WP_REASONS_PDF_DOC_ID)));
        assertThat(doc, hasJsonPath("localId", equalTo(WP_REASONS_PDF_DOC_ID)));
        assertThat(doc, hasJsonPath("content", notNullValue()));
    }

    private void assertWpReasonsPdfDocWithAdditionalFields(DocumentContext doc) {
        assertWpReasonsPdfDoc(doc);

        assertThat(doc, hasJsonPath("tags.value_smv", equalTo(WP_REASONS_PDF_TAGS)));
    }

}
