/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.search.utils.spring;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.ClusterStateProvider;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.springframework.beans.factory.FactoryBean;

import java.util.Collections;
import java.util.List;

/**
 * Spring {@code FactoryBean} that uses a {@code CloudSolrClient.Builder} to create an instance of a
 * {@code SolrClient}. The properties of this bean correspond to the builder methods of the {@code Builder}.
 *
 * @author avasquez
 */
public class CloudSolrClientFactoryBean implements FactoryBean<CloudSolrClient> {

    private List<String> zkHosts;
    private List<String> solrUrls;
    private HttpClient httpClient;
    private String zkChroot;
    private LBHttpSolrClient lbHttpSolrClient;
    private LBHttpSolrClient.Builder lbHttpSolrClientBuilder;
    private Boolean sendUpdatesOnlyToShardLeaders;
    private Boolean sendUpdatesToAllReplicasInShard;
    private Boolean sendDirectUpdatesToShardLeadersOnly;
    private Boolean sendDirectUpdatesToAnyShardReplica;
    private ClusterStateProvider clusterStateProvider;

    public void setZkHost(String zkHost) {
        this.zkHosts = Collections.singletonList(zkHost);
    }

    public void setZkHosts(List<String> zkHosts) {
        this.zkHosts = zkHosts;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrls = Collections.singletonList(solrUrl);
    }

    public void setSolrUrls(List<String> solrUrls) {
        this.solrUrls = solrUrls;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setZkChroot(String zkChroot) {
        this.zkChroot = zkChroot;
    }

    public void setLbHttpSolrClient(LBHttpSolrClient lbHttpSolrClient) {
        this.lbHttpSolrClient = lbHttpSolrClient;
    }

    public void setLbHttpSolrClientBuilder(LBHttpSolrClient.Builder lbHttpSolrClientBuilder) {
        this.lbHttpSolrClientBuilder = lbHttpSolrClientBuilder;
    }

    public void setSendUpdatesOnlyToShardLeaders(Boolean sendUpdatesOnlyToShardLeaders) {
        this.sendUpdatesOnlyToShardLeaders = sendUpdatesOnlyToShardLeaders;
    }

    public void setSendUpdatesToAllReplicasInShard(Boolean sendUpdatesToAllReplicasInShard) {
        this.sendUpdatesToAllReplicasInShard = sendUpdatesToAllReplicasInShard;
    }

    public void setSendDirectUpdatesToShardLeadersOnly(Boolean sendDirectUpdatesToShardLeadersOnly) {
        this.sendDirectUpdatesToShardLeadersOnly = sendDirectUpdatesToShardLeadersOnly;
    }

    public void setSendDirectUpdatesToAnyShardReplica(Boolean sendDirectUpdatesToAnyShardReplica) {
        this.sendDirectUpdatesToAnyShardReplica = sendDirectUpdatesToAnyShardReplica;
    }

    public void setClusterStateProvider(ClusterStateProvider clusterStateProvider) {
        this.clusterStateProvider = clusterStateProvider;
    }

    @Override
    @SuppressWarnings("deprecation")
    public CloudSolrClient getObject() throws Exception {
        CloudSolrClient.Builder builder = new CloudSolrClient.Builder();
        if (zkHosts != null) {
            builder.withZkHost(zkHosts);
        }
        if (solrUrls != null) {
            builder.withSolrUrl(solrUrls);
        }
        if (httpClient != null) {
            builder.withHttpClient(httpClient);
        }
        if (zkChroot != null) {
            builder.withZkChroot(zkChroot);
        }
        if (lbHttpSolrClient != null) {
            builder.withLBHttpSolrClient(lbHttpSolrClient);
        }
        if (lbHttpSolrClientBuilder != null) {
            builder.withLBHttpSolrClientBuilder(lbHttpSolrClientBuilder);
        }
        if (sendUpdatesOnlyToShardLeaders != null && sendUpdatesOnlyToShardLeaders) {
            builder.sendUpdatesOnlyToShardLeaders();
        }
        if (sendUpdatesToAllReplicasInShard != null && sendUpdatesToAllReplicasInShard) {
            builder.sendUpdatesToAllReplicasInShard();
        }
        if (sendDirectUpdatesToShardLeadersOnly != null && sendDirectUpdatesToShardLeadersOnly) {
            builder.sendDirectUpdatesToShardLeadersOnly();
        }
        if (sendDirectUpdatesToAnyShardReplica != null && sendDirectUpdatesToAnyShardReplica) {
            builder.sendDirectUpdatesToAnyShardReplica();
        }
        if (clusterStateProvider != null) {
            builder.withClusterStateProvider(clusterStateProvider);
        }

        return builder.build();
    }

    @Override
    public Class<?> getObjectType() {
        return CloudSolrClient.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

}
