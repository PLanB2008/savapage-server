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
package org.savapage.server.pages.user;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.SpSession;
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

    /**
     *
     */
    public Print(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);
        final ConfigManager cm = ConfigManager.instance();

        helper.addModifyLabelAttr("slider-print-copies", "max",
                cm.getConfigValue(Key.WEBAPP_USER_PROXY_PRINT_MAX_COPIES));

        final Label label = new Label("delete-pages-after-print");

        if (cm.isConfigValue(Key.WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_ENABLE)) {
            label.add(new AttributeModifier("checked", "checked"));
            label.add(new AttributeModifier("disabled", "disabled"));
        }

        add(label);

        final QuickSearchPanel panel =
                new QuickSearchPanel("quicksearch-printer");

        add(panel);

        panel.populate("sp-print-qs-printer", "",
                getLocalizer().getString("search-printer-placeholder", this));

        //
        helper.addLabel("print-remove-graphics-label",
                localized("print-remove-graphics"));

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

    }
}
