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

package org.craftercms.search.v3.service.internal.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.craftercms.search.v3.service.internal.QueryBuilder;

/**
 * Implementation of {@link QueryBuilder} for the Lucene syntax used by Solr
 * @author joseross
 */
public class SolrQueryBuilder extends AbstractQueryBuilder {

    public static final String OPEN_GROUP_CHAR = "(";
    public static final String CLOSE_GROUP_CHAR = ")";

    public static final String OPERATOR_NOT = "NOT";
    public static final String OPERATOR_AND = "AND";
    public static final String OPERATOR_OR = "OR";

    protected String keywordNow = "NOW";
    protected String keywordMinutes = "MINUTES";
    protected String keywordHours = "HOURS";
    protected String keywordDays = "DAYS";
    protected String keywordMonths = "MONTHS";
    protected String keywordYears = "YEARS";

    public SolrQueryBuilder() {
        super(OPEN_GROUP_CHAR, CLOSE_GROUP_CHAR, OPERATOR_NOT, OPERATOR_AND, OPERATOR_OR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryBuilder contentType(String value) {
        addOperatorIfNeeded();
        append("content-type:%s", quote(value));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder objectId(String value) {
        addOperatorIfNeeded();
        append("objectId:%s", value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder field(String name) {
        addOperatorIfNeeded();
        append("%s:", name);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder matches(Object value) {
        sb.append(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder hasPhrase(String text) {
        sb.append(quote(text));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder hasAny(Object... values) {
        sb.append("(").append(StringUtils.join(values, format(" %s ", orOperator))).append(")");
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder hasAll(Object... values) {
        sb.append("(").append(StringUtils.join(values, format(" %s ", andOperator))).append(")");
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder gt(Object value) {
        append("{%s TO *}", value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder gte(Object value) {
        append("[%s TO *]", value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder lt(Object value) {
        append("{* TO %s}", value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder lte(Object value) {
        append("[* TO %s]", value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder btw(Object start, Object end) {
        append("{%s TO %s}", start, end);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SolrQueryBuilder btwe(Object start, Object end) {
        append("[%s TO %s]", start, end);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryBuilder withBoost(Number value) {
        append("^%s", value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryBuilder withProximity(int value) {
        append("~%s", value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryBuilder withSimilarity(int value) {
        return withProximity(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryBuilder withSimilarity() {
        append("~");
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String regexp(final String pattern) {
        return format("/%s/", pattern);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String boost(Object value, Number boosting) {
        return format("%s^%s", value, boosting);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String quote(Object value) {
        return format("\"%s\"", value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String date(final String value, final String... modifiers) {
        if(ArrayUtils.isNotEmpty(modifiers)) {
            StringBuilder b = new StringBuilder();
            b.append(value);
            for(String modifier : modifiers) {
                b.append(modifier);
            }
            return b.toString();
        } else {
            return value;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String now(final String... modifiers) {
        return date(keywordNow, modifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String plus(final Number amount, final String unit) {
        return format("+%s%s", amount, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String minus(final Number amount, final String unit) {
        return format("-%s%s", amount, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String roundingTo(final String unit) {
        return format("/%s", unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String minutes() {
        return keywordMinutes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String hours() {
        return keywordHours;
    }

    @Override
    public String months() {
        return keywordMonths;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String days() {
        return keywordDays;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String years() {
        return keywordYears;
    }

}