/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.webapp;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class WebAppGoogleSignIn extends WebPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppGoogleSignIn(final PageParameters parameters) {

        super(parameters);

        add(new Label("app-title", "Google Sign-In"));

        add(MarkupHelper.modifyLabelAttr(new Label("webAppType", ""), "value",
                WebAppTypeEnum.USER.toString()));

        add(MarkupHelper.modifyLabelAttr(
                new Label("google-signin-client-id", ""), "content",
                ConfigManager.instance()
                        .getConfigValue(Key.AUTH_MODE_GOOGLE_CLIENT_ID)));
    }

}
