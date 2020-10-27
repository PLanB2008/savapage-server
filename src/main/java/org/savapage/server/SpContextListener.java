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
package org.savapage.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.config.ConfigManager;

/**
 * This context listener receives notifications when the web application (i.e.
 * the context) is started up or shutdown.
 *
 * @author Rijk Ravestein
 *
 */
@WebListener
public final class SpContextListener implements ServletContextListener {

    /**
     * At this point the context is initialized and we can safely send the
     * {@link AdminPublisher#init(int)} message. NOTE: the servlets are started
     * after this event.
     *
     * @param event
     *            The {@link ServletContextEvent}.
     */
    @Override
    public void contextInitialized(final ServletContextEvent event) {
        /*
         * Initialize the admin publisher. We use the SSL port for the CometD
         * Admin Client.
         */
        AdminPublisher.instance()
                .init(Integer.parseInt(WebApp.getServerSslPort()), true);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        /*
         * Context is destroyed. As last action we issue a log message.
         */
        AdminPublisher.instance().shutdown();

        try {
            ConfigManager.instance().exit();
        } catch (Exception e) {
            SpInfo.instance().log(e.getMessage());
        }

    }

}
