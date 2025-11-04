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

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.psl.PublicSuffixMatcher;
import org.apache.hc.client5.http.psl.PublicSuffixMatcherLoader;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.core5.function.Callback;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import static org.apache.hc.core5.util.Timeout.ofMilliseconds;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Helper class for OpenSearch client factories
 */
public class ClientFactoryUtils {
	private static final Logger logger = getLogger(ClientFactoryUtils.class);

	/**
	 * Creates a {@link AsyncClientConnectionManager} with the given parameters
	 *
	 * @param connectionTimeout      the connection timeout in milliseconds
	 * @param maxTotalConnections    the maximum number of total connections
	 * @param maxConnectionsPerRoute the maximum number of connections per route
	 * @return the created {@link AsyncClientConnectionManager}
	 */
	public static PoolingAsyncClientConnectionManager createConnectionManager(int connectionTimeout, int maxTotalConnections, int maxConnectionsPerRoute) {
		// Setup with everything just as the builder would do it
		SSLContext sslcontext = SSLContexts.createDefault();
		PublicSuffixMatcher publicSuffixMatcher = PublicSuffixMatcherLoader.getDefault();
		HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(publicSuffixMatcher);
		TlsStrategy sslStrategy = new ClientTlsStrategyBuilder()
				.setSslContext(sslcontext)
				.setHostnameVerifier(hostnameVerifier)
				.buildAsync();

		ConnectionConfig.Builder connectionConfigBuilder = ConnectionConfig.custom();

		if (connectionTimeout > 0) {
			logger.debug("Using custom connect timeout: {}", connectionTimeout);
			connectionConfigBuilder.setConnectTimeout(ofMilliseconds(connectionTimeout));
		} else {
			logger.debug("Using default connect timeout");
		}

		PoolingAsyncClientConnectionManager poolingNHttpClientConnectionManager = new PoolingAsyncClientConnectionManager(
				RegistryBuilder.<TlsStrategy>create()
						.register("https", sslStrategy)
						.build());

		poolingNHttpClientConnectionManager.setDefaultConnectionConfig(connectionConfigBuilder.build());

		if (maxTotalConnections > 0) {
			logger.debug("Using custom max total connections: {}", maxTotalConnections);
			poolingNHttpClientConnectionManager.setMaxTotal(maxTotalConnections);
		} else {
			logger.debug("Using default max total connections");
		}
		if (maxConnectionsPerRoute > 0) {
			logger.debug("Using custom max connections per route: {}", maxConnectionsPerRoute);
			poolingNHttpClientConnectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
		} else {
			logger.debug("Using default max connections per route");
		}
		return poolingNHttpClientConnectionManager;
	}

	/**
	 * Creates an {@link IOReactorConfig} with the given parameters
	 *
	 * @param socketTimeout   the socket timeout in milliseconds
	 * @param threadCount     the number of IO threads to use
	 * @param socketKeepAlive whether to enable socket keep-alive
	 * @return the created {@link IOReactorConfig}
	 */
	public static IOReactorConfig createIOReactorConfig(int socketTimeout,
														int threadCount, boolean socketKeepAlive) {
		IOReactorConfig.Builder configBuilder = IOReactorConfig.custom();
		if (threadCount > 0) {
			logger.debug("Using custom thread count: {}", threadCount);
			configBuilder.setIoThreadCount(threadCount);
		} else {
			logger.debug("Using default thread count");
		}
		if (socketTimeout > 0) {
			logger.debug("Using custom socket timeout: {}", socketTimeout);
			configBuilder.setSoTimeout(ofMilliseconds(socketTimeout));
		} else {
			logger.debug("Using default socket timeout");
		}
		if (socketKeepAlive) {
			logger.debug("Using socket keep alive");
			configBuilder.setSoKeepAlive(true);
		}

		return configBuilder.build();
	}

	/**
	 * Creates a callback to handle IO reactor exceptions
	 *
	 * @return the created {@link Callback}
	 */
	public static Callback<Exception> createIOReactorExceptionCallback() {
		return e -> logger.error("Error executing request", e);

	}
}
