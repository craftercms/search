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
package org.craftercms.search;

import org.craftercms.search.v3.service.internal.QueryBuilder;
import org.craftercms.search.v3.service.internal.impl.SolrQueryBuilder;

/**
 * @author joseross
 */
public class QueryBuilderTest {

    public static void main(String... args) {
        QueryBuilder qb = new SolrQueryBuilder();

        qb
            .contentType("/component/test")
            .field("title").matches("important").withSimilarity(1)
            .or(() -> {
                qb.field("featured").matches(true);
                qb.field("likes").gt(50);
            })
            .not(() -> qb.field("expired_dt").lte(qb.now()))
            .field("published_dt").gte(qb.now(qb.minus(1, qb.months())));

        System.out.println(qb);
    }

}
