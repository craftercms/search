package org.craftercms.search.service.impl.elasticsearch;

import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class UpdateComponent {
    private final Client client;

    @Autowired
    UpdateComponent(Client client) {
        this.client = client;
    }

    UpdateResponse update(String indexId, String type, String id, String json) {
        return client.prepareUpdate(indexId, type, id).setDoc(json).get();
    }
}
