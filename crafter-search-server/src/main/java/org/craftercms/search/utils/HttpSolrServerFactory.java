package org.craftercms.search.utils;

import java.util.Set;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.craftercms.commons.lang.UrlUtils;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link SolrServerFactory} that creates an HttpSolrServer with the specified properties for
 * each index ID.
 *
 * @author avasquez
 */
public class HttpSolrServerFactory implements SolrServerFactory {

    protected String baseUrl;
    protected HttpClient httpClient;
    protected ResponseParser parser;
    protected Boolean allowCompression;
    protected Integer defaultMaxConnectionsPerHost;
    protected Integer maxTotalConnections;
    protected Boolean useMultiPartPost;
    protected Boolean followRedirects;
    protected Integer maxRetries;
    protected Set<String> queryParams;
    protected RequestWriter requestWriter;
    protected Integer soTimeout;

    private HttpSolrServer defaultSolrServer;

    @Required
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setParser(ResponseParser parser) {
        this.parser = parser;
    }

    public void setAllowCompression(Boolean allowCompression) {
        this.allowCompression = allowCompression;
    }

    public void setDefaultMaxConnectionsPerHost(Integer defaultMaxConnectionsPerHost) {
        this.defaultMaxConnectionsPerHost = defaultMaxConnectionsPerHost;
    }

    public void setMaxTotalConnections(Integer maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public void setUseMultiPartPost(Boolean useMultiPartPost) {
        this.useMultiPartPost = useMultiPartPost;
    }

    public void setFollowRedirects(Boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setQueryParams(Set<String> queryParams) {
        this.queryParams = queryParams;
    }

    public void setRequestWriter(RequestWriter requestWriter) {
        this.requestWriter = requestWriter;
    }

    public void setSoTimeout(Integer soTimeout) {
        this.soTimeout = soTimeout;
    }

    @PostConstruct
    public void init() {
        defaultSolrServer = createSolrServer(baseUrl);
    }

    @Override
    public SolrServer getSolrServer(String indexId) {
        if (StringUtils.isNotEmpty(indexId)) {
            return createSolrServer(UrlUtils.concat(baseUrl, indexId));
        } else {
            return defaultSolrServer;
        }
    }

    protected HttpSolrServer createSolrServer(String finalBaseUrl) {
        HttpSolrServer solrServer;

        if (httpClient != null) {
            solrServer = new HttpSolrServer(finalBaseUrl, httpClient);
        } else {
            solrServer = new HttpSolrServer(finalBaseUrl);
        }

        if (parser != null) {
            solrServer.setParser(parser);
        }
        if (allowCompression != null) {
            solrServer.setAllowCompression(allowCompression);
        }
        if (defaultMaxConnectionsPerHost != null) {
            solrServer.setDefaultMaxConnectionsPerHost(defaultMaxConnectionsPerHost);
        }
        if (maxTotalConnections != null) {
            solrServer.setMaxTotalConnections(maxTotalConnections);
        }
        if (useMultiPartPost != null) {
            solrServer.setUseMultiPartPost(useMultiPartPost);
        }
        if (followRedirects != null) {
            solrServer.setFollowRedirects(followRedirects);
        }
        if (maxRetries != null) {
            solrServer.setMaxRetries(maxRetries);
        }
        if (queryParams != null) {
            solrServer.setQueryParams(queryParams);
        }
        if (requestWriter != null) {
            solrServer.setRequestWriter(requestWriter);
        }
        if (soTimeout != null) {
            solrServer.setSoTimeout(soTimeout);
        }

        return solrServer;
    }

}
