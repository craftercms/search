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
 * Holds the data needed to filter documents by the distance from a given location
 * @author joseross
 */
public class DistanceFilter extends GeoFilter {

    /**
     * Location to filter
     */
    protected Point point;

    /**
     * Distance from the location
     */
    protected double distance;

    /**
     * Units used for the distance
     */
    protected String units = "km";

    /**
     * Indicates if the filter should use a square instead of a circle
     */
    protected boolean square = false;

    public DistanceFilter() {
    }

    public DistanceFilter(final String field, final double latitude, final double longitude, final double distance) {
        super(field);
        this.point = new Point(latitude, longitude);
        this.distance = distance;
    }

    public Point getPoint() {
        return point;
    }

    public DistanceFilter setPoint(final Point point) {
        this.point = point;
        return this;
    }

    public double getDistance() {
        return distance;
    }

    public DistanceFilter setDistance(final long distance) {
        this.distance = distance;
        return this;
    }

    public String getUnits() {
        return units;
    }

    public DistanceFilter setUnits(final String units) {
        this.units = units;
        return this;
    }

    public boolean isSquare() {
        return square;
    }

    public DistanceFilter setSquare(final boolean square) {
        this.square = square;
        return this;
    }

    @Override
    public String toString() {
        return "DistanceFilter{" + "point=" + point + ", distance=" + distance + ", units='" + units + '\'' + ", "
            + "square=" + square + ", field='" + field + '\'' + '}';
    }

}
