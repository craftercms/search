/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.search.elasticsearch.batch;

import java.util.Map;

import org.craftercms.search.batch.impl.AbstractContentTypeMetadataBatchIndexer;
import org.craftercms.search.elasticsearch.ElasticSearchService;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author joseross
 */
public class ElasticSearchContentTypeMetadataBatchIndexer extends AbstractContentTypeMetadataBatchIndexer {

    protected ElasticSearchService elasticSearchService;

    @Required
    public void setElasticSearchService(final ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @Override
    protected void index(final String indexName, final String siteName, final String path,
                         final Map<String, Object> data) {
        elasticSearchService.index(indexName, siteName, path, data);
    }

    @Override
    protected Map<String, Object> getCurrentData(final String indexId, final String siteName, final String path) {
        return elasticSearchService.searchId(indexId, path);
    }

}
