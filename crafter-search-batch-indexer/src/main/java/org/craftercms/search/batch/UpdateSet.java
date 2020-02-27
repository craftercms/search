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

import java.util.List;

/**
 * Set of files to add or delete from the index.
 *
 * @author avasquez
 */
public class UpdateSet extends AbstractUpdateDetailProvider {

    private List<String> updatePaths;
    private List<String> deletePaths;

    public UpdateSet(List<String> updatePaths, List<String> deletePaths) {
        this.updatePaths = updatePaths;
        this.deletePaths = deletePaths;
    }

    public List<String> getUpdatePaths() {
        return updatePaths;
    }

    public List<String> getDeletePaths() {
        return deletePaths;
    }

}
