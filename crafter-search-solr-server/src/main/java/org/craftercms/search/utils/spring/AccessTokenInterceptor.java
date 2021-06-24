/*
 * Copyright (C) 2007-2021 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.search.utils.spring;

import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.ConstructorProperties;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Implementation of {@link org.springframework.web.servlet.HandlerInterceptor} that validates an access token.
 *
 * @author joseross
 * @since 3.1.15
 */
public class AccessTokenInterceptor extends HandlerInterceptorAdapter {

    /**
     * The name of the parameter to check
     */
    protected String parameterName;

    /**
     * The expected value of the token
     */
    protected String accessToken;

    @ConstructorProperties({"parameterName", "accessToken"})
    public AccessTokenInterceptor(String parameterName, String accessToken) {
        this.parameterName = parameterName;
        this.accessToken = accessToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (isNotEmpty(accessToken)) {
            String token = request.getParameter(parameterName);
            if (isEmpty(token)) {
                throw new MissingServletRequestParameterException(parameterName, "string");
            } else if(!accessToken.equals(token)) {
                throw new IllegalArgumentException("Invalid access token");
            }
        }

        return true;
    }

}
