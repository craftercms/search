/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All rights reserved.
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
package org.craftercms.search.service.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.lang.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


public class RestClientUtils {

    private static final Logger logger = LoggerFactory.getLogger(RestClientUtils.class);

    private RestClientUtils() {
    }

    public static String addParam(String url, String name, Object value) {
        try {
            return UrlUtils.addParam(url, name, value.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen, UTF-8 is a valid encoding
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void addAdditionalFieldsToMultiPartRequest(Map<String, List<String>> additionalFields, MultiValueMap<String, Object> parts,
                                                             String[] nonAdditionalFields, String multiValueSeparator) {
        if (MapUtils.isNotEmpty(additionalFields)) {
            for (Map.Entry<String, List<String>> additionalField : additionalFields.entrySet()) {
                String fieldName = additionalField.getKey();
                if (ArrayUtils.contains(nonAdditionalFields, fieldName)) {
                    logger.info("Ignoring non-additional field {}", fieldName);
                } else {
                    if (StringUtils.isEmpty(multiValueSeparator)) {
                        parts.put(fieldName, (List)additionalField.getValue());
                    } else {
                        parts.add(fieldName, StringUtils.join(additionalField.getValue(), multiValueSeparator));
                    }
                }
            }
        }
    }

    public static RestTemplate createRestTemplate(Charset charset) {
        RestTemplate restTemplate = new RestTemplate();

        // Set charset of the String part converter of the FormHttpMessageConverter.
        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof FormHttpMessageConverter) {
                StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(charset);
                stringHttpMessageConverter.setWriteAcceptCharset(false);

                List<HttpMessageConverter<?>> partConverters = new ArrayList<>();
                partConverters.add(new ByteArrayHttpMessageConverter());
                partConverters.add(stringHttpMessageConverter);
                partConverters.add(new ResourceHttpMessageConverter());

                ((FormHttpMessageConverter) converter).setPartConverters(partConverters);
            }
        }

        return restTemplate;
    }

}
