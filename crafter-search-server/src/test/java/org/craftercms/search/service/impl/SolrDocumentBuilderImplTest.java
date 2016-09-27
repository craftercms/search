package org.craftercms.search.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.craftercms.search.service.ElementParserService;
import org.craftercms.search.service.FieldValueConverter;
import org.craftercms.search.service.SolrDocumentPostProcessor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_ID_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_PUBLISHING_DATE_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_LOCAL_ID_FIELD_NAME;
import static org.craftercms.search.service.impl.SolrDocumentBuilderImpl.DEFAULT_SITE_FIELD_NAME;
import static org.craftercms.search.service.impl.SubDocumentElementParser.DEFAULT_CONTENT_TYPE_FIELD_NAME;
import static org.craftercms.search.service.impl.SubDocumentElementParser.DEFAULT_PARENT_ID_FIELD_NAME;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link SolrDocumentBuilderImpl}.
 *
 * @author avasquez
 */
public class SolrDocumentBuilderImplTest {

    private static final String SITE = "test";
    private static final String IPAD_ID = "/products/ipad.xml";
    private static final String IPAD_ID_ACCESSORIES0 = IPAD_ID + "_accessories_0";
    private static final String IPAD_ID_ACCESSORIES1 = IPAD_ID + "_accessories_1";
    private static final String TAB_ID = "/products/galaxyTab.xml";

    private SolrDocumentBuilderImpl builder;

    @Before
    public void setUp() throws Exception {
        FieldValueConverter fieldValueConverter = createFieldValueConverter();
        ElementParserService parserService = createElementParserService(fieldValueConverter);
        List<SolrDocumentPostProcessor> postProcessors = createPostProcessors();

        builder = new SolrDocumentBuilderImpl();
        builder.setSiteFieldName(DEFAULT_SITE_FIELD_NAME);
        builder.setLocalIdFieldName(DEFAULT_LOCAL_ID_FIELD_NAME);
        builder.setFieldValueConverter(fieldValueConverter);
        builder.setParserService(parserService);
        builder.setPostProcessors(postProcessors);
    }

    @Test
    public void testBuildForXml() throws Exception {
        String xml = IOUtils.toString((new ClassPathResource("/docs/ipad.xml")).getInputStream());

        SolrInputDocument doc = builder.build(SITE, IPAD_ID, xml, true);

        assertNotNull(doc);
        assertEquals(14, doc.size());
        assertNull(doc.getFieldValue("code"));
        assertNotNull(doc.getFieldValue(DEFAULT_PUBLISHING_DATE_FIELD_NAME));
        assertNotNull(doc.getFieldValue(DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME));
        assertEquals(SITE, doc.getFieldValue(DEFAULT_SITE_FIELD_NAME));
        assertEquals(SITE + ":" + IPAD_ID, doc.getFieldValue(DEFAULT_ID_FIELD_NAME));
        assertEquals(IPAD_ID, doc.getFieldValue(DEFAULT_LOCAL_ID_FIELD_NAME));
        assertEquals("product", doc.getFieldValue(DEFAULT_CONTENT_TYPE_FIELD_NAME));
        assertEquals("iPad Air 64GB", doc.getFieldValue("name_s"));
        assertEquals("iPad Air 64GB", doc.getFieldValue("name_t"));
        assertEquals("Apple MH182LL/A iPad Air 9.7-Inch Retina Display 64GB, Wi-Fi (Gold)",
                     doc.getFieldValue("description_html").toString().trim());
        assertEquals("2014-10-01T00:00:00.000Z", doc.getFieldValue("availableDate_dt"));
        assertEquals(Arrays.asList("Apple", "iPad", "Tablet"), doc.getFieldValues("tags.value_smv"));
        assertEquals(Arrays.asList("Silicon case with stand for iPad Air 64GB", "Lighting cable for iPad"),
                     trimValues(doc.getFieldValues("accessories.item.description_html")));
        assertEquals(Arrays.asList("Case", "Lighting Cable"), doc.getFieldValues("accessories.item.name_smv"));
        assertEquals(Arrays.asList("Black", "Blue", "Red"), doc.getFieldValues("accessories.item.colors.color_smv"));

        // Asset sub-docs
        List<SolrInputDocument> subDocs = doc.getChildDocuments();

        assertNotNull(subDocs);
        assertEquals(2, subDocs.size());

        // Assert first doc
        SolrInputDocument subDoc1 = subDocs.get(0);

        assertEquals(15, subDoc1.size());
        assertNotNull(doc.getFieldValue(DEFAULT_PUBLISHING_DATE_FIELD_NAME));
        assertNotNull(doc.getFieldValue(DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME));
        assertEquals(SITE, subDoc1.getFieldValue(DEFAULT_SITE_FIELD_NAME));
        assertEquals(SITE + ":" + IPAD_ID_ACCESSORIES0, subDoc1.getFieldValue(DEFAULT_ID_FIELD_NAME));
        assertEquals(IPAD_ID_ACCESSORIES0, subDoc1.getFieldValue(DEFAULT_LOCAL_ID_FIELD_NAME));
        assertEquals(SITE + ":" + IPAD_ID, subDoc1.getFieldValue(DEFAULT_PARENT_ID_FIELD_NAME));
        assertEquals("product_accessories", subDoc1.getFieldValue(DEFAULT_CONTENT_TYPE_FIELD_NAME));
        assertEquals("Case", subDoc1.getFieldValue("accessories.item.name_s"));
        assertEquals("Silicon case with stand for iPad Air 64GB",
                     subDoc1.getFieldValue("accessories.item.description_html").toString().trim());
        assertEquals(Arrays.asList("Black", "Blue", "Red"),
                     subDoc1.getFieldValues("accessories.item.colors.color_smv"));
        assertEquals("iPad Air 64GB", subDoc1.getFieldValue("name_s"));
        assertEquals("iPad Air 64GB", subDoc1.getFieldValue("name_t"));
        assertEquals("Apple MH182LL/A iPad Air 9.7-Inch Retina Display 64GB, Wi-Fi (Gold)",
                     subDoc1.getFieldValue("description_html").toString().trim());
        assertEquals("2014-10-01T00:00:00.000Z", subDoc1.getFieldValue("availableDate_dt"));
        assertEquals(Arrays.asList("Apple", "iPad", "Tablet"), subDoc1.getFieldValues("tags.value_smv"));

        // Assert second doc
        SolrInputDocument subDoc2 = subDocs.get(1);

        assertEquals(14, subDoc2.size());
        assertNotNull(doc.getFieldValue(DEFAULT_PUBLISHING_DATE_FIELD_NAME));
        assertNotNull(doc.getFieldValue(DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME));
        assertEquals(SITE, subDoc2.getFieldValue(DEFAULT_SITE_FIELD_NAME));
        assertEquals(SITE + ":" + IPAD_ID_ACCESSORIES1, subDoc2.getFieldValue(DEFAULT_ID_FIELD_NAME));
        assertEquals(IPAD_ID_ACCESSORIES1, subDoc2.getFieldValue(DEFAULT_LOCAL_ID_FIELD_NAME));
        assertEquals(SITE + ":" + IPAD_ID, subDoc2.getFieldValue(DEFAULT_PARENT_ID_FIELD_NAME));
        assertEquals("product_accessories", subDoc2.getFieldValue(DEFAULT_CONTENT_TYPE_FIELD_NAME));
        assertEquals("Lighting Cable", subDoc2.getFieldValue("accessories.item.name_s"));
        assertEquals("Lighting cable for iPad",
                     subDoc2.getFieldValue("accessories.item.description_html").toString().trim());
        assertEquals("iPad Air 64GB", subDoc2.getFieldValue("name_s"));
        assertEquals("iPad Air 64GB", subDoc2.getFieldValue("name_t"));
        assertEquals("Apple MH182LL/A iPad Air 9.7-Inch Retina Display 64GB, Wi-Fi (Gold)",
                     subDoc2.getFieldValue("description_html").toString().trim());
        assertEquals("2014-10-01T00:00:00.000Z", subDoc2.getFieldValue("availableDate_dt"));
        assertEquals(Arrays.asList("Apple", "iPad", "Tablet"), subDoc2.getFieldValues("tags.value_smv"));
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
        assertEquals(9, doc.size());
        assertNotNull(doc.getFieldValue(DEFAULT_PUBLISHING_DATE_FIELD_NAME));
        assertNotNull(doc.getFieldValue(DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME));
        assertEquals(SITE, doc.getFieldValue(DEFAULT_SITE_FIELD_NAME));
        assertEquals(SITE + ":" + TAB_ID, doc.getFieldValue("id"));
        assertEquals(TAB_ID, doc.getFieldValue(DEFAULT_LOCAL_ID_FIELD_NAME));
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
        assertNotNull(params.get(prefix + DEFAULT_PUBLISHING_DATE_FIELD_NAME + suffix));
        assertNotNull(params.get(prefix + DEFAULT_PUBLISHING_DATE_ALT_FIELD_NAME + suffix));
        assertEquals(SITE, params.get(prefix + DEFAULT_SITE_FIELD_NAME + suffix));
        assertEquals(SITE + ":" + TAB_ID, params.get(prefix + "id" + suffix));
        assertEquals(TAB_ID, params.get(prefix + DEFAULT_LOCAL_ID_FIELD_NAME + suffix));
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

        Map<String, FieldValueConverter> mappings = new HashMap<>(2);
        mappings.put("_html", htmlConverter);
        mappings.put("_dt", dateTimeConverter);

        CompositeSuffixBasedConverter compositeConverter = new CompositeSuffixBasedConverter();
        compositeConverter.setConverterMappings(mappings);

        return compositeConverter;
    }

    private ElementParserService createElementParserService(FieldValueConverter fieldValueConverter) {
        ElementParserServiceImpl parserService = new ElementParserServiceImpl();

        TokenizedElementParser tokenizedElementParser = new TokenizedElementParser();

        SubDocumentElementParser subDocElementParser = new SubDocumentElementParser();
        subDocElementParser.setSiteFieldName(DEFAULT_SITE_FIELD_NAME);
        subDocElementParser.setLocalIdFieldName(DEFAULT_LOCAL_ID_FIELD_NAME);

        DefaultElementParser defaultElementParser = new DefaultElementParser();
        defaultElementParser.setFieldValueConverter(fieldValueConverter);

        parserService.setParsers(Arrays.asList(tokenizedElementParser, subDocElementParser, defaultElementParser));

        return parserService;
    }

    private List<SolrDocumentPostProcessor> createPostProcessors() {
        DenormalizingPostProcessor denormalizingPostProcessor = new DenormalizingPostProcessor();
        RenameFieldsIfMultiValuePostProcessor renamePostProcessor = new RenameFieldsIfMultiValuePostProcessor();

        renamePostProcessor.setSingleToMultiValueSuffixMappings(createSingleToMultiValueSuffixMappings());

        return Arrays.asList(denormalizingPostProcessor, renamePostProcessor);
    }

    private Map<String, String> createSingleToMultiValueSuffixMappings() {
        Map<String, String> mappings = new HashMap<>(2);
        mappings.put("_s", "_smv");

        return mappings;
    }

    private Collection<String> trimValues(Collection<Object> values) {
        List<String> trimmedValues = new ArrayList<>(values.size());
        for (Object value : values) {
            trimmedValues.add(value.toString().trim());
        }

        return trimmedValues;
    }

}
