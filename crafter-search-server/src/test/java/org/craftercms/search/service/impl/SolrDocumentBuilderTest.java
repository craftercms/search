package org.craftercms.search.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * Created by alfonsovasquez on 9/2/16.
 */
public class SolrDocumentBuilderTest {

    private static final String SITE = "test";
    private static final String IPAD_ID = "/products/ipad.xml";
    private static final String TAB_ID = "/products/galaxyTab.xml";

    private static final String SITE_FIELD_NAME = "crafterSite";
    private static final String LOCAL_ID_FIELD_NAME = "localId";

    private SolrDocumentBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new SolrDocumentBuilder();
        builder.setSiteFieldName(SITE_FIELD_NAME);
        builder.setLocalIdFieldName(LOCAL_ID_FIELD_NAME);
        builder.setFieldValueConverter(createFieldValueConverter());
        builder.setSingleToMultiValueSuffixMappings(createSingleToMultiValueSuffixMappings());
    }

    @Test
    public void testBuildForXml() throws Exception {
        String xml = IOUtils.toString((new ClassPathResource("/docs/ipad.xml")).getInputStream());

        SolrInputDocument doc = builder.build(SITE, IPAD_ID, xml, true);

        assertNotNull(doc);
        assertNull(doc.getFieldValue("code"));
        assertEquals(SITE, doc.getFieldValue(SITE_FIELD_NAME));
        assertEquals(SITE + ":" + IPAD_ID, doc.getFieldValue("id"));
        assertEquals(IPAD_ID, doc.getFieldValue(LOCAL_ID_FIELD_NAME));
        assertEquals("iPad Air 64GB", doc.getFieldValue("name"));
        assertEquals("Apple MH182LL/A iPad Air 9.7-Inch Retina Display 64GB, Wi-Fi (Gold)",
                     doc.getFieldValue("description_html").toString().trim());
        assertEquals("2014-10-01T00:00:00.000Z", doc.getFieldValue("availableDate_dt"));
        assertEquals(Arrays.asList("Apple", "iPad", "Tablet"), doc.getFieldValues("tags.value_smv"));
    }

    @Test
    public void testBuildForFields() throws Exception {
        MultiValueMap<String, String> fields = new LinkedMultiValueMap<>(3);
        fields.add("name", "Samsung Galaxy Tab 4");
        fields.add("description_html", "<p>Samsung Galaxy Tab 4 (7-Inch, White)</p>");
        fields.add("availableDate_dt", "04/01/2014 11:00:00");
        fields.add("tags.value_smv", "Samsung");
        fields.add("tags.value_smv", "Galaxy");
        fields.add("tags.value_smv", "Tablet");

        SolrInputDocument doc = builder.build(SITE, TAB_ID, fields);

        assertNotNull(doc);
        assertEquals(SITE, doc.getFieldValue(SITE_FIELD_NAME));
        assertEquals(SITE + ":" + TAB_ID, doc.getFieldValue("id"));
        assertEquals(TAB_ID, doc.getFieldValue(LOCAL_ID_FIELD_NAME));
        assertEquals("Samsung Galaxy Tab 4", doc.getFieldValue("name"));
        assertEquals("Samsung Galaxy Tab 4 (7-Inch, White)", doc.getFieldValue("description_html").toString().trim());
        assertEquals("2014-04-01T11:00:00.000Z", doc.getFieldValue("availableDate_dt"));
        assertEquals(Arrays.asList("Samsung", "Galaxy", "Tablet"), doc.getFieldValues("tags.value_smv"));
    }

    @Test
    public void testParamsBuild() throws Exception {
        MultiValueMap<String, String> fields = new LinkedMultiValueMap<>(3);
        fields.add("name", "Samsung Galaxy Tab 4");
        fields.add("description_html", "<p>Samsung Galaxy Tab 4 (7-Inch, White)</p>");
        fields.add("availableDate_dt", "04/01/2014 11:00:00");
        fields.add("tags.value_smv", "Samsung");
        fields.add("tags.value_smv", "Galaxy");
        fields.add("tags.value_smv", "Tablet");

        String prefix = "p.";
        String suffix = ".s";

        ModifiableSolrParams params = builder.buildParams(SITE, TAB_ID, prefix, suffix, fields);

        assertNotNull(params);
        assertEquals(SITE, params.get(prefix + SITE_FIELD_NAME + suffix));
        assertEquals(SITE + ":" + TAB_ID, params.get(prefix + "id" + suffix));
        assertEquals(TAB_ID, params.get(prefix + LOCAL_ID_FIELD_NAME + suffix));
        assertEquals("Samsung Galaxy Tab 4", params.get(prefix + "name" + suffix));
        assertEquals("Samsung Galaxy Tab 4 (7-Inch, White)",
                     params.get(prefix + "description_html" + suffix).trim());
        assertEquals("2014-04-01T11:00:00.000Z", params.get(prefix + "availableDate_dt" + suffix));
        assertArrayEquals(new String[] {"Samsung", "Galaxy", "Tablet"},
                          params.getParams(prefix + "tags.value_smv" + suffix));
    }

    private FieldValueConverter createFieldValueConverter() {
        HtmlStrippingConverter htmlConverter = new HtmlStrippingConverter();

        DateTimeConverter dateTimeConverter = new DateTimeConverter();
        dateTimeConverter.setDateTimeFieldPattern("MM/dd/yyyy HH:mm:ss");
        dateTimeConverter.init();

        Map<String, FieldValueConverter> mappings = new HashMap<>(2);
        mappings.put("_html", htmlConverter);
        mappings.put("_dt", dateTimeConverter);

        CompositeSuffixBasedConverter compositeConverter = new CompositeSuffixBasedConverter();
        compositeConverter.setConverterMappings(mappings);

        return compositeConverter;
    }

    private Map<String, String> createSingleToMultiValueSuffixMappings() {
        Map<String, String> mappings = new HashMap<>(2);
        mappings.put("_s", "_smv");

        return mappings;
    }

}
