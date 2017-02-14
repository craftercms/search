package org.craftercms.search.service.impl.elasticsearch;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.impl.QueryParams;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

@Component
class QueryComponent {
    static final String QUERY_PARAMETER = "q";
    static final String ALL_INDICES = "_all";
    static final String DOCUMENTS_RESULT = "documents";

    private final Client client;

    @Autowired
    QueryComponent(Client client) {
        this.client = client;
    }

    Map<String, Object> search(Query query) {
        return search(ALL_INDICES, query);
    }

    Map<String, Object> search(String indexId, Query query) {
        String finalIndexId = StringUtils.isNoneBlank(indexId) ? indexId : ALL_INDICES;
        QueryParams queryParams = (QueryParams) query;
        String queryString = String.join(StringUtils.EMPTY, queryParams.getParam(QUERY_PARAMETER));
        QueryBuilder queryBuilder = queryStringQuery(queryString);
        SearchResponse response = client.prepareSearch(finalIndexId).setQuery(queryBuilder).get();
        Map<String, Object> result = new HashMap<>();
        result.put(DOCUMENTS_RESULT, Stream.of(response.getHits().getHits()).map(hit -> hit.getSource()).collect(Collectors.toList()));

        return result;
    }
}