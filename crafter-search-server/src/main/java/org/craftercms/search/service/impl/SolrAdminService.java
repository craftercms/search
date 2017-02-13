/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.craftercms.search.exception.SearchException;
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
        CoreAdminRequest.Create create = new CoreAdminRequest.Create();
        create.setCoreName(id);

        if (StringUtils.isNotEmpty(defaultInstanceDir)) {
            create.setInstanceDir(defaultInstanceDir);
        }
        if (StringUtils.isNotEmpty(defaultConfigName)) {
            create.setConfigName(defaultConfigName);
        }
        if (StringUtils.isNotEmpty(defaultSchemaName)) {
            create.setSchemaName(defaultSchemaName);
        }
        if (StringUtils.isNotEmpty(defaultDataDir)) {
            create.setDataDir(defaultDataDir);
        }
        if (StringUtils.isNotEmpty(defaultConfigSet)) {
            create.setConfigSet(defaultConfigSet);
        }

        logger.info("Creating Solr core = " + id + ", instanceDir = " + defaultInstanceDir + ", configName = " + defaultConfigName +
                    ", schemaName = " + defaultSchemaName + ", dataDir = " + defaultDataDir + ", configSet = " + defaultConfigSet);

        try {
            create.process(solrClient);
        } catch (SolrServerException | IOException e) {
            throw new SearchException(id, "Failed to create core", e);
        }
    }

    @Override
    public void deleteIndex(String id, IndexDeleteMode mode) throws SearchException {
        CoreAdminRequest.Unload unload = new CoreAdminRequest.Unload(true);
        unload.setCoreName(id);
        unload.setDeleteDataDir(mode == IndexDeleteMode.ALL_DATA);
        unload.setDeleteInstanceDir(mode == IndexDeleteMode.ALL_DATA_AND_CONFIG);

        logger.info("Deleting Solr core = " + id + ", mode = " + mode);

        try {
            unload.process(solrClient);
        } catch (SolrServerException | IOException e) {
            throw new SearchException(id, "Failed to delete core", e);
        }
    }

}
