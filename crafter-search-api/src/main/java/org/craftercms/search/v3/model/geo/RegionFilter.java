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

package org.craftercms.search.v3.model.geo;

/**
 * Holds the data needed to filter documents contained in a given region
 * @author joseross
 */
public class RegionFilter extends GeoFilter {

    /**
     * Initial location for the region
     */
    protected Point bottomLeft;

    /**
     * Closing location for the region
     */
    protected Point topRight;

    public RegionFilter() {
    }

    public RegionFilter(final String field, final Point bottomLeft, final Point topRight) {
        super(field);
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    public Point getBottomLeft() {
        return bottomLeft;
    }

    public RegionFilter setBottomLeft(final Point bottomLeft) {
        this.bottomLeft = bottomLeft;
        return this;
    }

    public Point getTopRight() {
        return topRight;
    }

    public RegionFilter setTopRight(final Point topRight) {
        this.topRight = topRight;
        return this;
    }

    @Override
    public String toString() {
        return "RegionFilter{" + "bottomLeft=" + bottomLeft + ", topRight=" + topRight + ", field='" +
            field + '\'' + '}';
    }

}
