/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
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
package org.savapage.server;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.config.ConfigManager;

/**
 * This context listener receives notifications when the web application (i.e.
 * the context) is started up or shutdown.
 * <p>
 * NOTE: This class is referred in {@code web.xml}.
 * </p>
 *
 * <pre>
 *  {@code
 * <listener>
 *     <listener-class>org.savapage.server.SpContextListener</listener-class>
 * </listener>
 * }
 * </pre>
 *
 * @author Datraverse B.V.
 */
public class SpContextListener implements ServletContextListener {

    /**
     * At this point the context is initialized and we can safely send the
     * {@link AdminPublisher#init(int)} message. NOTE: the servlets are started
     * after this event.
     *
     * @param event
     */
    @Override
    public final void contextInitialized(final ServletContextEvent event) {

        try {

            /*
             * Pass server properties to the central WebApp.
             */
            WebApp.setServerProps(ConfigManager.readServerProperties());

            /*
             * Initialize the admin publisher. We use the regular port for the
             * CometD Admin Client.
             */
            final int serverPort = Integer.parseInt(WebApp.getServerPort());

            AdminPublisher.instance().init(serverPort);

        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }

    @Override
    public final void contextDestroyed(final ServletContextEvent event) {
        /*
         * Context is destroyed. As last action we issue a log message.
         */
        AdminPublisher.instance().shutdown();

        try {
            ConfigManager.instance().exit();
        } catch (Exception e) {
            SpInfo.instance().log(e.getMessage());
        }
        SpInfo.instance().logDestroy("Server shutdown. Bye!");
    }

}