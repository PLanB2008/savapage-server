/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dto.ProxyPrinterDto;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.services.ServiceContext;

/**
 * Sets the (basic) properties of a Proxy {@link Printer}.
 * <p>
 * Also, a logical delete can be applied or reversed.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterSet extends ApiRequestMixin {

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final ProxyPrinterDto dto = JsonAbstractBase
                .create(ProxyPrinterDto.class, this.getParmValueDto());

        final long id = dto.getId();

        final Printer jpaPrinter = printerDao.findById(id);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (jpaPrinter == null) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-printer-not-found",
                    String.valueOf(id));
            return;
        }

        /*
         * INVARIANT: PPD extension file MUST exist.
         */
        if (StringUtils.isNotBlank(dto.getPpdExtFile())) {
            final File filePPDExt =
                    PROXY_PRINT_SERVICE.getPPDExtFile(dto.getPpdExtFile());
            if (!filePPDExt.exists()) {
                setApiResult(ApiResultCodeEnum.ERROR, "msg-file-not-present",
                        filePPDExt.getAbsolutePath());
                return;
            }
        }

        /*
         * Job Ticket Printer.
         */
        if (BooleanUtils.isTrue(dto.getJobTicket())) {

            /*
             * INVARIANT: printer MUST NOT be PaperCut managed.
             */
            if (PAPERCUT_SERVICE
                    .isExtPaperCutPrint(jpaPrinter.getPrinterName())) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-jobticket-papercut-not-allowed");
                return;
            }

            /*
             * INVARIANT: An existing printer group MUST be specified.
             */
            final String value = dto.getJobTicketGroup();

            if (StringUtils.isBlank(value)) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-jobticket-group-not-entered");
                return;
            }

            final PrinterGroup jpaPrinterGroup = ServiceContext.getDaoContext()
                    .getPrinterGroupDao().findByName(value);

            if (jpaPrinterGroup == null) {
                setApiResult(ApiResultCodeEnum.ERROR,
                        "msg-jobticket-group-not-found", value);
                return;
            }

        }

        PROXY_PRINT_SERVICE.setProxyPrinterProps(jpaPrinter, dto);

        setApiResult(ApiResultCodeEnum.OK, "msg-printer-saved-ok");
    }

}
