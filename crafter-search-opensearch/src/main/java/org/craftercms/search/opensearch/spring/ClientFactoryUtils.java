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

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcher;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Helper class for OpenSearch client factories
 */
public class ClientFactoryUtils {
	private static final Logger logger = getLogger(ClientFactoryUtils.class);

	/**
	 * Creates a {@link PoolingNHttpClientConnectionManager} with the given parameters
	 *
	 * @param connectionTimeout      the connection timeout in milliseconds
	 * @param socketTimeout          the socket timeout in milliseconds
	 * @param threadCount            the number of IO threads to use
	 * @param socketKeepAlive        whether to enable socket keep-alive
	 * @param maxTotalConnections    the maximum number of total connections
	 * @param maxConnectionsPerRoute the maximum number of connections per route
	 * @return the created {@link PoolingNHttpClientConnectionManager}
	 * @throws IOReactorException if there is an error creating the IO reactor
	 */
	public static PoolingNHttpClientConnectionManager createConnectionManager(int connectionTimeout, int socketTimeout,
																			  int threadCount, boolean socketKeepAlive,
																			  int maxTotalConnections, int maxConnectionsPerRoute)
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

		if (connectionTimeout > 0) {
			logger.debug("Using custom connect timeout: {}", connectionTimeout);
			configBuilder.setConnectTimeout(connectionTimeout);
		} else {
			logger.debug("Using default connect timeout");
		}

		if (socketTimeout > 0) {
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

		PoolingNHttpClientConnectionManager poolingNHttpClientConnectionManager = new PoolingNHttpClientConnectionManager(
				reactor,
				RegistryBuilder.<SchemeIOSessionStrategy>create()
						.register("http", NoopIOSessionStrategy.INSTANCE)
						.register("https", sslStrategy)
						.build());
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
}
