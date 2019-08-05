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

import java.io.IOException;

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.jpa.User;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * User quick search by Card number.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserCardQuickSearch extends ApiRequestMixin {

    /**
     * .
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private String card;

        public String getCard() {
            return card;
        }

        @SuppressWarnings("unused")
        public void setCard(String card) {
            this.card = card;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dto =
                AbstractDto.create(DtoReq.class, this.getParmValueDto());

        final User user = USER_SERVICE.findUserByCardNumber(dto.getCard());

        if (user == null) {
            setApiResult(ApiResultCodeEnum.ERROR, "msg-usercard-unknown");
            return;
        }

        final QuickSearchItemDto item = new QuickSearchItemDto();

        item.setKey(user.getId());
        item.setText(user.getUserId());

        setResponse(item);
        setApiResultOk();
    }

}