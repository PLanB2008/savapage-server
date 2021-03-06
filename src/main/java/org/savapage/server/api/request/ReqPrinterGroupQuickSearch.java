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
package org.savapage.server.api.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.savapage.core.dao.PrinterGroupDao;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchFilterDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.services.ServiceContext;

/**
 * Printer Group Quick Search.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqPrinterGroupQuickSearch extends ApiRequestMixin {

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DtoRsp extends AbstractDto {

        private List<QuickSearchItemDto> items;

        @SuppressWarnings("unused")
        public List<QuickSearchItemDto> getItems() {
            return items;
        }

        public void setItems(List<QuickSearchItemDto> items) {
            this.items = items;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final PrinterGroupDao dao =
                ServiceContext.getDaoContext().getPrinterGroupDao();

        final QuickSearchFilterDto dto = AbstractDto
                .create(QuickSearchFilterDto.class, this.getParmValueDto());

        final PrinterGroupDao.ListFilter filter =
                new PrinterGroupDao.ListFilter();

        filter.setContainingNameText(dto.getFilter());

        final List<QuickSearchItemDto> list = new ArrayList<>();

        QuickSearchItemDto itemWlk;

        for (final PrinterGroup group : dao.getListChunk(filter, 0,
                dto.getMaxResults(), true)) {

            itemWlk = new QuickSearchItemDto();
            itemWlk.setKey(group.getId());
            if (group.getGroupName().equalsIgnoreCase(group.getDisplayName())) {
                itemWlk.setText(group.getDisplayName());
            } else {
                itemWlk.setText(group.getGroupName().toUpperCase());
            }
            list.add(itemWlk);
        }

        //
        final DtoRsp rsp = new DtoRsp();
        rsp.setItems(list);

        setResponse(rsp);
        setApiResultOk();
    }

}
