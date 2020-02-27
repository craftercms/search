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

package org.craftercms.search.elasticsearch.spring;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Factory class for the Elasticsearch rest client
 * @author joseross
 */
public class ElasticsearchClientFactory extends AbstractFactoryBean<RestHighLevelClient> {

    /**
     * List of Elasticsearch urls
     */
    protected String[] serverUrls;

    /**
     * The username for Elasticsearch
     */
    protected String username;

    /**
     * The password for Elasticsearch
     */
    protected String password;

    public ElasticsearchClientFactory(final String[] serverUrls) {
        this.serverUrls = serverUrls;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    protected RestHighLevelClient createInstance() {
        HttpHost[] hosts = Stream.of(serverUrls).map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder clientBuilder = RestClient.builder(hosts);
        if (StringUtils.isNoneEmpty(username, password)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            clientBuilder
                .setHttpClientConfigCallback(builder -> builder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return new RestHighLevelClient(clientBuilder);
    }

    @Override
    protected void destroyInstance(final RestHighLevelClient instance) throws Exception {
        instance.close();
    }

    @Override
    public Class<?> getObjectType() {
        return RestHighLevelClient.class;
    }

}