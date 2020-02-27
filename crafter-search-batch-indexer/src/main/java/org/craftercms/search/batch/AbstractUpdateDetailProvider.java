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

package org.craftercms.search.batch;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation for {@link UpdateDetailProvider}
 * @author joseross
 */
public class AbstractUpdateDetailProvider implements UpdateDetailProvider {

    /**
     * Holds all known instances of {@link UpdateDetail} mapped by their id
     */
    protected Map<String, UpdateDetail> updateDetails;

    /**
     * Holds all changed files mapped by the {@link UpdateDetail} id
     */
    protected Map<String, String> updateLog;

    public Map<String, UpdateDetail> getUpdateDetails() {
        return updateDetails;
    }

    public void setUpdateDetails(final Map<String, UpdateDetail> updateDetails) {
        this.updateDetails = updateDetails;
    }

    public Map<String, String> getUpdateLog() {
        return updateLog;
    }

    public void setUpdateLog(final Map<String, String> updateLog) {
        this.updateLog = updateLog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UpdateDetail getUpdateDetail(final String file) {
        if(updateDetails != null && updateLog != null) {
            return updateDetails.get(updateLog.get(StringUtils.removeStart(file, "/")));
        } else {
            return null;
        }
    }

}
