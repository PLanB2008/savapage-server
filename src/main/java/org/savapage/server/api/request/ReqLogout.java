/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.io.IOException;

import org.savapage.core.dto.AbstractDto;
import org.savapage.core.jpa.User;
import org.savapage.core.msg.UserMsgIndicator;
import org.savapage.server.SpSession;
import org.savapage.server.auth.ClientAppUserAuthManager;
import org.savapage.server.auth.WebAppUserAuthManager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Logs out by replacing the underlying (Web)Session, invalidating the current
 * one and creating a new one. Also sends the
 * {@link UserMsgIndicator.Msg#STOP_POLL_REQ} message.
 *
 * @author Rijk Ravestein
 *
 */
public final class ReqLogout extends ApiRequestMixin {

    /**
     * .
     */
    @JsonInclude(Include.NON_NULL)
    private static class DtoReq extends AbstractDto {

        private String authToken;

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

    }

    @Override
    protected void onRequest(final String requestingUser, final User lockedUser)
            throws IOException {

        final DtoReq dtoReq = DtoReq.create(DtoReq.class, getParmValue("dto"));

        ClientAppUserAuthManager.removeUserAuthToken(this.getRemoteAddr());

        WebAppUserAuthManager.instance().removeUserAuthToken(
                dtoReq.getAuthToken(), this.getSessionWebAppType());

        ApiRequestHelper.stopReplaceSession(SpSession.get(), requestingUser,
                this.getRemoteAddr());

        setApiResultOk();
    }

}
