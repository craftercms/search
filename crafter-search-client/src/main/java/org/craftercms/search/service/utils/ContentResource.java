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

import java.io.IOException;
import java.io.InputStream;

import org.craftercms.core.service.Content;
import org.springframework.core.io.AbstractResource;

public class ContentResource extends AbstractResource {

    private Content content;
    private String filename;

    public ContentResource(Content content, String filename) {
        this.content = content;
        this.filename = filename;
    }

    @Override
    public String getDescription() {
        return content.toString();
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public long contentLength() throws IOException {
        return content.getLength();
    }

    @Override
    public long lastModified() throws IOException {
        return content.getLastModified();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return content.getInputStream();
    }

}
