package org.craftercms.search.service.impl.elasticsearch;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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

import static org.junit.Assert.assertEquals;

public class ElasticsearchSearchService_IT {
    private Client client;
    private ElasticsearchSearchService service;
    private String index;

    @Before
    public void setUp() throws UnknownHostException {
        client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress(
                new InetSocketTransportAddress(InetAddress.getLocalHost(), 9300));
        QueryComponent queryComponent = new QueryComponent(client);
        DeletionComponent deletionComponent = new DeletionComponent(client);
        UpdateComponent updateComponent = new UpdateComponent(client);
        service = new ElasticsearchSearchService(queryComponent, deletionComponent, updateComponent);
        index = "test_index_" + StringUtils.lowerCase(RandomStringUtils.randomAlphanumeric(5));
        client.admin().indices().prepareCreate(index).get();
    }

    @After
    public void tearDown() {
        client.admin().indices().prepareDelete(index).get();
        client.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEndToEnd() {
        service.update(index, "site", "id1", createJson("Test", true, 42), false);
        service.update(index, "site", "id2", createJson("Test2", false, 51), false);
        service.update(index, "site", "id3", createJson("Test3", true, 255), false);
        refreshIndex();
        QueryParams query = new QueryParams();
        query.addParam("q", "real:true");
        Map<String, Object> result = service.search(index, query);
        List<Map<String, Object>> documents = (List<Map<String, Object>>) result.get("documents");
        assertEquals(3, documents.size());
        service.delete(index, "site", "id1");
        refreshIndex();
        result = service.search(index, query);
        documents = (List<Map<String, Object>>) result.get("documents");
        assertEquals(2, documents.size());
    }

    private void refreshIndex() {
        client.admin().indices().prepareRefresh(index).get();
    }

    private static String createJson(String name, boolean real, int count) {
        return "{\"name\":\""+ name + "\",\"real\":" + real + ",\"count\":" + count + "}";
    }
}