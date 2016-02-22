/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import java.util.Calendar;
import java.util.MissingResourceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppAbout extends AbstractPage {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    public AppAbout() {

        add(new Label("app-version", ConfigManager.getAppNameVersion()));
        add(new Label("current-year", String.valueOf(Calendar.getInstance()
                .get(Calendar.YEAR))));

        add(new Label("app-name", CommunityDictEnum.SAVAPAGE.getWord()));

        Label labelWrk;

        //
        labelWrk =
                new Label("app-copyright-owner-url",
                        CommunityDictEnum.DATRAVERSE_BV.getWord());
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.DATRAVERSE_BV_URL.getWord()));
        add(labelWrk);

        //
        labelWrk =
                new Label("savapage-source-code-url",
                        localized("source-code-link"));
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.COMMUNITY_SOURCE_CODE_URL.getWord()));
        add(labelWrk);

        //
        final PrinterDriverDownloadPanel downloadPanel =
                new PrinterDriverDownloadPanel("printerdriver-download-panel");
        add(downloadPanel);
        downloadPanel.populate();

        //
        String translatorInfo;
        try {
            translatorInfo =
                    localized("translator-info", localized("_translator_name"));
        } catch (MissingResourceException e) {
            translatorInfo = null;
        }

        addVisible(StringUtils.isNotBlank(translatorInfo), "translator-info",
                translatorInfo);

    }

}
