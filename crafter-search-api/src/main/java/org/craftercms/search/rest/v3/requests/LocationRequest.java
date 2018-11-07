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

package org.craftercms.search.rest.v3.requests;

import java.util.LinkedList;
import java.util.List;

import org.craftercms.search.v3.model.geo.DistanceFilter;
import org.craftercms.search.v3.model.geo.Point;
import org.craftercms.search.v3.model.geo.RegionFilter;

/**
 * Holds the data needed to filter documents using geo-spatial information during a search operation
 * @author joseross
 */
public class LocationRequest {

    /**
     * List of {@link DistanceFilter}s
     */
    List<DistanceFilter> distances = new LinkedList<>();

    /**
     * List of {@link RegionFilter}s
     */
    List<RegionFilter> regions = new LinkedList<>();

    public List<DistanceFilter> getDistances() {
        return distances;
    }

    public LocationRequest setDistances(final List<DistanceFilter> distances) {
        this.distances = distances;
        return this;
    }

    public LocationRequest near(String field, double latitude, double longitude, double distance) {
        this.distances.add(new DistanceFilter(field, latitude, longitude, distance));
        return this;
    }

    public List<RegionFilter> getRegions() {
        return regions;
    }

    public LocationRequest setRegions(final List<RegionFilter> regions) {
        this.regions = regions;
        return this;
    }

    public LocationRequest within(String field, double upperLat, double upperLon, double lowerLat, double lowerLon) {
        regions.add(new RegionFilter(field, new Point(upperLat, upperLon), new Point(lowerLat, lowerLon)));
        return this;
    }

    @Override
    public String toString() {
        return "LocationRequest{" + "distances=" + distances + ", regions=" + regions + '}';
    }

}
