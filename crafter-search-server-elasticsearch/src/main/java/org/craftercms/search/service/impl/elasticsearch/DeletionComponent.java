package org.craftercms.search.service.impl.elasticsearch;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class DeletionComponent {
    private final Client client;

    @Autowired
    DeletionComponent(Client client) {
        this.client = client;
    }

    DeleteResponse delete(String indexId, String type, String id) {
        return client.prepareDelete(indexId, type, id).get();
    }
}