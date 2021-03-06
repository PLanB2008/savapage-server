/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.server.pages;

import java.math.BigDecimal;
import java.util.List;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.services.ServiceContext;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogAccountTrxAddin extends AbstractAccountTrxAddin {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * URL parameter for the {@link DocLog} database key.
     */
    private static final String PARM_DOCLOG_ID = "docLogId";

    /**
     * @param parameters
     *            The {@link PageParameters}.
     */
    public DocLogAccountTrxAddin(final PageParameters parameters) {
        super(parameters);

        final DocLog docLog = this.getDocLog();
        final List<AccountTrx> trxList;

        final BigDecimal totalAmount;
        final int totalCopies;

        if (docLog == null) {
            trxList = null;
            totalCopies = 0;
            totalAmount = BigDecimal.ZERO;
        } else {
            trxList = docLog.getTransactions();
            totalAmount = docLog.getCostOriginal();
            if (docLog.getDocOut() != null
                    && docLog.getDocOut().getPrintOut() != null) {
                totalCopies =
                        docLog.getDocOut().getPrintOut().getNumberOfCopies();
            } else {
                totalCopies = 1;
            }
        }

        this.populate(totalAmount, totalCopies, trxList, false);
    }

    @Override
    protected String getJobTicketFileName() {
        return null;
    }

    /**
     * Uses {@link #PARM_DOCLOG_ID} to get the {@link DocLog}.
     *
     * @return The {@link DocLog}.
     */
    protected DocLog getDocLog() {
        return ServiceContext.getDaoContext().getDocLogDao()
                .findById(Long.valueOf(this.getParmValue(PARM_DOCLOG_ID)));
    }

}
