/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.search.opensearch.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.ssl.SSLContexts;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * Implementation of {@link AbstractFactoryBean} to create instances of {@link OpenSearchClient}
 * @author joseross
 * @since 4.0.0
 */
public class OpenSearchClientFactory extends AbstractFactoryBean<OpenSearchClient> {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchClientFactory.class);

    /**
     * List of OpenSearch urls
     */
    protected final String[] serverUrls;

    /**
     * The username for OpenSearch
     */
    protected String username;

    /**
     * The password for OpenSearch
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

    /**
     * Indicates if socket keep alive should be enabled
     */
    protected boolean socketKeepAlive = false;

    @ConstructorProperties({"serverUrls"})
    public OpenSearchClientFactory(final String[] serverUrls) {
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

    public void setSocketKeepAlive(boolean socketKeepAlive) {
        this.socketKeepAlive = socketKeepAlive;
    }

    public static PoolingNHttpClientConnectionManager createConnectionManager(int connectionTimeout, int socketTimeout,
                                                                              int threadCount, boolean socketKeepAlive)
            throws IOReactorException {
        // Setup with everything just as the builder would do it
        SSLContext sslcontext = SSLContexts.createDefault();
        PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.getDefault();
        HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);
        SchemeIOSessionStrategy sslStrategy = new SSLIOSessionStrategy(sslcontext, null, null, hostnameVerifier);

        // Create the custom reactor
        IOReactorConfig.Builder configBuilder = IOReactorConfig.custom();

        if (threadCount > 0) {
            logger.debug("Using custom thread count: {}", threadCount);
            configBuilder.setIoThreadCount(threadCount);
        } else {
            logger.debug("Using default thread count");
        }

        if (connectionTimeout >= 0) {
            logger.debug("Using custom connect timeout: {}", connectionTimeout);
            configBuilder.setConnectTimeout(connectionTimeout);
        } else {
            logger.debug("Using default connect timeout");
        }

        if (socketTimeout >= 0) {
            logger.debug("Using custom socket timeout: {}", socketTimeout);
            configBuilder.setSoTimeout(socketTimeout);
        } else {
            logger.debug("Using default socket timeout");
        }

        if (socketKeepAlive) {
            logger.debug("Using socket keep alive");
            configBuilder.setSoKeepAlive(true);
        }

        DefaultConnectingIOReactor reactor = new DefaultConnectingIOReactor(configBuilder.build());

        // Set up a generic exception handler that just logs everything to prevent the client from shutting down
        reactor.setExceptionHandler(new IOReactorExceptionHandler() {
            @Override
            public boolean handle(IOException e) {
                logger.error("Error executing request", e);
                return true;
            }

            @Override
            public boolean handle(RuntimeException e) {
                logger.error("Error executing request", e);
                return true;
            }
        });

        return new PoolingNHttpClientConnectionManager(
                reactor,
                RegistryBuilder.<SchemeIOSessionStrategy>create()
                        .register("http", NoopIOSessionStrategy.INSTANCE)
                        .register("https", sslStrategy)
                        .build());
    }

    public static OpenSearchClient createClient(String[] serverUrls, String username, String password,
                                                   int connectTimeout, int socketTimeout, int threadCount,
                                                   boolean socketKeepAlive) {
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

            try {
                builder.setConnectionManager(
                        createConnectionManager(connectTimeout, socketTimeout, threadCount, socketKeepAlive));
            } catch (IOReactorException e) {
                logger.warn("Error setting up custom exception handler", e);
            }

            return builder;
        };
        clientBuilder.setRequestConfigCallback(requestConfigCallback);
        clientBuilder.setHttpClientConfigCallback(httpClientConfigCallback);
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OpenSearchTransport transport = new RestClientTransport(clientBuilder.build(),
                                                                    new JacksonJsonpMapper(mapper));
        return new OpenSearchClient(transport);
    }

    @Override
    public Class<?> getObjectType() {
        return OpenSearchClient.class;
    }

    @Override
    protected OpenSearchClient createInstance() {
        return createClient(serverUrls, username, password, connectTimeout, socketTimeout, threadCount,
                socketKeepAlive);
    }

    @Override
    protected void destroyInstance(OpenSearchClient instance) throws Exception {
        instance._transport().close();
    }

}
