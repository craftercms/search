package org.craftercms.search.service.impl.elasticsearch;

import org.craftercms.search.service.impl.QueryParams;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static org.craftercms.search.service.impl.elasticsearch.QueryComponent.DOCUMENTS_RESULT;
import static org.craftercms.search.service.impl.elasticsearch.QueryComponent.QUERY_PARAMETER;

public class QueryComponent_IT {
    private Client client;
    private QueryComponent queryComponent;

    @Before
    public void setUp() throws UnknownHostException {
        client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress(
                new InetSocketTransportAddress(InetAddress.getLocalHost(), 9300));
        queryComponent = new QueryComponent(client);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test() {
        QueryParams query = new QueryParams();
        query.addParam(QUERY_PARAMETER, "real:true");
        Map<String, Object> result = queryComponent.search("test_index", query);
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.get(DOCUMENTS_RESULT);
        System.out.println("Size: " + documents.size());
        documents.stream().forEach(System.out::println);
    }
}