/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.server.pages.admin;

import org.savapage.server.pages.AbstractAuthPage;

/**
 * Abstract page for all pages which need authorized access. The constructor
 * checks the SpSession to see if the user is administrator and authorized to
 * see the pages.
 *
 * @author Datraverse B.V.
 */
public abstract class AbstractAdminPage extends AbstractAuthPage {

    private static final long serialVersionUID = 1L;

    @Override
    protected boolean needMembership() {
        return true;
    }

    public AbstractAdminPage() {

        if (isAuthErrorHandled()) {
            return;
        }
        checkAdminAuthorization();
    }
}