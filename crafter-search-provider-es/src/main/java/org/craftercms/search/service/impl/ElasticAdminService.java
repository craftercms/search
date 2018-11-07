/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.search.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.exception.SearchServerException;
import org.craftercms.search.service.AdminService;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

/**
 * Implementation of {@link AdminService} for ElasticSearch
 * @author joseross
 */
public class ElasticAdminService implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticAdminService.class);

    /**
     * File containing the settings for new indexes
     */
    protected Resource indexSettings;

    /**
     * ElasticSearch rest client
     */
    protected RestHighLevelClient client;

    @Required
    public void setIndexSettings(final Resource indexSettings) {
        this.indexSettings = indexSettings;
    }

    @Required
    public void setClient(final RestHighLevelClient client) {
        this.client = client;
    }

    public void destroy() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            // do nothing...
        }
    }

    @Override
    public void createIndex(final String id) throws SearchException {
        CreateIndexRequest request = new CreateIndexRequest(id);
        try (InputStream is = indexSettings.getInputStream()) {
            request.source(IOUtils.toString(is, Charset.defaultCharset()), XContentType.JSON);
            CreateIndexResponse response = client.indices().create(request);
            logger.debug("Creation of index {} result: {}", id, response.isAcknowledged());
        } catch (IOException e) {
            logger.error("Unable to create index " + id, e);
            throw new SearchServerException(id, "Unable to create index " + id, e);
        } catch (Exception e) {
            logger.error("Creation of index  " + id + " failed", e);
            throw new SearchException(id, "Creation of index " + id + " failed", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getIndexInfo(final String id) throws SearchException {
        GetIndexRequest request = new GetIndexRequest().indices(id);
        try {
            GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);
            Map<String, Object> map = new HashMap<>();
            Settings settings = response.getSettings().get(id);
            for(String name : settings.keySet()) {
                map.put(name, settings.get(name));
            }
            return map;
        } catch (IOException e) {
            logger.error("Unable to get infor for index " + id, e);
            throw new SearchServerException(id, "Unable to get info for index " + id, e);
        } catch (Exception e) {
            logger.error("Get info for index  " + id + " failed", e);
            throw new SearchException(id, "Get info for index " + id + " failed", e);
        }
    }

    @Override
    public void deleteIndex(final String id, final IndexDeleteMode mode) throws SearchException {
        DeleteIndexRequest request = new DeleteIndexRequest(id);
        try {
            AcknowledgedResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
            logger.debug("Deletion of index {} result: {}", id, response.isAcknowledged());
        } catch (IOException e) {
            logger.error("Unable to delete index " + id, e);
            throw new SearchServerException(id, "Unable to delete index " + id, e);
        } catch (Exception e) {
            logger.error("Delete for index " + id + " failed", e);
            throw new SearchException(id, "Delete for index " + id + " failed", e);
        }
    }

}