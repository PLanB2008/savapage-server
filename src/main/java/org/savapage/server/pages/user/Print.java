/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.server.pages.user;

import java.util.EnumSet;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.server.SpSession;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.EnumRadioPanel;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.QuickSearchPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Print extends AbstractUserPage {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    final String ID_DELETE_PAGES = "delete-pages-after-print";
    final String ID_DELETE_PAGES_WARN = "delete-pages-after-print-warn";
    final String ID_DELETE_PAGES_SCOPE = "delete-pages-after-print-scope";
    final String HTML_NAME_DELETE_PAGES_SCOPE = ID_DELETE_PAGES_SCOPE;

    /**
     *
     */
    public Print(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);
        final ConfigManager cm = ConfigManager.instance();

        helper.addModifyLabelAttr("slider-print-copies", "max",
                cm.getConfigValue(Key.WEBAPP_USER_PROXY_PRINT_MAX_COPIES));

        if (cm.isConfigValue(Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_ENABLE)) {

            final InboxSelectScopeEnum clearScope =
                    cm.getConfigEnum(InboxSelectScopeEnum.class,
                            Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_SCOPE);

            final String keyWarn;
            switch (clearScope) {
            case ALL:
                keyWarn = "delete-pages-after-print-info-all";
                break;
            case JOBS:
                keyWarn = "delete-pages-after-print-info-jobs";
                break;
            case PAGES:
                keyWarn = "delete-pages-after-print-info-pages";
                break;
            case NONE:
            default:
                throw new SpException(String.format("%s is not handled.",
                        clearScope.toString()));
            }

            helper.discloseLabel(ID_DELETE_PAGES);
            helper.encloseLabel(ID_DELETE_PAGES_WARN, localized(keyWarn), true);

        } else {

            helper.discloseLabel(ID_DELETE_PAGES_WARN);

            add(new Label(ID_DELETE_PAGES));

            final EnumRadioPanel clearInboxScopePanel =
                    new EnumRadioPanel(ID_DELETE_PAGES_SCOPE);

            clearInboxScopePanel.populate(
                    EnumSet.complementOf(EnumSet.of(InboxSelectScopeEnum.NONE)),
                    InboxSelectScopeEnum.ALL,
                    InboxSelectScopeEnum.uiTextMap(getLocale()),
                    HTML_NAME_DELETE_PAGES_SCOPE);

            add(clearInboxScopePanel);
        }

        final QuickSearchPanel panel =
                new QuickSearchPanel("quicksearch-printer");

        add(panel);

        panel.populate("sp-print-qs-printer", "",
                getLocalizer().getString("search-printer-placeholder", this));

        // Prepare for hiding ...
        helper.encloseLabel("print-documents-separate",
                localized("print-documents-separate"), true);

        //
        helper.encloseLabel("print-remove-graphics-label",
                localized("print-remove-graphics"),
                cm.isConfigValue(Key.PROXY_PRINT_REMOVE_GRAPHICS_ENABLE));

        //
        helper.encloseLabel("print-ecoprint", "",
                ConfigManager.isEcoPrintEnabled());

        final Integer discount =
                cm.getConfigInt(Key.ECO_PRINT_DISCOUNT_PERC, 0);

        final String ecoPrintLabel;

        if (discount.intValue() > 0) {
            ecoPrintLabel = localized("print-ecoprint-with-discount", discount);
        } else {
            ecoPrintLabel = localized("print-ecoprint");
        }

        helper.addLabel("print-ecoprint-label", ecoPrintLabel);

        //
        helper.encloseLabel("prompt-jobticket-delivery-datetime",
                localized("prompt-jobticket-delivery-datetime"),
                cm.isConfigValue(Key.JOBTICKET_DELIVERY_DATETIME_ENABLE));

        //
        final org.savapage.core.jpa.User user = SpSession.get().getUser();

        final boolean isPrintDelegate = ACCESS_CONTROL_SERVICE.hasAccess(user,
                ACLRoleEnum.PRINT_DELEGATE);

        addVisible(isPrintDelegate, "button-print-delegation", "-");

        //
        final Integer privsLetterhead = ACCESS_CONTROL_SERVICE
                .getUserPrivileges(user, ACLOidEnum.U_LETTERHEAD);

        helper.encloseLabel("prompt-letterhead", localized("prompt-letterhead"),
                privsLetterhead == null || ACLPermissionEnum.READER
                        .isPresent(privsLetterhead.intValue()));

        //
        add(new Label("button-send-jobticket",
                HtmlButtonEnum.SEND.uiText(getLocale())));
    }
}
