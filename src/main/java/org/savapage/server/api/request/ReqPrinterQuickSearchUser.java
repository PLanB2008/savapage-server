/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request;

import org.savapage.core.SpException;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.client.IppConnectException;
import org.savapage.core.json.JsonPrinterList;

/**
 * Quick Search of Proxy Printers allowed for a requesting User on their
 * Terminal.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterQuickSearchUser
        extends ReqPrinterQuickSearchMixin {

    @Override
    protected JsonPrinterList getPrinterList(final String requestingUser) {
        try {
            return PROXY_PRINT_SERVICE.getUserPrinterList(
                    ApiRequestHelper.getHostTerminal(this.getRemoteAddr()),
                    requestingUser);
        } catch (IppConnectException | IppSyntaxException e) {
            throw new SpException(e.getMessage());
        }
    }

}
