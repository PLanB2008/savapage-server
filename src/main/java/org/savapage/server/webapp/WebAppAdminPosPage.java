/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
package org.savapage.server.webapp;

import java.util.EnumSet;
import java.util.Set;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class WebAppAdminPosPage extends AbstractWebAppPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppAdminPosPage(final PageParameters parameters) {

        super(parameters);

        final String appTitle =
                getWebAppTitle(getLocalizer().getString("webapp-title-suffix",
                        this));

        addZeroPagePanel(WebAppTypeEnum.POS);

        add(new Label("app-title", appTitle));

        addFileDownloadApiPanel();
    }

    @Override
    boolean isJqueryCoreRenderedByWicket() {
        return true;
    }

    @Override
    protected WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.POS;
    }

    @Override
    protected void renderWebAppTypeJsFiles(final IHeaderResponse response,
            final String nocache) {

        renderJs(response, String.format("%s%s",
                "jquery.savapage-admin-page-pos.js", nocache));
        renderJs(response,
                String.format("%s%s", getSpecializedJsFileName(), nocache));
    }

    @Override
    protected String getSpecializedCssFileName() {
        return "jquery.savapage-admin-pos.css";
    }

    @Override
    protected String getSpecializedJsFileName() {
        return "jquery.savapage-admin-pos.js";
    }

    @Override
    protected Set<JavaScriptLibrary> getJavaScriptToRender() {
        return EnumSet.noneOf(JavaScriptLibrary.class);
    }

}
