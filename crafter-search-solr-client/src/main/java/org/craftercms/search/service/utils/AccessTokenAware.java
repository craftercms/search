/*
 * Copyright (C) 2007-2021 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.search.service.utils;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.craftercms.search.service.utils.RestClientUtils.addParam;

/**
 * Abstract class that holds the data to validate an access token.
 *
 * @author joseross
 * @since 3.1.15
 */
public abstract class AccessTokenAware {

    /**
     * The name of the parameter to check the access token
     */
    protected String accessTokenParameter;

    /**
     * The expected value fo the access token
     */
    protected String accessTokenValue;

    public void setAccessTokenParameter(String accessTokenParameter) {
        this.accessTokenParameter = accessTokenParameter;
    }

    public void setAccessTokenValue(String accessTokenValue) {
        this.accessTokenValue = accessTokenValue;
    }

    protected String addTokenIfNeeded(String url) {
        if (isNotEmpty(accessTokenValue)) {
            return addParam(url, accessTokenParameter, accessTokenValue);
        }
        return url;
    }

}
