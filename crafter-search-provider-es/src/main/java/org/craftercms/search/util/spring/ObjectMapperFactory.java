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

package org.craftercms.search.util.spring;

import org.craftercms.commons.jackson.search.CrafterMultivaluedModule;
import org.springframework.beans.factory.FactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Factory class for Jackson {@link ObjectMapper}
 * <p>All instances will be configured to use the {@link CrafterMultivaluedModule}</p>
 * @author joseross
 */
public class ObjectMapperFactory implements FactoryBean<ObjectMapper> {

    /**
     * Indicates if it should return {@link XmlMapper} instances
     */
    protected boolean xml = false;

    public void setXml(final boolean xml) {
        this.xml = xml;
    }

    @Override
    public ObjectMapper getObject() {
        ObjectMapper mapper;
        if(xml) {
            mapper = new XmlMapper();
        } else {
            mapper = new ObjectMapper();
        }
        mapper.registerModule(new CrafterMultivaluedModule());
        return mapper;
    }

    @Override
    public Class<?> getObjectType() {
        return ObjectMapper.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
