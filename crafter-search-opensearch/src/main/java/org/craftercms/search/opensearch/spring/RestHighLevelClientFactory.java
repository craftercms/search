/*
 * Copyright (C) 2007-2025 Crafter Software Corporation. All Rights Reserved.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.nio.reactor.IOReactorException;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.beans.ConstructorProperties;
import java.util.stream.Stream;

import static org.craftercms.search.opensearch.spring.ClientFactoryUtils.createConnectionManager;

/**
 * Factory class for the OpenSearch rest client
 *
 * @author joseross
 */
public class RestHighLevelClientFactory extends AbstractFactoryBean<RestHighLevelClient> {

	private static final Logger logger = LoggerFactory.getLogger(RestHighLevelClientFactory.class);

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
	/**
	 * The maximum number of connections
	 */
	protected int maxTotalConnections = -1;
	/**
	 * The maximum number of connections per route
	 */
	protected int maxConnectionsPerRoute = -1;

	@ConstructorProperties({"serverUrls"})
	public RestHighLevelClientFactory(final String[] serverUrls) {
		this.serverUrls = serverUrls;
	}

	@SuppressWarnings("unused")
	public void setUsername(final String username) {
		this.username = username;
	}

	@SuppressWarnings("unused")
	public void setPassword(final String password) {
		this.password = password;
	}

	@SuppressWarnings("unused")
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	@SuppressWarnings("unused")
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	@SuppressWarnings("unused")
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	@SuppressWarnings("unused")
	public void setSocketKeepAlive(boolean socketKeepAlive) {
		this.socketKeepAlive = socketKeepAlive;
	}

	@SuppressWarnings("unused")
	public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
		this.maxConnectionsPerRoute = maxConnectionsPerRoute;
	}

	@SuppressWarnings("unused")
	public void setMaxTotalConnections(int maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}

	public static RestHighLevelClient createClient(String[] serverUrls, String username, String password,
												   int connectTimeout, int socketTimeout, int threadCount,
												   boolean socketKeepAlive, int maxTotalConnections, int maxConnectionsPerRoute) {
		logger.debug("Building client for urls: {}", (Object) serverUrls);
		HttpHost[] hosts = Stream.of(serverUrls).map(HttpHost::create).toArray(HttpHost[]::new);
		RestClientBuilder clientBuilder = RestClient.builder(hosts);
		RestClientBuilder.RequestConfigCallback requestConfigCallback = builder -> {
			if (connectTimeout >= 0) {
				logger.debug("Using custom connect timeout: {}", connectTimeout);
				builder.setConnectTimeout(connectTimeout);
				builder.setConnectionRequestTimeout(connectTimeout);
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
						createConnectionManager(connectTimeout, socketTimeout, threadCount, socketKeepAlive, maxTotalConnections, maxConnectionsPerRoute));
			} catch (IOReactorException e) {
				logger.warn("Error setting up custom exception handler", e);
			}

			return builder;
		};
		clientBuilder.setRequestConfigCallback(requestConfigCallback);
		clientBuilder.setHttpClientConfigCallback(httpClientConfigCallback);
		return new RestHighLevelClient(clientBuilder);
	}

	@Override
	protected RestHighLevelClient createInstance() {
		return createClient(serverUrls, username, password, connectTimeout, socketTimeout, threadCount,
				socketKeepAlive, maxTotalConnections, maxConnectionsPerRoute);
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
