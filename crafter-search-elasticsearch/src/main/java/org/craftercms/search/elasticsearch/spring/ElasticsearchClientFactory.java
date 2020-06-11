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
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Factory class for the Elasticsearch rest client
 * @author joseross
 */
public class ElasticsearchClientFactory extends AbstractFactoryBean<RestHighLevelClient> {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchClientFactory.class);

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

    /**
     * The connection timeout in milliseconds
     */
    protected int connectTimeout = -1;

    /**
     * The socket timeout in milliseconds
     */
    protected int socketTimeout = -1;

    /**
     * The number of threads to use
     */
    protected int threadCount = -1;

    public ElasticsearchClientFactory(final String[] serverUrls) {
        this.serverUrls = serverUrls;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public static RestHighLevelClient createClient(String[] serverUrls, String username, String password,
                                                   int connectTimeout, int socketTimeout, int threadCount) {
        logger.debug("Building client for urls: {}", (Object) serverUrls);
        HttpHost[] hosts = Stream.of(serverUrls).map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder clientBuilder = RestClient.builder(hosts);
        RestClientBuilder.RequestConfigCallback requestConfigCallback = builder -> {
            if (connectTimeout >= 0) {
                logger.debug("Using custom connect timeout: {}", connectTimeout);
                builder.setConnectTimeout(connectTimeout);
            } else {
                logger.debug("Using default connect timeout");
            }
            if (socketTimeout >= 0) {
                logger.debug("Using custom socket timeout: {}", socketTimeout);
                builder.setSocketTimeout(socketTimeout);
            } else {
                logger.debug("Using default socket timeout");
            }
            return builder;
        };
        RestClientBuilder.HttpClientConfigCallback httpClientConfigCallback = builder -> {
            if (StringUtils.isNoneEmpty(username, password)) {
                logger.debug("Using basic auth with user: {}", username);
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                builder.setDefaultCredentialsProvider(credentialsProvider);
            } else {
                logger.debug("No credentials provided");
            }
            if (threadCount > 0) {
                logger.debug("Using custom thread count: {}", threadCount);
                builder.setDefaultIOReactorConfig(IOReactorConfig.custom()
                        .setIoThreadCount(threadCount)
                        .build());
            } else {
                logger.debug("Using default thread count");
            }
            return builder;
        };
        clientBuilder.setRequestConfigCallback(requestConfigCallback);
        clientBuilder.setHttpClientConfigCallback(httpClientConfigCallback);
        return new RestHighLevelClient(clientBuilder);
    }

    @Override
    protected RestHighLevelClient createInstance() {
        return createClient(serverUrls, username, password, connectTimeout, socketTimeout, threadCount);
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