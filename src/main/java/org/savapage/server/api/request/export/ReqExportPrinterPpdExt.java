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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.api.request.export;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.time.Duration;
import org.savapage.core.SpException;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.jpa.Printer;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAttrLookup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class ReqExportPrinterPpdExt extends ApiRequestExportMixin {

    /** */
    private final Long printerID;

    /**
     *
     * @param id
     *            The Database key of printer.
     */
    public ReqExportPrinterPpdExt(final Long id) {
        this.printerID = id;
    }

    @Override
    protected final IRequestHandler onExport(final WebAppTypeEnum webAppType,
            final String requestingUser, final RequestCycle requestCycle,
            final PageParameters parameters, final boolean isGetAction,
            final File tempExportFile) {

        // tempExportFile is NOT applicable.

        final PrinterDao printerDao =
                ServiceContext.getDaoContext().getPrinterDao();

        final Printer printer = printerDao.findById(this.printerID);

        /*
         * INVARIANT: printer must exist in database.
         */
        if (printer == null) {
            throw new SpException(
                    "Printer [" + this.printerID + "] not found.");
        }

        final PrinterAttrLookup lookup = new PrinterAttrLookup(printer);
        final String ppdExtName =
                lookup.get(PrinterAttrEnum.CUSTOM_PPD_EXT_FILE);

        /*
         * INVARIANT: PPDE attribute must exist in database.
         */
        if (StringUtils.isBlank(ppdExtName)) {
            throw new SpException("Printer [" + this.printerID
                    + "]: PPD extension not found.");
        }

        final File filePPDExt = ServiceContext.getServiceFactory()
                .getProxyPrintService().getPPDExtFile(ppdExtName);

        /*
         * INVARIANT: PPDE file must exist.
         */
        if (!filePPDExt.exists()) {
            throw new SpException("PPD extension ["
                    + filePPDExt.getAbsolutePath() + "] not found.");
        }
        final FileResourceStream stream = new FileResourceStream(filePPDExt);

        final ResourceStreamRequestHandler handler =
                new ResourceStreamRequestHandler(stream);

        handler.setFileName(ppdExtName);
        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        handler.setCacheDuration(Duration.NONE);

        return handler;
    }
}
