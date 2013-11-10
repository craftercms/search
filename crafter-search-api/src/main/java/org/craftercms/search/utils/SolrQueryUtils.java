package org.craftercms.search.utils;

/**
 * Utility methods for Solr queries.
 *
 * @author Alfonso Vásquez
 */
public class SolrQueryUtils {

    public static final String USER_SEARCH_QUERY_ILLEGAL_CHARS = "():[]{}\\";

    private SolrQueryUtils() {
    }

    /**
     * Escapes characters in a user search query (the search terms the user entered). User search queries shouldn't
     * contain the following characters, which can break the search, without escaping: ( ) : [ ] { } \
     *
     * @param q
     *          the query to escape
     * @return the escaped query
     */
    public static String escapeUserSearchQuery(String q) {
        return escapeChars(q, USER_SEARCH_QUERY_ILLEGAL_CHARS);
    }

    /**
     * Escapes the characters from {@code chars} in the {@code str} parameter.
     *
     * @param str
     *          the string to escape
     * @param chars
     *          the characters to escape
     * @return the escaped string
     */
    public static String escapeChars(String str, String chars) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (chars.indexOf(c) >= 0) {
                sb.append('\\');
            }

            sb.append(c);
        }

        return sb.toString();
    }

}
