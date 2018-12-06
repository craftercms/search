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
 *     // Build a query to search for articles in the last month having more than 50 likes and less than 10 dislikes.
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

    // Crafter specific fields

    /**
     * Search by crafter content type field
     *
     * <pre>
     *     qb.contentType("/page/article")
     * </pre>
     *
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder contentType(String value);

    /**
     * Search by crafter  object id field
     *
     * <pre>
     *     qb.objectId("1fac09ed-d854-49d8-8ab8-398dcd1991ec")
     * </pre>
     *
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder objectId(String value);

    // General fields and matching

    /**
     * Search in the given field
     *
     * <pre>
     *     qb.field("title")...
     * </pre>
     *
     * @param name the field to search
     * @return the query builder
     */
    QueryBuilder field(String name);

    /**
     * Matches the given value
     *
     * <pre>
     *     qb.field("featured").matches(true);
     * </pre>
     *
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder matches(Object value);

    /**
     * Matches the givien phrase
     *
     * <pre>
     *     qb.field("summary").hasPhrase("welcome home");
     * </pre>
     *
     * @param text the phrase to search
     * @return the query builder
     */
    QueryBuilder hasPhrase(String text);

    /**
     * Matches at least one of the given values
     *
     * <pre>
     *     qb.field("categories").hasAny("catA", "catB", "catC");
     * </pre>
     *
     * @param values the values to search
     * @return the query builder
     */
    QueryBuilder hasAny(Object... values);

    /**
     * Matches all of the given values
     *
     * <pre>
     *     qb.field("categories").hasAll("catA", "catB", "catC");
     * </pre>
     *
     * @param values the values to search
     * @return the query builder
     */
    QueryBuilder hasAll(Object... values);

    /**
     * Matches values greater than the given value
     *
     * <pre>
     *     qb.field("likes").gt(50);
     * </pre>
     *
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder gt(Object value);

    /**
     * Matches values greater or equal than the given value
     *
     * <pre>
     *     qb.field("likes").gte(50);
     * </pre>
     *
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder gte(Object value);

    /**
     * Matches values less than the given value
     *
     * <pre>
     *     qb.field("dislikes").lt(10);
     * </pre>
     *
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder lt(Object value);

    /**
     * Matches values less or equal than the given value
     *
     * <pre>
     *     qb.field("dislikes").lte(10);
     * </pre>
     *
     * @param value the value to search
     * @return the query builder
     */
    QueryBuilder lte(Object value);

    /**
     * Matches values between (excluding) the given limits
     *
     * <pre>
     *     qb.field("age").btw(20, 30);
     * </pre>
     *
     * @param start starting limit
     * @param end ending limit
     * @return the query builder
     */
    QueryBuilder btw(Object start, Object end);

    /**
     * Matches values between (including) the given limits
     *
     * <pre>
     *     qb.field("age").btwe(20, 30);
     * </pre>
     *
     * @param start starting limit
     * @param end ending limit
     * @return the query builder
     */
    QueryBuilder btwe(Object start, Object end);

    // Grouping operators

    /**
     * <p>Groups the given statements using the AND operator</p>
     *
     * <pre>
     *     qb.and(() -> {
     *         qb.field("featured").matches(true);
     *         qb.field("views").gt(100);
     *     });
     * </pre>
     *
     * @param statements the statements
     * @return the query builder
     */
    QueryBuilder and(Runnable statements);

    /**
     * <p>Groups the given statements using the OR operator</p>
     *
     * <pre>
     *     qb.or(() -> {
     *         qb.field("category").matches("movies");
     *         qb.field("subject").hasPhrase("watch movies");
     *     });
     * </pre>
     *
     * @param statements the statements
     * @return the query builder
     */
    QueryBuilder or(Runnable statements);

    /**
     * <p>Negates and groups the given statements using the default operator</p>
     *
     * <pre>
     *     qb.not(() -> {
     *         qb.field("flags").gt(50);
     *         qb.field("dislikes").gt(50);
     *     });
     * </pre>
     *
     * @param statements the statements
     * @return the query builder
     */
    QueryBuilder not(Runnable statements);

    // Utility methods

    /**
     * Adds a boosting with the given value
     *
     * <pre>
     *     qb.field("features").matches(true).withBoost(2);
     * </pre>
     *
     * @param value the value for boosting
     */
    QueryBuilder withBoost(Number value);

    /**
     * Adds a proximity search with the given value
     *
     * <pre>
     *     qb.field("summary").hasPhrase("welcome home").withProximity(5);
     * </pre>
     *
     * @param value the maximum proximity for the search
     */
    QueryBuilder withProximity(int value);

    /**
     * Adds a similarity search with the given value
     *
     * <pre>
     *     qb.field("summary").matches("book").withSimilarity(1);
     * </pre>
     *
     * @param value the maximum differences for the search
     */
    QueryBuilder withSimilarity(int value);

    /**
     * Adds a similarity search with the default value used by the search provider
     *
     * <pre>
     *     qb.field("summary").matches("book").withSimilarity();
     * </pre>
     *
     */
    QueryBuilder withSimilarity();

    /**
     * Utility method to build a regular expression search
     *
     * <pre>
     *     qb.field("full_name").matches(qb.regexp("john .*"));
     * </pre>
     *
     * @param pattern the regular expression pattern
     * @return the regular expression search
     */
    String regexp(String pattern);

    /**
     * Utility method to add boosting to a single term
     *
     * <pre>
     *     qb.field("categories").hasAny("catA", boost("catB", 2));
     * </pre>
     *
     * @param value the term to boost
     * @param boosting the value for boosting
     * @return the boosted value
     */
    String boost(Object value, Number boosting);

    /**
     * Utility method to quote any value
     *
     * <pre>
     *     qb.field("url").matches(quote("/value"));
     * </pre>
     *
     * @param value the value to quote
     * @return the quoted value
     */
    String quote(Object value);

    // Date expressions

    /**
     * Utility method to specify a date expression
     *
     * <pre>
     *     qb.field("date_dt").lt(qb.date("2008-09-15T15:53:00"));
     * </pre>
     *
     * @param value the base date
     * @param modifiers list of modifiers
     * @return the date expression
     */
    String date(String value, String... modifiers);

    /**
     * Utility method to specify a date expression with the current time
     *
     * <pre>
     *     qb.field("date_dt").gt(qb.now());
     * </pre>
     *
     * @param modifiers list of modifiers
     * @return the date expression
     */
    String now(String... modifiers);

    /**
     * Modifier to add the given amount of time to a date expression
     *
     * <pre>
     *     qb.field("date_dt").gt(qb.now(qb.plus(1, qb.months())));
     * </pre>
     *
     * @param amount the amount of time
     * @param unit the units of time
     * @return the date modifier
     */
    String plus(Number amount, String unit);

    /**
     * Modifier to substract the given amount of time to a date expression
     *
     * <pre>
     *     qb.field("date_dt").lt(qb.now(qb.minus(15, qb.days())));
     * </pre>
     *
     * @param amount the amount of time
     * @param unit the units of time
     * @return the date modifier
     */
    String minus(Number amount, String unit);

    /**
     * Modifier to round dates using the given time unit
     *
     * qb.field("date_dt").gt(qb.now(qb.roundingTo(qb.months())));
     *
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

    // Groovy DSL integration

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
     * @see
     * <a href="http://docs.groovy-lang.org/docs/latest/html/documentation/core-domain-specific-languages.html"
     *    target="blank">
     *     Groovt DSL
     * </a>
     *
     * @param statements set of groovy statements
     * @return the query builder
     */
    QueryBuilder addStatements(Closure<Void> statements);

    /**
     * <p>Groups the given Groovy DSL statements using the OR operator</p>
     *
     * <pre>
     *     or {
     *         field "category" matches "movies"
     *         field "subject" hasPhrase "watch movies"
     *     }
     * </pre>
     *
     * @param statements the statements to group
     */
    void or(Closure<Void> statements);

    /**
     * <p>Groups the given Groovy DSL statements using the AND operator</p>
     *
     * <pre>
     *     and {
     *          field "featured" matches true
     *          field "views" gt 100
     *     }
     * </pre>
     *
     * @param statements the statements to group
     */
    void and(Closure<Void> statements);

    /**
     * <p>Negates and groups the given Groovy DSL statements using the default operator</p>
     *
     * <pre>
     *     not {
     *         field "flags" gt 50
     *         field "dislikes" gt 50
     *     }
     * </pre>
     *
     * @param statements the statements to group
     */
    void not(Closure<Void> statements);

}
