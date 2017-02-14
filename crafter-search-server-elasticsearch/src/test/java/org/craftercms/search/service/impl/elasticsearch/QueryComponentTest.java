package org.craftercms.search.service.impl.elasticsearch;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.impl.QueryParams;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.craftercms.search.service.impl.elasticsearch.QueryComponent.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QueryComponentTest {
    private static final String TEST_INDEX = "test_index";
    private static final String QUERY_STRING = "real:true";

    @Mock
    private Client client;
    @InjectMocks
    private QueryComponent queryComponent;

    @Before
    public void setUpForTestIndex() {
        SearchResponse searchResponse = mock(SearchResponse.class);
        SearchHits searchHits = mock(SearchHits.class);
        when(searchResponse.getHits()).thenReturn(searchHits);
        SearchHit[] hits = asHits(makeHitSource1(), makeHitSource2());
        when(searchHits.getHits()).thenReturn(hits);
        SearchRequestBuilder requestBuilder = mock(SearchRequestBuilder.class);
        when(client.prepareSearch(TEST_INDEX)).thenReturn(requestBuilder);
        when(requestBuilder.setQuery(any(QueryBuilder.class))).thenReturn(requestBuilder);
        when(requestBuilder.get()).thenReturn(searchResponse);
    }

    @Before
    public void setUpForAllIndices() {
        SearchResponse searchResponse = mock(SearchResponse.class);
        SearchHits searchHits = mock(SearchHits.class);
        when(searchResponse.getHits()).thenReturn(searchHits);
        SearchHit[] hits = asHits(makeHitSource1(), makeHitSource2(), makeHitSource3());
        when(searchHits.getHits()).thenReturn(hits);
        SearchRequestBuilder requestBuilder = mock(SearchRequestBuilder.class);
        when(client.prepareSearch(ALL_INDICES)).thenReturn(requestBuilder);
        when(requestBuilder.setQuery(any(QueryBuilder.class))).thenReturn(requestBuilder);
        when(requestBuilder.get()).thenReturn(searchResponse);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifySearchWithIndexYieldsExpectedResults() {
        Map<String, Object> results = queryComponent.search(TEST_INDEX, buildQuery());
        verifySearchResults(results, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifySearchAllIndicesYieldsExpectedResults() {
        Map<String, Object> results = queryComponent.search(buildQuery());
        verifySearchResults(results, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifySearchNullIndexSameAsAllIndices() {
        Map<String, Object> results = queryComponent.search(null, buildQuery());
        verifySearchResults(results, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifySearchEmptyIndexNameSameAsAllIndices() {
        Map<String, Object> results = queryComponent.search(StringUtils.EMPTY, buildQuery());
        verifySearchResults(results, 3);
    }

    private static Map<String, Object> makeHitSource1() {
        return makeHitSource("Tester1", true, 42);
    }

    private static Map<String, Object> makeHitSource2() {
        return makeHitSource("Tester2", true, 255);
    }

    private static Map<String, Object> makeHitSource3() {
        return makeHitSource("Tester3", true, 8);
    }

    private static Map<String, Object> makeHitSource(String name, boolean real, int count) {
        Map<String, Object> hit = new HashMap<>();
        hit.put("name", name);
        hit.put("real", real);
        hit.put("count", count);

        return hit;
    }

    private static SearchHit[] asHits(Map<String, Object>... sources) {
        return Stream.of(sources).map(QueryComponentTest::asHit).toArray(SearchHit[]::new);
    }

    private static SearchHit asHit(Map<String, Object> source) {
        SearchHit hit = mock(SearchHit.class);
        when(hit.getSource()).thenReturn(source);

        return hit;
    }

    private Query buildQuery() {
        QueryParams query = new QueryParams();
        query.addParam(QUERY_PARAMETER, QUERY_STRING);

        return query;
    }

    private void verifySearchResults(Map<String, Object> results, int expectedCount) {
        List<Map<String, Object>> documents = (List<Map<String, Object>>) results.get(DOCUMENTS_RESULT);
        assertEquals("There should be two documents.", expectedCount, documents.size());
    }
}