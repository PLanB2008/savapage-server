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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.InetUtils;
import org.savapage.server.SpSession;
import org.savapage.server.pages.CommunityStatusFooterPanel;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class Main extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    private static final String CSS_CLASS_MAIN_ACTIONS = "main_actions";

    private static final String CSS_CLASS_MAIN_ACTIONS_BASE =
            "main_action_base";

    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static enum NavButtonEnum {
        ABOUT, BROWSE, UPLOAD, PDF, LETTERHEAD, SORT, PRINT, TICKET
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class NavBarItem {

        private final String itemCssClass;
        private final String imgCssClass;
        private final String buttonHtmlId;
        private final String buttonText;

        public NavBarItem(final String itemCssClass, final String imgCssClass,
                final String buttonHtmlId, final String buttonText) {
            this.imgCssClass = imgCssClass;
            this.itemCssClass = itemCssClass;
            this.buttonHtmlId = buttonHtmlId;
            this.buttonText = buttonText;
        }
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private class NavBarRow extends PropertyListView<NavBarItem> {

        /**
         *
         * @param id
         *            The Wicket id.
         * @param list
         *            The list.
         */
        NavBarRow(final String id, final List<NavBarItem> list) {
            super(id, list);
        }

        /**
        *
        */
        private static final long serialVersionUID = 1L;

        @Override
        protected void populateItem(final ListItem<NavBarItem> listItem) {

            final NavBarItem navBarItem = listItem.getModelObject();

            final WebMarkupContainer contItem = new WebMarkupContainer("item");
            contItem.add(
                    new AttributeModifier("class", navBarItem.itemCssClass));

            final WebMarkupContainer contButton =
                    new WebMarkupContainer("button");
            contButton
                    .add(new AttributeAppender("class",
                            String.format(" %s", navBarItem.imgCssClass)))
                    .add(new AttributeModifier("id", navBarItem.buttonHtmlId));

            contButton.add(new Label("button-text", navBarItem.buttonText));
            contItem.add(contButton);

            listItem.add(contItem);
        }
    }

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public Main(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        final Set<NavButtonEnum> buttonCandidates = new HashSet<>();

        buttonCandidates.add(NavButtonEnum.BROWSE);
        buttonCandidates.add(NavButtonEnum.ABOUT);

        final boolean isUpload = (ConfigManager.isWebPrintEnabled()
                && InetUtils.isIp4AddrInCidrRanges(
                        ConfigManager.instance().getConfigValue(
                                Key.WEB_PRINT_LIMIT_IP_ADDRESSES),
                        getClientIpAddr()));

        if (isUpload) {
            buttonCandidates.add(NavButtonEnum.UPLOAD);
        }

        //
        final org.savapage.core.jpa.User user = SpSession.get().getUser();

        final boolean isPrintDelegate = user != null && ACCESS_CONTROL_SERVICE
                .hasAccess(user, ACLRoleEnum.PRINT_DELEGATE);

        addVisible(isPrintDelegate, "button-print-delegation", "-");

        //
        add(new CommunityStatusFooterPanel("community-status-footer-panel",
                false));

        //
        final Set<NavButtonEnum> buttonPrivileged = getNavButtonPriv(user);

        //
        this.populateNavBar(buttonPrivileged, buttonCandidates);

        //
        addVisible(isUpload && buttonCandidates.contains(NavButtonEnum.UPLOAD),
                "button-upload", localized("button-upload"));

        addVisible(buttonCandidates.contains(NavButtonEnum.ABOUT),
                "button-mini-about", localized("button-about"));

        //
        this.add(MarkupHelper.createEncloseLabel("main-arr-action-pdf",
                localized("button-pdf"),
                buttonPrivileged.contains(NavButtonEnum.PDF)));

        this.add(MarkupHelper.createEncloseLabel("main-arr-action-print",
                localized("button-print"),
                buttonPrivileged.contains(NavButtonEnum.PRINT)));

        this.add(MarkupHelper.createEncloseLabel("main-arr-action-ticket",
                localized("button-ticket"),
                !buttonPrivileged.contains(NavButtonEnum.PRINT)
                        && buttonPrivileged.contains(NavButtonEnum.TICKET)));

        //
        final String userId;
        final String userName;

        if (user == null) {
            userId = "";
            userName = "";
        } else {
            userName = StringUtils.defaultString(user.getFullName());
            userId = user.getUserId();
        }

        final boolean showMiniUserDetails =
                ACCESS_CONTROL_SERVICE.hasUserAccess(user, ACLOidEnum.U_USER);

        if (showMiniUserDetails) {

            helper.encloseLabel("mini-user-balance", "",
                    ACCESS_CONTROL_SERVICE.hasUserPermission(user,
                            ACLOidEnum.U_FINANCIAL, ACLPermissionEnum.READER));

            helper.addModifyLabelAttr("mini-user-details-name", userId, "title",
                    userName);

            helper.discloseLabel("mini-user-name");

        } else {

            helper.discloseLabel("mini-user-details-name");
            final Label name = MarkupHelper.createEncloseLabel("mini-user-name",
                    userId, true);
            add(name.add(new AttributeModifier("title", userName)));
        }
    }

    /**
     *
     * @param user
     *            The user.
     * @return The privileged navigation buttons.
     */
    private static Set<NavButtonEnum>
            getNavButtonPriv(final org.savapage.core.jpa.User user) {

        final Set<NavButtonEnum> set = new HashSet<>();

        NavButtonEnum navButtonWlk;

        //
        navButtonWlk = NavButtonEnum.PDF;

        final Integer inboxPriv = ACCESS_CONTROL_SERVICE.getUserPrivileges(user,
                ACLOidEnum.U_INBOX);

        if (inboxPriv == null
                || ACLPermissionEnum.DOWNLOAD.isPresent(inboxPriv.intValue())
                || ACLPermissionEnum.SEND.isPresent(inboxPriv.intValue())) {
            set.add(navButtonWlk);
        }

        //
        if (ACCESS_CONTROL_SERVICE.isAuthorized(user,
                ACLRoleEnum.PRINT_CREATOR)) {
            set.add(NavButtonEnum.PRINT);
        } else if (ACCESS_CONTROL_SERVICE.isAuthorized(user,
                ACLRoleEnum.JOB_TICKET_CREATOR)) {
            set.add(NavButtonEnum.TICKET);
        }

        //
        navButtonWlk = NavButtonEnum.SORT;

        if (inboxPriv == null
                || ACLPermissionEnum.EDITOR.isPresent(inboxPriv.intValue())) {
            set.add(navButtonWlk);
        }

        //
        navButtonWlk = NavButtonEnum.LETTERHEAD;
        if (ACCESS_CONTROL_SERVICE.hasUserAccess(user,
                ACLOidEnum.U_LETTERHEAD)) {
            set.add(navButtonWlk);
        }

        //
        return set;
    }

    /**
     * .
     */
    private NavBarItem
            useButtonCandidate(final Set<NavButtonEnum> buttonCandidates) {

        NavButtonEnum candidate = null;
        NavBarItem item = null;

        /*
         * Order is important!
         */

        // #1
        if (item == null) {
            candidate = NavButtonEnum.UPLOAD;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                        "ui-icon-main-upload", "button-mini-upload",
                        localized("button-upload"));
            }
        }

        // #2
        if (item == null) {
            candidate = NavButtonEnum.BROWSE;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                        "ui-icon-main-browse", "button-browser",
                        localized("button-browse"));
            }
        }

        // #3
        if (item == null) {
            candidate = NavButtonEnum.ABOUT;
            if (buttonCandidates.contains(candidate)) {
                item = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                        "ui-icon-main-about", "button-about",
                        localized("button-about"));
            }
        }

        if (item != null) {
            buttonCandidates.remove(candidate);
        }

        return item;
    }

    /**
     *
     * @param buttonPrivileged
     */
    private void populateNavBar(final Set<NavButtonEnum> buttonPrivileged,
            final Set<NavButtonEnum> buttonCandidates) {

        List<NavBarItem> items = new ArrayList<>();

        NavBarItem itemWlk;

        // ----------------------------------------------------------
        // Row 1
        // ----------------------------------------------------------

        // ------------
        // PDF
        // ------------
        if (buttonPrivileged.contains(NavButtonEnum.PDF)) {
            itemWlk = new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-pdf-properties", "button-main-pdf-properties",
                    localized("button-pdf"));
        } else {
            itemWlk = useButtonCandidate(buttonCandidates);
        }

        if (itemWlk != null) {
            items.add(itemWlk);
        }

        // --------------------------
        // Print or Ticket (or none)
        // --------------------------
        if (buttonPrivileged.contains(NavButtonEnum.PRINT)) {
            items.add(
                    new NavBarItem(CSS_CLASS_MAIN_ACTIONS, "ui-icon-main-print",
                            "button-main-print", localized("button-print")));
        } else if (buttonPrivileged.contains(NavButtonEnum.TICKET)) {
            items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-jobticket", "button-main-print",
                    localized("button-ticket")));
        }

        // ------------
        // Letterhead
        // ------------
        if (buttonPrivileged.contains(NavButtonEnum.LETTERHEAD)) {
            itemWlk = new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                    "ui-icon-main-letterhead", "button-main-letterhead",
                    localized("button-letterhead"));
        } else {
            itemWlk = useButtonCandidate(buttonCandidates);
        }

        if (itemWlk != null) {
            items.add(itemWlk);
        }

        // ------------
        // Delete
        // ------------
        items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS, "ui-icon-main-clear",
                "button-main-clear", localized("button-clear")));

        //
        add(new NavBarRow("main-navbar-row-top", items));

        // ----------------------------------------------------------
        // Row 2
        // ----------------------------------------------------------
        items = new ArrayList<>();

        items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-logout", "button-logout",
                localized("button-logout")));

        items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-refresh", "button-main-refresh",
                localized("button-refresh")));

        items.add(new NavBarItem(CSS_CLASS_MAIN_ACTIONS_BASE,
                "ui-icon-main-doclog", "button-main-doclog",
                localized("button-doclog")));

        if (buttonPrivileged.contains(NavButtonEnum.SORT)) {
            itemWlk = new NavBarItem(CSS_CLASS_MAIN_ACTIONS,
                    "ui-icon-main-arr-edit", "main-arr-edit",
                    localized("button-sort"));
        } else {
            itemWlk = useButtonCandidate(buttonCandidates);
        }

        if (itemWlk != null) {
            items.add(itemWlk);
        }

        add(new NavBarRow("main-navbar-row-bottom", items));
    }

}
