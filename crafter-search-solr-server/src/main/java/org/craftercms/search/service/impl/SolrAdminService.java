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
package org.craftercms.search.service.impl;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.util.NamedList;
import org.craftercms.search.exception.IndexNotFoundException;
import org.craftercms.search.exception.SearchException;
import org.craftercms.search.exception.SearchServerException;
import org.craftercms.search.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 2/3/17.
 */
public class SolrAdminService implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(SolrAdminService.class);

    /**
     * The Solr client used to execute requests against a Solr server.
     */
    protected SolrClient solrClient;
    /**
     * The default directory where Solr config and data will be stored.
     */
    protected String defaultInstanceDir;
    /**
     * The default Solr config filename.
     */
    protected String defaultConfigName;
    /**
     * The default Solr schema filename.
     */
    protected String defaultSchemaName;
    /**
     * The default data directory.
     */
    protected String defaultDataDir;
    /**
     * The default name of the config set.
     */
    protected String defaultConfigSet;

    /**
     * Sets the Solr client used to execute requests against a Solr server.
     */
    @Required
    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public void setDefaultInstanceDir(String defaultInstanceDir) {
        this.defaultInstanceDir = defaultInstanceDir;
    }

    public void setDefaultConfigName(String defaultConfigName) {
        this.defaultConfigName = defaultConfigName;
    }

    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    public void setDefaultDataDir(String defaultDataDir) {
        this.defaultDataDir = defaultDataDir;
    }

    public void setDefaultConfigSet(String defaultConfigSet) {
        this.defaultConfigSet = defaultConfigSet;
    }

    @Override
    public void createIndex(String id) throws SearchException {
        CoreAdminRequest.Create request = new CoreAdminRequest.Create();
        request.setCoreName(id);

        if (StringUtils.isNotEmpty(defaultInstanceDir)) {
            request.setInstanceDir(defaultInstanceDir);
        }
        if (StringUtils.isNotEmpty(defaultConfigName)) {
            request.setConfigName(defaultConfigName);
        }
        if (StringUtils.isNotEmpty(defaultSchemaName)) {
            request.setSchemaName(defaultSchemaName);
        }
        if (StringUtils.isNotEmpty(defaultDataDir)) {
            request.setDataDir(defaultDataDir);
        }
        if (StringUtils.isNotEmpty(defaultConfigSet)) {
            request.setConfigSet(defaultConfigSet);
        }

        logger.info("Creating Solr core = " + id + ", instanceDir = " + defaultInstanceDir + ", configName = " + defaultConfigName +
                    ", schemaName = " + defaultSchemaName + ", dataDir = " + defaultDataDir + ", configSet = " + defaultConfigSet);

        try {
            request.process(solrClient);
        } catch (SolrServerException | IOException e) {
            throw new SearchServerException(id, "Unable to create core", e);
        } catch (Exception e) {
            throw new SearchException(id, "Failed to create core", e);
        }
    }

    @Override
    public Map<String, Object> getIndexInfo(String id) throws SearchException {
        CoreAdminRequest request = new CoreAdminRequest();
        request.setCoreName(id);
        request.setIndexInfoNeeded(true);
        request.setAction(CoreAdminParams.CoreAdminAction.STATUS);

        try {
            CoreAdminResponse response = request.process(solrClient);
            Map<String, Object> info = null;

            if (response != null) {
                NamedList<Object> status = response.getCoreStatus(id);
                if (status != null) {
                    info = status.asShallowMap();
                }
            }

            if (MapUtils.isNotEmpty(info)) {
                return info;
            } else {
                throw new IndexNotFoundException("Index '" + id + "' not found");
            }
        } catch (SolrServerException | IOException e) {
            throw new SearchServerException(id, "Unable to get core info", e);
        } catch (Exception e) {
            throw new SearchException(id, "Failed to get core info", e);
        }
    }

    @Override
    public void deleteIndex(String id, IndexDeleteMode mode) throws SearchException {
        CoreAdminRequest.Unload request = new CoreAdminRequest.Unload(true);
        request.setCoreName(id);
        request.setDeleteDataDir(mode == IndexDeleteMode.ALL_DATA);
        request.setDeleteInstanceDir(mode == IndexDeleteMode.ALL_DATA_AND_CONFIG);

        logger.info("Deleting Solr core = " + id + ", mode = " + mode);

        try {
            request.process(solrClient);
        } catch (SolrServerException | IOException e) {
            throw new SearchServerException(id, "Unable to delete core", e);
        } catch (Exception e) {
            throw new SearchException(id, "Failed to delete core", e);
        }
    }

}
