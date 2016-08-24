package org.craftercms.search.service;

/**
 * Converts an incoming string field value to the actual value that will be indexed.
 *
 * @author avasquez
 */
public interface FieldValueConverter {

    Object convert(String name, String value);

}
