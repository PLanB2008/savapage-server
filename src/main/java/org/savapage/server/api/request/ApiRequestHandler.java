/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
 * Authors: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import java.util.Map;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface ApiRequestHandler {

    /**
     * Processes an JSON API request.
     *
     * @param parameters
     *            The {@link PageParameters}.
     * @param isGetAction
     *            {@code true} when this is an HTML GET request.
     * @param requestingUser
     *            The user if of the requesting user.
     * @param lockedUser
     *            The locked {@link User} instance, can be {@code null}.
     * @return The response.
     * @throws Exception
     *             When an unexpected error is encountered.
     */
    Map<String, Object> process(PageParameters parameters, boolean isGetAction,
            String requestingUser, User lockedUser) throws Exception;

}
