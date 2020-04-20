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
package org.savapage.server.api.request;

import java.io.IOException;

import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;

/**
 * Sets User Telegram ID.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqUserSetTelegramID extends ApiRequestMixin {

    /** */
    private static class DtoReq extends AbstractDto {

        private String id;

        public String getId() {
            return id;
        }

        @SuppressWarnings("unused")
        public void setId(String id) {
            this.id = id;
        }
    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final User jpaUser;
        if (lockedUser == null) {
            jpaUser = USER_DAO.findById(this.getSessionUserDbKey());
        } else {
            jpaUser = lockedUser;
        }

        final DtoReq dto = DtoReq.create(DtoReq.class, this.getParmValueDto());

        USER_SERVICE.setUserAttrValue(jpaUser, UserAttrEnum.EXT_TELEGRAM_ID,
                CryptoUser.encryptUserAttr(jpaUser.getId(), dto.getId()));

        this.setApiResultOk();
    }

}
