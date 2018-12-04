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

package org.craftercms.search.v3.service.internal;

import groovy.lang.Closure;

/**
 * <p>Defines the operations to build a search query</p>
 *
 * <pre>
 *     // Example, build a query to search for articles in the previous month having more than 50 likes and less than
 *     10 dislikes.
 *
 *     QueryBuilder qb = ... // obtain an appropriate implementation
 *     qb
 *         .contentType("/page/article")
 *         .field("likes").gt(50)
 *         .field("dislikes").lt(10)
 *         .field("date_dt").gte( qb.now( qb.minus(1, qb.months()) ) )
 *
 *     return qb.toString() // obtain the resulting query as a string
 * </pre>
 *
 * @author joseross
 */
public interface QueryBuilder {

    /**
     * Search by crafter content type field
     * @param contentType the value to search
     * @return the query builder
     */
    QueryBuilder contentType(String contentType);

    /**
     * Search by crafter  object id field
     * @param objectId the value to search
     * @return the query builder
     */
    QueryBuilder objectId(String objectId);

    /**
     * Search by the given field
     * @param name the field to search
     * @return the query builder
     */
    QueryBuilder field(String name);

    /**
     * Search by the given value
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder matches(Object value);

    /**
     * Search by the given phrase
     * @param text the phrase to search
     * @return the query builder
     */
    QueryBuilder hasPhrase(String text);

    /**
     * Search by all the given values
     * @param values the values to search
     * @return the query builder
     */
    QueryBuilder hasAny(Object... values);

    /**
     * Search by at least one of the given values
     * @param values the values to search
     * @return the query builder
     */
    QueryBuilder hasAll(Object... values);

    /**
     * Search by values greater than the given value
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder gt(Object value);

    /**
     * Search by values greater or equal than the given value
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder gte(Object value);

    /**
     * Search by values less than the given value
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder lt(Object value);

    /**
     * Search by values less or equal than the given value
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder lte(Object value);

    /**
     * Search by values between (excluding) the given limits
     * @param start starting limit
     * @param end ending limit
     * @return the query builder
     */
    QueryBuilder btw(Object start, Object end);

    /**
     * Search by values between (including) the given limits
     * @param start starting limit
     * @param end ending limit
     * @return the query builder
     */
    QueryBuilder btwe(Object start, Object end);

    /**
     * Groups the given statements using the OR operator
     * @param statements the statements to group
     */
    void or(Closure<Void> statements);

    /**
     * Groups the given statements using the AND operator
     * @param statements the statements to group
     */
    void and(Closure<Void> statements);

    /**
     * Negates the given statements using the NOT operator
     * @param statements the statements to group
     */
    void not(Closure<Void> statements);

    /**
     * Adds a boosting with the given value
     * @param value the value for boosting
     */
    void withBoost(Number value);

    /**
     * Adds a proximity search with the given value
     * @param value the maximum proximity for the search
     */
    void withProximity(int value);

    /**
     * Adds a similarity search with the given value
     * @param value the maximum differences for the search
     */
    void withSimilarity(int value);

    /**
     * Adds a similirty search with a default value
     */
    void withSimilarity();

    /**
     * Utility method to build a regular expression search
     * @param pattern the regular expression pattern
     * @return the regular expression search
     */
    String regexp(String pattern);

    /**
     * Utility method to add boosting to a single term
     * @param value the term to boost
     * @param boosting the value for boosting
     * @return the boosted value
     */
    String boost(Object value, Number boosting);

    /**
     * Utility method to quote any value
     * @param value the value to quote
     * @return the quoted value
     */
    String quote(Object value);

    /**
     * Utility method to specify a date expression
     * @param value the base date
     * @param modifiers list of modifiers
     * @return the date expression
     */
    String date(String value, String... modifiers);

    /**
     * Utility method to specify a date expression with the current time
     * @param modifiers list of modifiers
     * @return the date expression
     */
    String now(String... modifiers);

    /**
     * Modifier to add the given amount of time to a date expression
     * @param amount the amount of time
     * @param unit the units of time
     * @return the date modifier
     */
    String plus(Number amount, String unit);

    /**
     * Modifier to substract the given amount of time to a date expression
     * @param amount the amount of time
     * @param unit the units of time
     * @return the date modifier
     */
    String minus(Number amount, String unit);

    /**
     * Modifier to round dates using the given time unit
     * @param unit the unit of time
     * @return the date modifier
     */
    String roundingTo(String unit);

    /**
     * Utility method for the minutes unit keyword
     * @return the minutes keyword
     */
    String minutes();

    /**
     * Utility method for the hours unit keyword
     * @return the hours keyword
     */
    String hours();

    /**
     * Utility method for the months unit keyword
     * @return the months keyword
     */
    String months();

    /**
     * Utility method for the days unit keyword
     * @return the days keyword
     */
    String days();

    /**
     * Utility method for the years unit keyword
     * @return the years keywordß
     */
    String years();

    /**
     * <p>Process a set of Groovy DSL statements. This allows shorter and better looking code:</p>
     *
     * <pre>
     *     def qb = ... // obtain an appropriate instance
     *     qb.addStatements {
     *         contentType "/page/article"
     *         field "likes" gt 50
     *         field "dislikes" lt 10
     *         field "date_dt" gte now( minus(1, months() ) )
     *     }
     *     return qb as String // obtain the resulting query as a string
     * </pre>
     *
     * @param statements set of groovy statements
     * @return the query builder
     */
    QueryBuilder addStatements(Closure<Void> statements);

}
