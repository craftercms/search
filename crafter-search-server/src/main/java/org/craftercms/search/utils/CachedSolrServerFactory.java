package org.craftercms.search.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.solr.client.solrj.SolrServer;
import org.springframework.beans.factory.annotation.Required;

/**
 * {@link SolrServerFactory} decorator that caches SolrServers by index ID.
 *
 * @author avasquez
 */
public class CachedSolrServerFactory implements SolrServerFactory {

    protected SolrServerFactory actualFactory;
    protected Map<String, SolrServer> cache;

    public CachedSolrServerFactory() {
        this.cache = new ConcurrentHashMap<>();
    }

    @Required
    public void setActualFactory(SolrServerFactory actualFactory) {
        this.actualFactory = actualFactory;
    }

    @Override
    public SolrServer getSolrServer(String indexId) {
        SolrServer solrServer;

        if (indexId == null) {
            indexId = "";
        }

        if (!cache.containsKey(indexId)) {
            synchronized (cache) {
                if (!cache.containsKey(indexId)) {
                    solrServer = actualFactory.getSolrServer(indexId);
                    cache.put(indexId, solrServer);
                } else {
                    solrServer = cache.get(indexId);
                }
            }
        } else {
            solrServer = cache.get(indexId);
        }

        return solrServer;
    }

}
