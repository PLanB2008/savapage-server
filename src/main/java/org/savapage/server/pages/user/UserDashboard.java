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
package org.savapage.server.pages.user;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserDashboard extends AbstractUserPage {

    /**
     * .
     */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The parameters.
     */
    public UserDashboard(final PageParameters parameters) {

        super(parameters);

        final UserIdDto authUser = SpSession.get().getUserIdDto();

        final org.savapage.core.jpa.User jpaUser = ServiceContext
                .getDaoContext().getUserDao().findById(authUser.getDbKey());

        final boolean canResetPassword;

        if (authUser.isInternalUser()) {

            canResetPassword = ConfigManager.instance()
                    .isConfigValue(Key.INTERNAL_USERS_CAN_CHANGE_PW)
                    && USER_SERVICE.hasInternalPassword(jpaUser);

        } else {
            canResetPassword = false;
        }

        final MarkupHelper helper = new MarkupHelper(this);
        final ConfigManager cm = ConfigManager.instance();

        helper.encloseLabel("button-user-pw-dialog",
                this.getLocalizer().getString("button-password", this),
                canResetPassword);

        helper.encloseLabel("button-user-pin-dialog",
                this.getLocalizer().getString("button-pin", this),
                cm.isConfigValue(Key.USER_CAN_CHANGE_PIN));

        final boolean hasUriBase = StringUtils.isNotBlank(
                cm.getConfigValue(Key.IPP_INTERNET_PRINTER_URI_BASE));

        helper.encloseLabel("button-user-internet-printer-dialog",
                this.getLocalizer().getString("button-internet-printer", this),
                hasUriBase);

        //
        final boolean enableTelegram =
                cm.isConfigValue(Key.USER_EXT_TELEGRAM_TOTP_ENABLE)
                        && StringUtils.isNotBlank(
                                cm.getConfigValue(Key.EXT_TELEGRAM_BOT_TOKEN));
        if (enableTelegram) {
            helper.addModifyLabelAttr("telegram-id", MarkupHelper.ATTR_VALUE,
                    USER_SERVICE.getUserAttrValue(jpaUser,
                            UserAttrEnum.EXT_TELEGRAM_ID));
            helper.addButton("btn-telegram-ok", HtmlButtonEnum.OK);
            helper.addButton("btn-telegram-cancel", HtmlButtonEnum.CANCEL);
        }
        helper.encloseLabel("btn-telegram", "Telegram", enableTelegram);
        helper.encloseLabel("header-telegram", "Telegram ID", enableTelegram);

        //
        helper.encloseLabel("button-totp-dialog", "TOTP",
                cm.isConfigValue(Key.USER_TOTP_ENABLE));
    }
}
