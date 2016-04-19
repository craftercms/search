package org.craftercms.search.utils;

import org.apache.solr.client.solrj.SolrServer;

/**
 * Factory used to retrieve an index specific SolrServer.
 *
 * @author avasquez
 */
public interface SolrServerFactory {

    /**
     * Returns a SolrServer specific to an index ID, or a default SolrServer when the ID is null or empty.
     */
    SolrServer getSolrServer(String indexId);

}
