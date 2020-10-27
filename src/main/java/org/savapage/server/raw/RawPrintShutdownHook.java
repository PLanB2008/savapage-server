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
package org.savapage.server.raw;

import org.savapage.core.SpInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RawPrintShutdownHook extends Thread {

    /**
     * The parent {@link RawPrintServer}.
     */
    private final RawPrintServer myServer;

    /**
     *
     * @param server
     *            The parent {@link RawPrintServer}.
     */
    public RawPrintShutdownHook(final RawPrintServer server) {
        super("RawPrintShutdownThread");
        this.myServer = server;
    }

    @Override
    public void run() {
        SpInfo.instance().log("Shutting down IP Print Server ...");
        myServer.shutdown();
        SpInfo.instance().log("... IP Print Server shutdown completed.");
    }
}
